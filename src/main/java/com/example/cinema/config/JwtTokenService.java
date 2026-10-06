package com.example.cinema.config;

import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public class JwtTokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final long expirationSeconds;

    JwtTokenService(String secret, long expirationSeconds, Clock clock) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_SECONDS must be greater than zero");
        }

        SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestampValidator));
        this.decoder = jwtDecoder;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("role", user.role().name())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public AuthenticatedUser verify(String token) throws JwtException {
        Jwt jwt = decoder.decode(token);
        String subject = jwt.getSubject();
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BadJwtException("JWT subject must be a UUID", exception);
        }
        if (!userId.toString().equals(subject)) {
            throw new BadJwtException("JWT subject must be a canonical UUID");
        }

        Object roleClaim = jwt.getClaims().get("role");
        if (!(roleClaim instanceof String roleName)) {
            throw new BadJwtException("JWT role claim must be a string");
        }
        User.Role role;
        try {
            role = User.Role.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            throw new BadJwtException("JWT role claim is invalid", exception);
        }

        Object issuedAtClaim = jwt.getClaims().get("iat");
        Object expiresAtClaim = jwt.getClaims().get("exp");
        Instant now = clock.instant();
        if (!(issuedAtClaim instanceof Instant issuedAt)
                || !(expiresAtClaim instanceof Instant expiresAt)
                || !expiresAt.isAfter(issuedAt)
                || !expiresAt.isAfter(now)
                || issuedAt.isAfter(now)) {
            throw new BadJwtException("JWT time claims are invalid");
        }
        return new AuthenticatedUser(userId, role);
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
