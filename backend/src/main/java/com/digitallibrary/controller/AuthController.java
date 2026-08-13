package com.digitallibrary.controller;

import com.digitallibrary.audit.Audited;
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

import org.springframework.security.crypto.password.PasswordEncoder;
import com.digitallibrary.enums.UserRole;
import com.digitallibrary.enums.SubscriptionPlan;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomUserDetailsService userDetailsService,
                          AppUserRepository appUserRepository,
                          RefreshTokenService refreshTokenService,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.appUserRepository = appUserRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @Audited(action = "USER_REGISTER", entity = "User")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        AppUser newUser = new AppUser(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                UserRole.ROLE_USER
        );
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setSubscriptionPlan(SubscriptionPlan.FREE);
        newUser.setEmailVerified(true); // Default verified for demo/dev

        AppUser savedUser = appUserRepository.save(newUser);
        return ApiResponse.success("Account created successfully", UserResponse.fromEntity(savedUser));
    }

    @PostMapping("/login")
    @Audited(action = "USER_LOGIN", entity = "User")
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
}
