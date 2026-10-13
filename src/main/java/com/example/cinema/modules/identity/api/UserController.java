package com.example.cinema.modules.identity.api;

import com.example.cinema.modules.identity.business.IdentityService;
import com.example.cinema.modules.identity.business.User;
import com.example.cinema.shared.api.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final IdentityService identityService;

    public UserController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's public profile.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing, invalid, or no longer valid",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserResponse me(@AuthenticationPrincipal UUID userId, Authentication authentication) {
        User user = identityService.findAuthenticatedUser(userId);
        // The JWT filter supplies exactly one validated CUSTOMER/ADMIN role.
        User.Role role = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                ? User.Role.ADMIN : User.Role.CUSTOMER;
        return UserResponse.from(user, role);
    }
}
