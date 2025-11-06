package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.config.JwtTokenConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

/**
 * JWT Token Service for encoding and decoding JWT tokens.
 */
@Service
@Getter
public class JwtTokenService {


    private final JwtTokenConfiguration jwtTokenConfiguration;

    public JwtTokenService(JwtTokenConfiguration jwtTokenConfiguration) {
        this.jwtTokenConfiguration = jwtTokenConfiguration;
    }

    private SecretKey getSigningKey() {
        final String secretKey = jwtTokenConfiguration.getSecretKey();
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims decode(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String encode(String principle) {
        final Duration expiredTime = jwtTokenConfiguration.getExpiredTime();
        final long now = System.currentTimeMillis();
        final long expiredAt = now + expiredTime.toMillis();
        return Jwts.builder()
                .subject(principle)
                .issuedAt(new Date(now))
                .expiration(new Date(expiredAt))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isValidToken(String jwt) {
        return decode(jwt).getExpiration().after(new Date());
    }
}