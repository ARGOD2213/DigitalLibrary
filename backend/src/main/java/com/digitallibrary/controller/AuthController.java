package com.digitallibrary.controller;

import com.digitallibrary.dto.*;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.RefreshToken;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.security.CustomUserDetailsService;
import com.digitallibrary.security.JwtService;
import com.digitallibrary.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomUserDetailsService userDetailsService,
                          AppUserRepository appUserRepository,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.appUserRepository = appUserRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);
        AppUser appUser = appUserRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(appUser);

        return ApiResponse.success("Login successful",
                new AuthResponse(accessToken, refreshToken.getToken(), UserResponse.fromEntity(appUser)));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        AppUser user = newRefreshToken.getUser();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        return ApiResponse.success("Token refreshed successfully",
                new TokenRefreshResponse(newAccessToken, newRefreshToken.getToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestBody(required = false) LogoutRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }
        return ApiResponse.success("Logout successful", "Session ended");
    }

    @GetMapping("/demo-users")
    public ApiResponse<String> demoUsers() {
        return ApiResponse.success("Demo accounts",
                "admin@library.com/admin123, user@library.com/user123, vendor@library.com/vendor123");
    }
}
