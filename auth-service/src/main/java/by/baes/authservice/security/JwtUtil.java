package by.baes.authservice.security;

import by.baes.authservice.entity.Role;
import by.baes.authservice.entity.User;
import by.baes.authservice.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Collectors;


@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(User user) {
        log.debug("Generating token for user: {}", user.getName());
        String token = Jwts.builder()
                .setSubject(user.getName())
                .claim("roles", user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
        log.debug("Token generated for user: {}", user.getName());
        return token;
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid JWT token: " + e.getMessage());
        }
    }

    public long getExpiration() {
        return expiration;
    }
}