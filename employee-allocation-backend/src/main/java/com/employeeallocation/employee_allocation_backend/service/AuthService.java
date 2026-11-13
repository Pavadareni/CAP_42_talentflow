
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;
import com.employeeallocation.employee_allocation_backend.dto.auth.LoginResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository,
            JwtEncoder jwtEncoder) {

        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(String email, String password) {

        // 1. Authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        // 2. Retrieve authenticated user
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // 3. Generate token timestamps
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        // 4. Create JWT claims
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("employee-allocation-system")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getEmail())
                .claim("roles", List.of("ROLE_" + user.getRole().name()))
                .build();

        // 5. Sign JWT using HS256
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        // 6. Return login response
        return new LoginResponse(
                token,
                "Bearer",
                expirationMs / 1000,
                user.getEmail(),
                user.getRole().name()
        );
    }
}
