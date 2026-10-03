package com.example.cinema.config;

import com.example.cinema.shared.api.ErrorResponse;
import com.example.cinema.shared.api.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            Environment environment,
            JwtTokenService jwtTokenService
    ) throws Exception {
        boolean docsPublic = environment.acceptsProfiles(Profiles.of("local", "test"));
        var paths = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> publicMatchers = new ArrayList<>(List.of(
                paths.matcher(HttpMethod.POST, "/api/v1/auth/register"),
                paths.matcher(HttpMethod.POST, "/api/v1/auth/login"),
                paths.matcher(HttpMethod.GET, "/actuator/health/liveness"),
                paths.matcher(HttpMethod.GET, "/actuator/health/readiness")));
        if (docsPublic) {
            publicMatchers.addAll(List.of(
                    paths.matcher("/swagger-ui.html"), paths.matcher("/swagger-ui/**"),
                    paths.matcher("/v3/api-docs"), paths.matcher("/v3/api-docs/**")));
        }
        RequestMatcher publicRoutes = new OrRequestMatcher(publicMatchers);
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) ->
                writeError(request, response, objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(request, response, objectMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied")))
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    authorize.requestMatchers(publicRoutes).permitAll();
                    authorize.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService, authenticationEntryPoint, publicRoutes),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code, message, RequestIdFilter.get(request)));
    }
}
