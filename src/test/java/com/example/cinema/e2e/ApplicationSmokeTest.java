package com.example.cinema.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.cinema.config.DemoAdminSeeder;
import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.api.LoginResponse;
import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
                "management.endpoint.health.group.readiness.include=readinessState"
        }
)
@ActiveProfiles("test")
@Import(ApplicationSmokeTest.SmokeTestController.class)
class ApplicationSmokeTest {

    @MockitoBean(name = "userRepositoryAdapter")
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${cinema.jwt.secret}")
    private String jwtSecret;

    @Test
    void livenessProbeIsUp() {
        ResponseEntity<JsonNode> liveness = restTemplate.getForEntity("/actuator/health/liveness", JsonNode.class);

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(liveness.getBody()).isNotNull();
        assertThat(liveness.getBody().path("status").asText()).isEqualTo("UP");
    }

    @Test
    void demoAdminSeederIsEnabledInTestProfileWithoutDemoSettings() {
        assertThat(applicationContext.getBeansOfType(DemoAdminSeeder.class)).hasSize(1);
    }

    @Test
    void nonPublicApiRequiresAuthentication() {
        String requestId = "0f43cdb5-7972-40a7-8932-b60cd50f031e";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/secured", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getLocation()).isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().path("error").path("details").isArray()).isTrue();
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo(requestId);
        assertThat(response.getBody().path("error").path("requestId").asText()).isEqualTo(requestId);
    }

