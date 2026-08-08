package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.AuthResponse;
import com.digitallibrary.dto.LoginRequest;
import com.digitallibrary.dto.UserResponse;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.security.JwtService;
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
    private final AppUserRepository appUserRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          AppUserRepository appUserRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        AppUser appUser = appUserRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return ApiResponse.success("Login successful", new AuthResponse(token, UserResponse.fromEntity(appUser)));
    }

    @GetMapping("/demo-users")
    public ApiResponse<String> demoUsers() {
        return ApiResponse.success("Demo accounts",
                "admin@library.com/admin123, user@library.com/user123, partner@library.com/partner123");
    }
}
