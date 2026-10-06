package com.example.cinema.modules.identity.api;

import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.business.AuthenticatedUser;
import com.example.cinema.modules.identity.business.IdentityService;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.shared.api.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Register a customer", description = "Creates a customer account. This endpoint is public.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Registration data is invalid",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        User user = identityService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user, user.role().name()));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates with username and password. This endpoint is public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Login data is invalid",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser user = identityService.login(request.username(), request.password());
        return ResponseEntity.ok(LoginResponse.bearerToken(
                jwtTokenService.issue(user), jwtTokenService.expirationSeconds()));
    }
}
