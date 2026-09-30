package com.example.cinema.modules.identity.business;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdentityService identityService = new IdentityService(userRepository, passwordEncoder);

    @Test
    void registerNormalizesUsernameAndStoresBCryptHashForCustomer() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        User registered = identityService.register(" Alice ", " password1 ");

        Instant after = Instant.now();
        assertThat(registered.id()).isNotNull();
        assertThat(registered.username()).isEqualTo("alice");
        assertThat(registered.passwordHash()).isNotEqualTo(" password1 ");
        assertThat(passwordEncoder.matches(" password1 ", registered.passwordHash())).isTrue();
        assertThat(registered.toString()).doesNotContain(registered.passwordHash());
        assertThat(registered.role()).isEqualTo(User.Role.CUSTOMER);
        assertThat(registered.createdAt()).isBetween(before, after);
        assertThat(registered.updatedAt()).isEqualTo(registered.createdAt());
        verify(userRepository).save(registered);
    }

    @Test
    void registerRejectsInvalidUsernamesAfterTrimming() {
        for (String username : List.of("ab", "a".repeat(51), "bad name", "name!")) {
            assertThatThrownBy(() -> identityService.register(username, "password1"))
                    .isInstanceOf(InvalidRegistrationException.class);
        }

        assertThatThrownBy(() -> identityService.register(null, "password1"))
                .isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void registerRejectsPasswordsOutsideEightToSixteenCharacters() {
        for (String password : List.of("1234567", "12345678901234567")) {
            assertThatThrownBy(() -> identityService.register("alice", password))
                    .isInstanceOf(InvalidRegistrationException.class);
        }

        assertThatThrownBy(() -> identityService.register("alice", null))
                .isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void registerRejectsAnExistingNormalizedUsername() {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new User(
                        UUID.randomUUID(), "alice", "existing-hash", User.Role.CUSTOMER,
                        Instant.now(), Instant.now())));
        PasswordEncoder unusedEncoder = mock(PasswordEncoder.class);
        IdentityService service = new IdentityService(userRepository, unusedEncoder);

        assertThatThrownBy(() -> service.register(" Alice ", "password1"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
        verify(unusedEncoder, never()).encode(any());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void loginNormalizesUsernameAndReturnsOnlyIdAndRole(CapturedOutput output) {
        UUID id = UUID.randomUUID();
        String password = " password1 ";
        String hash = passwordEncoder.encode(password);
        Instant timestamp = Instant.now();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(
                id, "alice", hash, User.Role.ADMIN, timestamp, timestamp)));

        AuthenticatedUser authenticated = identityService.login(" Alice ", password);

        assertThat(authenticated).isEqualTo(new AuthenticatedUser(id, User.Role.ADMIN));
        assertThat(output).contains("Login succeeded userId=" + id).doesNotContain(password, hash);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void loginReturnsSameErrorForUnknownUsernameAndWrongPassword(CapturedOutput output) {
        Instant timestamp = Instant.now();
        String hash = passwordEncoder.encode("password1");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User(
                UUID.randomUUID(), "alice", hash, User.Role.CUSTOMER,
                timestamp, timestamp)));
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityService.login("unknown", "password1"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> identityService.login("alice", "wrongpass"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        assertThat(output).contains("Login failed").doesNotContain("password1", "wrongpass", hash);
    }

    @Test
    void loginChecksValidBCryptHashEvenWhenUsernameDoesNotExist() {
        PasswordEncoder trackingEncoder = mock(PasswordEncoder.class);
        IdentityService service = new IdentityService(userRepository, trackingEncoder);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("missing", "password1"))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(trackingEncoder).matches(eq("password1"), hash.capture());
        assertThat(passwordEncoder.matches("dummy-password", hash.getValue())).isTrue();
    }
}
