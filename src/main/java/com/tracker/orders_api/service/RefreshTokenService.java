package com.tracker.orders_api.service;

import com.tracker.orders_api.controller.dto.RefreshTokenResponse;
import  com.tracker.orders_api.entities.RefreshToken;
import com.tracker.orders_api.entities.Role;
import  com.tracker.orders_api.repository.RefreshTokenRepository;
import  com.tracker.orders_api.repository.UserRepository;
import  com.tracker.orders_api.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {
    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public RefreshTokenService(RefreshTokenRepository repo, UserRepository userRepo,  JwtEncoder jwtEncoder) {
        this.refreshTokenRepository = repo;
        this.userRepository = userRepo;
        this.jwtEncoder = jwtEncoder;
    }

    public RefreshToken createRefreshToken(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        var existingToken  = refreshTokenRepository.findByUser(user);

        if(existingToken.isPresent() && !isTokenExpired(existingToken.get()))
            return existingToken.get();

        RefreshToken refreshToken =
                existingToken.orElseGet(RefreshToken::new);

        Objects.requireNonNull(refreshToken).setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(refreshToken);
    }

    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    public ResponseEntity<?> execute(String refreshToken){
        return refreshTokenRepository.findByToken(refreshToken)
                .map(token -> {
                    if (isTokenExpired(token)) {
                        refreshTokenRepository.delete(token);
                        RefreshTokenResponse newJwt = generateToken(token.getUser().getName());

                        return ResponseEntity.ok(newJwt);
                    }
                    return ResponseEntity.ok(Map.of("refreshToken", token));

                })
                .orElse(ResponseEntity.badRequest().body(Map.of("message","Invalid refresh token.")));
    }

    public ResponseEntity<?> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token is required.");
        }

        return refreshTokenRepository.findByToken(refreshToken)
                .map(token -> {
                    refreshTokenRepository.delete(token);
                    return ResponseEntity.ok("Logged out successfully.");
                })
                .orElse(ResponseEntity.badRequest().body("Invalid refresh token."));

    }

    public RefreshTokenResponse generateToken(String name){
        var user =  userRepository.findByName(name);

        if(user.isEmpty() ){
            throw new BadCredentialsException("user invalid!");
        }

        var now = Instant.now();
        var expiresIn = 300L;

        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("mybackend")
                .subject(user.get().getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("name", user.get().getName())
                .claim("scope", scopes)
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        var refreshToken = createRefreshToken(user.get().getId()).getToken();

        return new RefreshTokenResponse(jwtValue, refreshToken);
    }
}