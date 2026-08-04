package com.tracker.orders_api.service;

import  com.tracker.orders_api.entities.RefreshToken;
import  com.tracker.orders_api.repository.RefreshTokenRepository;
import  com.tracker.orders_api.repository.UserRepository;
import  com.tracker.orders_api.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    public RefreshTokenService(RefreshTokenRepository repo, UserRepository userRepo, JwtUtils jwtUtils) {
        this.refreshTokenRepository = repo;
        this.userRepository = userRepo;
        this.jwtUtils = jwtUtils;
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
                        return ResponseEntity.badRequest().body("Refresh token expired. Please login again.");
                    }
                    String newJwt = jwtUtils.generateToken(token.getUser().getName());
                    return ResponseEntity.ok(Map.of("token", newJwt));
                })
                .orElse(ResponseEntity.badRequest().body("Invalid refresh token."));
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
}