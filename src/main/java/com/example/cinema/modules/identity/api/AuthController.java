package com.example.cinema.modules.identity.api;

import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.IdentityService;
import com.example.cinema.modules.identity.business.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityService identityService;
    private final JwtTokenService jwtTokenService;

    public AuthController(IdentityService identityService, JwtTokenService jwtTokenService) {
        this.identityService = identityService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        User user = identityService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser user = identityService.login(request.username(), request.password());
        return ResponseEntity.ok(LoginResponse.bearerToken(
                jwtTokenService.issue(user), jwtTokenService.expirationSeconds()));
    }
}
