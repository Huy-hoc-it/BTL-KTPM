package com.example.cinema.e2e;

import com.example.cinema.config.DemoAdminSeeder;
import com.example.cinema.modules.identity.business.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
        "management.endpoint.health.group.readiness.include=readinessState",
        "cinema.jwt.secret=test-only-jwt-secret-for-production-profile-test",
        "cinema.jwt.expiration-seconds=3600"
})
@ActiveProfiles("prod")
class ProductionDocsSecurityTest {

    @MockitoBean(name = "userRepositoryAdapter")
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void swaggerAndOpenApiRequireAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @Test
    void demoAdminSeederIsNotLoadedInProduction() {
        org.assertj.core.api.Assertions.assertThat(applicationContext.getBeansOfType(DemoAdminSeeder.class)).isEmpty();
    }
}
