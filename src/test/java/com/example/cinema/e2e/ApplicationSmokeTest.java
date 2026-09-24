package com.example.cinema.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
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
@Import(ApplicationSmokeTest.ErrorTestController.class)
class ApplicationSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessProbeIsUp() {
        ResponseEntity<JsonNode> liveness = restTemplate.getForEntity("/actuator/health/liveness", JsonNode.class);

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(liveness.getBody()).isNotNull();
        assertThat(liveness.getBody().path("status").asText()).isEqualTo("UP");
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
    void publicAuthRequestKeepsItsHttpError() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/auth/not-found", JsonNode.class);

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
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/auth/not-found", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

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
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/auth/test-errors?count=abc", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/test-errors", new HttpEntity<>("{", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void invalidBodyReturnsValidationError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/test-errors", new HttpEntity<>("{}", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void unexpectedErrorDoesNotLeakInternalDetails() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/auth/test-errors/unexpected", JsonNode.class);

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
    }

    @Test
    void swaggerUiIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Swagger UI");
    }

    @RestController
    static class ErrorTestController {

        @GetMapping("/api/v1/auth/test-errors")
        String query(@RequestParam int count) {
            return "ok";
        }

        @PostMapping("/api/v1/auth/test-errors")
        String body(@Valid @RequestBody TestBody body) {
            return body.value();
        }

        @GetMapping("/api/v1/auth/test-errors/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret SQL detail");
        }

        record TestBody(@NotBlank String value) {
        }
    }
}
