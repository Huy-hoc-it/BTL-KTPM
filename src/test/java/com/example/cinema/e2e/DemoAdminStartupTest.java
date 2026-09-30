package com.example.cinema.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
                "management.endpoint.health.group.readiness.include=readinessState",
                "DEMO_ADMIN_USERNAME=startup-admin",
                "DEMO_ADMIN_PASSWORD=demo-pass-1234"
        }
)
@ActiveProfiles("test")
@Import(ApplicationSmokeTest.SmokeTestController.class)
class DemoAdminStartupTest {

    @MockitoBean(name = "userRepositoryAdapter")
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void configuredAdminIsSeededAtStartupAndCanUseAdminApi() {
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        User admin = savedUser.getValue();
        assertThat(admin.username()).isEqualTo("startup-admin");
        assertThat(admin.role()).isEqualTo(User.Role.ADMIN);
        assertThat(passwordEncoder.matches("demo-pass-1234", admin.passwordHash())).isTrue();

        when(userRepository.findByUsername("startup-admin")).thenReturn(Optional.of(admin));
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of("username", "startup-admin", "password", "demo-pass-1234"),
                JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().path("data").path("accessToken").asText());
        ResponseEntity<JsonNode> adminApi = restTemplate.exchange(
                "/api/v1/admin/secured", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(adminApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminApi.getBody().path("userId").asText()).isEqualTo(admin.id().toString());
    }
}
