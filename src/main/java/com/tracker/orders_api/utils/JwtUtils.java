package com.tracker.orders_api.utils;

import com.tracker.orders_api.entities.Role;
import com.tracker.orders_api.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;

    public JwtUtils(JwtEncoder jwtEncoder, UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
    }

    public String generateToken(String name){
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

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
