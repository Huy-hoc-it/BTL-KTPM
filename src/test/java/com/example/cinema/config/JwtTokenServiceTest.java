package com.example.cinema.config;

import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    private static final String SECRET = "jwt-unit-test-secret-that-is-at-least-32-bytes";

    @Test
    void signedTokenCanBeVerifiedForItsUserIdAndRole() {
        Instant now = Instant.now().minusSeconds(5).truncatedTo(ChronoUnit.SECONDS);
        JwtTokenService tokenService = new JwtTokenService(
                SECRET, 3600, Clock.fixed(now, ZoneOffset.UTC));
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.fromString("7b1c6d73-8fb0-4b41-9892-1bb26860886c"), User.Role.ADMIN);

        String token = tokenService.issue(user);
        var jwt = NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build().decode(token);

        assertThat(tokenService.verify(token)).isEqualTo(user);
        assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(tokenService.expirationSeconds()).isEqualTo(3600);
    }

    @Test
    void rejectsATokenWithAChangedSignature() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600, Clock.systemUTC());
        String token = tokenService.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER));
        String[] segments = token.split("\\.");
        segments[2] = (segments[2].startsWith("A") ? "B" : "A") + segments[2].substring(1);

        assertThatThrownBy(() -> tokenService.verify(String.join(".", segments)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnExpiredTokenWithoutSleeping() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtTokenService issuer = new JwtTokenService(SECRET, 3600, past);
        JwtTokenService verifier = new JwtTokenService(SECRET, 3600, Clock.systemUTC());
        String expiredToken = issuer.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER));

        assertThatThrownBy(() -> verifier.verify(expiredToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void verificationUsesTheInjectedClock() {
        Instant issuedAt = Instant.parse("2020-01-01T00:00:00Z");
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER);
        JwtTokenService issuer = new JwtTokenService(SECRET, 3600, Clock.fixed(issuedAt, ZoneOffset.UTC));
        JwtTokenService verifier = new JwtTokenService(
                SECRET, 3600, Clock.fixed(issuedAt.plusSeconds(1800), ZoneOffset.UTC));

        assertThat(verifier.verify(issuer.issue(user))).isEqualTo(user);
    }

    @Test
    void rejectsTokenAtExpirationAndWithinDefaultClockSkew() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtTokenService issuer = new JwtTokenService(SECRET, 3600, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.issue(new AuthenticatedUser(UUID.randomUUID(), User.Role.CUSTOMER));
        JwtTokenService atExpiration = new JwtTokenService(
                SECRET, 3600, Clock.fixed(issuedAt.plusSeconds(3600), ZoneOffset.UTC));
        JwtTokenService tenSecondsLate = new JwtTokenService(
                SECRET, 3600, Clock.fixed(issuedAt.plusSeconds(3610), ZoneOffset.UTC));

        assertThatThrownBy(() -> atExpiration.verify(token)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> tenSecondsLate.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAValidlySignedTokenWithAnUnsupportedRole() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600, Clock.systemUTC());
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "GUEST")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey()));
        String invalidRoleToken = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> tokenService.verify(invalidRoleToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsInvalidSecretAndExpirationConfiguration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JwtTokenService("too-short", 3600, Clock.systemUTC()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JwtTokenService(SECRET, 0, Clock.systemUTC()));
    }

    private static SecretKey secretKey() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
