package com.example.cinema.config;

import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoAdminSeederTest {

    @Test
    void createsHashedAdminOnceAndNormalizesUsername() throws Exception {
        Map<String, User> users = new HashMap<>();
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByUsername(anyString()))
                .thenAnswer(call -> Optional.ofNullable(users.get(call.getArgument(0))));
        when(repository.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            users.put(user.username(), user);
            return user;
        });
        var passwordEncoder = new BCryptPasswordEncoder();
        var seeder = new DemoAdminSeeder(repository, passwordEncoder, " Demo-Admin ", "demo-pass-1234");

        seeder.run(null);
        seeder.run(null);

        assertThat(users).hasSize(1);
        User admin = users.get("demo-admin");
        assertThat(admin.role()).isEqualTo(User.Role.ADMIN);
        assertThat(admin.passwordHash()).isNotEqualTo("demo-pass-1234");
        assertThat(passwordEncoder.matches("demo-pass-1234", admin.passwordHash())).isTrue();
        assertThat(admin.createdAt()).isNotNull();
        verify(repository, times(2)).findByUsername("demo-admin");
        verify(repository).save(admin);
    }

    @Test
    void refusesToUpgradeAnExistingCustomer() {
        User customer = new User(UUID.randomUUID(), "demo-admin", "hash", User.Role.CUSTOMER,
                java.time.Instant.now(), java.time.Instant.now());
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByUsername("demo-admin")).thenReturn(Optional.of(customer));
        var seeder = new DemoAdminSeeder(repository, new BCryptPasswordEncoder(), "demo-admin", "demo-pass-1234");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_ADMIN_USERNAME")
                .hasMessageContaining("non-Admin");
        verify(repository, never()).save(any());
    }

    @Test
    void skipsSeedingWhenEitherSettingIsMissing() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        var passwordEncoder = new BCryptPasswordEncoder();

        new DemoAdminSeeder(repository, passwordEncoder, "", "demo-pass-1234").run(null);
        new DemoAdminSeeder(repository, passwordEncoder, "demo-admin", "").run(null);

        verify(repository, never()).findByUsername(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDemoPasswordsOutsideTheAccountPolicy() {
        UserRepository repository = mock(UserRepository.class);
        var seeder = new DemoAdminSeeder(repository, new BCryptPasswordEncoder(), "demo-admin", "short");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_ADMIN_PASSWORD");
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsPasswordsThatAreEightSpacesBecausePasswordsAreNotTrimmed() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByUsername("demo-admin")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        var passwordEncoder = new BCryptPasswordEncoder();
        String password = "        ";
        var seeder = new DemoAdminSeeder(repository, passwordEncoder, "demo-admin", password);

        seeder.run(null);

        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(repository).save(saved.capture());
        assertThat(passwordEncoder.matches(password, saved.getValue().passwordHash())).isTrue();
    }
}
