package com.tracker.orders_api.controller.user;

import com.tracker.orders_api.controller.dto.LoginRequest;
import com.tracker.orders_api.controller.dto.LoginResponse;
import com.tracker.orders_api.controller.dto.RefreshTokenRequest;
import com.tracker.orders_api.entities.Role;
import com.tracker.orders_api.repository.UserRepository;
import com.tracker.orders_api.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
public class TokenController {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private BCryptPasswordEncoder passwordEncoder;



    public TokenController(JwtEncoder jwtEncoder,
                           UserRepository userRepository, RefreshTokenService refreshTokenService,
                           BCryptPasswordEncoder passwordEncoder){
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;

    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest){
       var user =  userRepository.findByEmail(loginRequest.email());

       if(user.isEmpty() || !user.get().isLoginCorrect(loginRequest, passwordEncoder)){
           throw new BadCredentialsException("user or password invalid!");
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
               .claim("email", user.get().getEmail())
               .claim("scope", scopes)
               .build();

       var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

       var refreshToken = refreshTokenService.createRefreshToken(user.get().getId()).getToken();
       return ResponseEntity.ok(new LoginResponse(jwtValue, refreshToken, expiresIn));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        return refreshTokenService.execute(request.refreshToken());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        return refreshTokenService.logout(request.refreshToken());
    }

}
