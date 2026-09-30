package com.example.cinema.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.api.RegisterRequest;
import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import com.example.cinema.modules.identity.business.UsernameAlreadyExistsException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always"
)
@ActiveProfiles("test")
class PostgreSqlIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void flywayInitializesPostgreSQL() {
        Boolean historyTableExists = jdbcTemplate.queryForObject(
                "select to_regclass('public.flyway_schema_history') is not null",
                Boolean.class
        );
        Boolean usersTableExists = jdbcTemplate.queryForObject(
                "select to_regclass('public.users') is not null",
                Boolean.class
        );

        assertThat(historyTableExists).isTrue();
        assertThat(usersTableExists).isTrue();
    }

    @Test
    void usersRequireUniqueLowercaseUsernameAndKnownRole() {
        String username = "customer-" + UUID.randomUUID();
        insertUser(username, "CUSTOMER");

        assertThatThrownBy(() -> insertUser(username, "CUSTOMER"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertUser("UpperCase", "CUSTOMER"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser("unknown-role", "GUEST"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userRepositoryStoresAndFindsUserByIdAndUsername() {
        UUID id = UUID.randomUUID();
        String username = "user-" + id;
        Instant timestamp = Instant.parse("2026-09-29T00:00:00Z");
        User expected = new User(id, username, "test-hash", User.Role.CUSTOMER, timestamp, timestamp);

        User saved = userRepository.save(expected);

        assertThat(saved).isEqualTo(expected);
        assertThat(userRepository.findById(id)).contains(expected);
        assertThat(userRepository.findByUsername(username)).contains(expected);
    }

    @Test
    void userRepositoryMapsOnlyUsernameUniqueViolationToBusinessError() {
        User existing = userRepository.save(new User(
                UUID.randomUUID(), "duplicate-user", "test-hash", User.Role.CUSTOMER,
                Instant.parse("2026-09-29T00:00:00Z"), Instant.parse("2026-09-29T00:00:00Z")));
        User duplicateUsername = new User(
                UUID.randomUUID(), existing.username(), "another-hash", User.Role.CUSTOMER,
                existing.createdAt(), existing.updatedAt());

        assertThatThrownBy(() -> userRepository.save(duplicateUsername))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        assertThatThrownBy(() -> userRepository.save(new User(
                UUID.randomUUID(), null, "test-hash", User.Role.CUSTOMER,
                existing.createdAt(), existing.updatedAt())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registerCreatesCustomerAndDoesNotExposeCredentials() {
        String username = "register-" + UUID.randomUUID();
        String password = " password1 ";
        assertThat(new RegisterRequest(username, password).toString()).doesNotContain(password);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("username", " " + username.toUpperCase() + " ", "password", password, "role", "ADMIN"),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("data").path("username").asText()).isEqualTo(username);
        assertThat(response.getBody().path("data").path("role").asText()).isEqualTo("CUSTOMER");
        assertThat(response.getBody().path("data").path("id").asText()).isNotBlank();
        assertThat(response.getBody().toString())
                .doesNotContain("password", "passwordHash", "accessToken", password);

        User stored = userRepository.findByUsername(username).orElseThrow();
        assertThat(stored.role()).isEqualTo(User.Role.CUSTOMER);
        assertThat(stored.passwordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, stored.passwordHash())).isTrue();
    }

    @Test
    void registerMapsInvalidInputMalformedJsonAndDuplicateUsername() {
        ResponseEntity<JsonNode> missingFields = restTemplate.postForEntity(
                "/api/v1/auth/register", Map.of(), JsonNode.class);
        assertThat(missingFields.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(missingFields.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");

        ResponseEntity<JsonNode> invalidUsername = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("username", "not valid", "password", "password1"),
                JsonNode.class
        );
        assertThat(invalidUsername.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(invalidUsername.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> malformedJson = restTemplate.postForEntity(
                "/api/v1/auth/register", new HttpEntity<>("{", headers), JsonNode.class);
        assertThat(malformedJson.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(malformedJson.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");

        for (String body : new String[] {
                "{\"username\":123,\"password\":\"password1\"}",
                "{\"username\":\"valid-user\",\"password\":12345678}"
        }) {
            ResponseEntity<JsonNode> wrongType = restTemplate.postForEntity(
                    "/api/v1/auth/register", new HttpEntity<>(body, headers), JsonNode.class);
            assertThat(wrongType.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(wrongType.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
        }

        String username = "duplicate-" + UUID.randomUUID();
        ResponseEntity<JsonNode> first = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("username", username, "password", "password1"),
                JsonNode.class
        );
        ResponseEntity<JsonNode> duplicate = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("username", " " + username.toUpperCase() + " ", "password", "password1"),
                JsonNode.class
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().path("error").path("code").asText())
                .isEqualTo("USERNAME_ALREADY_EXISTS");
        assertThat(duplicate.getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    void loginReturnsSignedBearerTokenForRegisteredUser() {
        String username = "login-" + UUID.randomUUID();
        String password = " password1 ";
        ResponseEntity<JsonNode> registration = restTemplate.postForEntity(
                "/api/v1/auth/register", Map.of("username", username, "password", password), JsonNode.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registration.getBody()).isNotNull();
        UUID userId = UUID.fromString(registration.getBody().path("data").path("id").asText());

        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of("username", username, "password", password), JsonNode.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(login.getBody().path("data").path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(login.getBody().path("data").path("expiresIn").asLong()).isEqualTo(3600);
        String accessToken = login.getBody().path("data").path("accessToken").asText();
        assertThat(jwtTokenService.verify(accessToken)).isEqualTo(
                new AuthenticatedUser(userId, User.Role.CUSTOMER));
        assertThat(login.getBody().toString()).doesNotContain(password, "passwordHash");
    }

    @Test
    void loginMapsInvalidCredentialsAndInvalidRequests() {
        ResponseEntity<JsonNode> invalidCredentials = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of("username", "unknown-user", "password", "password1"),
                JsonNode.class);

        assertThat(invalidCredentials.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(invalidCredentials.getBody().path("error").path("code").asText())
                .isEqualTo("INVALID_CREDENTIALS");
        assertThat(invalidCredentials.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(invalidCredentials.getBody().path("error").path("requestId").asText())
                .isEqualTo(invalidCredentials.getHeaders().getFirst("X-Request-Id"));

        ResponseEntity<JsonNode> missingFields = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of(), JsonNode.class);
        assertThat(missingFields.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(missingFields.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> malformedJson = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>("{", headers), JsonNode.class);
        assertThat(malformedJson.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(malformedJson.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    private void insertUser(String username, String role) {
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), username, "test-hash", role);
    }

    @Test
    void readinessIncludesDatabaseHealth() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/actuator/health/readiness",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().path("components").path("db").path("status").asText())
                .isEqualTo("UP");
    }
}