    @Test
    void bearerTokenSetsUuidPrincipalAndEnforcesAdminRole() {
        UUID customerId = UUID.randomUUID();
        ResponseEntity<JsonNode> customer = restTemplate.exchange(
                "/api/v1/secured", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(customerId, User.Role.CUSTOMER)), JsonNode.class);
        assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customer.getBody().path("userId").asText()).isEqualTo(customerId.toString());

        UUID adminId = UUID.randomUUID();
        ResponseEntity<JsonNode> admin = restTemplate.exchange(
                "/api/v1/admin/secured", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminId, User.Role.ADMIN)), JsonNode.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getBody().path("userId").asText()).isEqualTo(adminId.toString());

        ResponseEntity<JsonNode> denied = restTemplate.exchange(
                "/api/v1/admin/secured", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(customerId, User.Role.CUSTOMER)), JsonNode.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().path("error").path("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void invalidBearerTokenReturnsSharedUnauthorizedResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-jwt");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().path("error").path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void expiredBearerTokenReturnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken(UUID.randomUUID()));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/secured", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void currentUserReturnsOnlyPublicProfileFields() {
        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode("password1");
        Instant now = Instant.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId, "alice", passwordHash, User.Role.CUSTOMER, now, now)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userId, User.Role.CUSTOMER)), JsonNode.class);

        JsonNode data = response.getBody().path("data");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.path("id").asText()).isEqualTo(userId.toString());
        assertThat(data.path("username").asText()).isEqualTo("alice");
        assertThat(data.path("role").asText()).isEqualTo("CUSTOMER");
        assertThat(data.size()).isEqualTo(3);
        assertThat(response.getBody().toString()).doesNotContain(passwordHash, "createdAt", "passwordHash");
    }

    @Test
    void currentUserReportsTheRoleUsedByTheBearerToken() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId, "alice", "stored-hash", User.Role.CUSTOMER, now, now)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userId, User.Role.ADMIN)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("data").path("role").asText()).isEqualTo("ADMIN");
    }

    @Test
    void currentUserRequiresAuthentication() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/users/me", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void currentUserReturnsUnauthorizedWhenAccountNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userId, User.Role.CUSTOMER)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().path("error").path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void onlyRegisterAndLoginPostRoutesArePublic() {
        assertThat(restTemplate.getForEntity("/api/v1/auth/login", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.postForEntity("/api/v1/auth/other", Map.of(), JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicRoutesIgnoreExpiredBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken(UUID.randomUUID()));

        ResponseEntity<JsonNode> register = restTemplate.postForEntity(
                "/api/v1/auth/register", new HttpEntity<>(Map.of(), headers), JsonNode.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(register.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");

        ResponseEntity<JsonNode> liveness = restTemplate.exchange(
                "/actuator/health/liveness", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> docs = restTemplate.exchange(
                "/v3/api-docs", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void loginReturnsBearerTokenForValidCredentialsDespiteExpiredHeader() {
        UUID userId = UUID.randomUUID();
        String username = "login-user";
        String password = "password1";
        Instant now = Instant.now();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User(
                userId, username, passwordEncoder.encode(password), User.Role.CUSTOMER, now, now)));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken(UUID.randomUUID()));
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(Map.of("username", "  LOGIN-USER  ", "password", password), headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("data").path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(response.getBody().path("data").path("expiresIn").asLong()).isEqualTo(3600);
        String accessToken = response.getBody().path("data").path("accessToken").asText();
        assertThat(response.getBody().toString()).doesNotContain(password, "passwordHash");
        assertThat(new LoginResponse(new LoginResponse.Data(accessToken, "Bearer", 3600)).toString())
                .doesNotContain(accessToken);
        assertThat(jwtTokenService.verify(accessToken))
                .isEqualTo(new AuthenticatedUser(userId, User.Role.CUSTOMER));
    }

    @Test
    void invalidLoginCredentialsUseTheSharedUnauthorizedResponse() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of("username", "unknown-user", "password", "password1"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(response.getBody().path("error").path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));

        ResponseEntity<JsonNode> missingFields = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of(), JsonNode.class);
        assertThat(missingFields.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(missingFields.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void invalidLoginFieldShapesReturnValidationError() {
        for (String[] fields : new String[][] {
                {"ab", "password1"},
                {"a".repeat(51), "password1"},
                {"bad name", "password1"},
                {"valid-user", "1234567"},
                {"valid-user", "x".repeat(17)}
        }) {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    "/api/v1/auth/login",
                    Map.of("username", fields[0], "password", fields[1]),
                    JsonNode.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Test
    void authenticatedMissingRouteKeepsItsHttpError() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/secured/not-found", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(UUID.randomUUID(), User.Role.CUSTOMER)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().path("error").path("details").isArray()).isTrue();
        String requestId = response.getHeaders().getFirst("X-Request-Id");
        assertThat(requestId).isNotBlank();
        assertThat(UUID.fromString(requestId)).isNotNull();
        assertThat(response.getBody().path("error").path("requestId").asText()).isEqualTo(requestId);
    }

    @Test
    void invalidRequestIdIsReplaced() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", "not-a-uuid");
        headers.setBearerAuth(jwtTokenService.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER)));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/secured/not-found", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        String requestId = response.getHeaders().getFirst("X-Request-Id");
        assertThat(requestId).isNotBlank().isNotEqualTo("not-a-uuid");
        assertThat(UUID.fromString(requestId)).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("requestId").asText()).isEqualTo(requestId);
    }

    @Test
    void unsupportedMethodKeepsItsHttpStatus() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/v3/api-docs", null, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void invalidQueryParameterReturnsBadRequest() {
        ResponseEntity<JsonNode> response = restTemplate.exchange("/api/v1/secured/test-errors?count=abc",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(UUID.randomUUID(), User.Role.CUSTOMER)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenService.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER)));
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/secured/test-errors", new HttpEntity<>("{", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void invalidBodyReturnsValidationError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenService.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER)));
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/secured/test-errors", new HttpEntity<>("{}", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void unexpectedErrorDoesNotLeakInternalDetails() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/secured/test-errors/unexpected", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(UUID.randomUUID(), User.Role.CUSTOMER)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().toString()).doesNotContain("secret SQL detail");
        assertThat(response.getBody().path("error").path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/secured"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void openApiDocumentIsAvailable() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasNonNull("openapi")).isTrue();
        assertThat(response.getBody().path("info").path("title").asText()).isEqualTo("Cinema API");
        assertThat(response.getBody().path("info").path("version").asText()).isEqualTo("v1");
        JsonNode document = response.getBody();
        JsonNode register = document.path("paths").path("/api/v1/auth/register").path("post");
        JsonNode login = document.path("paths").path("/api/v1/auth/login").path("post");
        JsonNode currentUser = document.path("paths").path("/api/v1/users/me").path("get");

        assertThat(register.isObject()).isTrue();
        assertThat(register.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText()).endsWith("/RegisterRequest");
        assertThat(register.path("responses").has("201")).isTrue();
        assertThat(register.path("responses").has("400")).isTrue();
        assertThat(register.path("responses").has("409")).isTrue();
        assertThat(register.path("responses").has("422")).isTrue();
        assertThat(register.path("responses").path("201").path("content").path("application/json")
                .path("schema").path("$ref").asText()).endsWith("/UserResponse");
        JsonNode userDataSchema = document.path("components").path("schemas").path("UserResponse")
                .path("properties").path("data");
        assertThat(userDataSchema.path("$ref").asText()).startsWith("#/components/schemas/");
        JsonNode roleSchema = document.at(userDataSchema.path("$ref").asText().substring(1))
                .path("properties").path("role");
        assertThat(roleSchema.path("enum").toString()).isEqualTo("[\"CUSTOMER\",\"ADMIN\"]");
        assertThat(login.isObject()).isTrue();
        assertThat(login.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText()).endsWith("/LoginRequest");
        assertThat(login.path("responses").has("200")).isTrue();
        assertThat(login.path("responses").has("400")).isTrue();
        assertThat(login.path("responses").has("401")).isTrue();
        assertThat(login.path("responses").has("422")).isTrue();
        assertThat(login.path("responses").path("401").path("content").path("application/json")
                .path("schema").path("$ref").asText()).endsWith("/ErrorResponse");
        assertThat(currentUser.isObject()).isTrue();
        assertThat(currentUser.path("responses").has("200")).isTrue();
        assertThat(currentUser.path("responses").has("401")).isTrue();

        assertThat(register.has("security")).isFalse();
        assertThat(login.has("security")).isFalse();
        assertThat(currentUser.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth")
                .path("type").asText()).isEqualTo("http");
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth")
                .path("scheme").asText()).isEqualTo("bearer");
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth")
                .path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void swaggerUiIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Swagger UI");
    }

    @RestController
    static class SmokeTestController {

        @GetMapping("/api/v1/secured")
        Map<String, UUID> secured(@AuthenticationPrincipal UUID userId) {
            return Map.of("userId", userId);
        }

        @GetMapping("/api/v1/admin/secured")
        Map<String, UUID> admin(@AuthenticationPrincipal UUID userId) {
            return Map.of("userId", userId);
        }

        @GetMapping("/api/v1/secured/test-errors")
        String query(@RequestParam int count) {
            return "ok";
        }

        @PostMapping("/api/v1/secured/test-errors")
        String body(@Valid @RequestBody TestBody body) {
            return body.value();
        }

        @GetMapping("/api/v1/secured/test-errors/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret SQL detail");
        }

        record TestBody(@NotBlank String value) {
        }
    }

    private HttpHeaders bearerHeaders(UUID userId, User.Role role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtTokenService.issue(new AuthenticatedUser(userId, role)));
        return headers;
    }

    private String expiredToken(UUID userId) {
        Instant issuedAt = Instant.now().minusSeconds(7200).truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim("role", User.Role.CUSTOMER.name())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .build();
        var key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
