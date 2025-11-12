package com.example.trip_sheet_backend.security;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil {

    private static final String SECRET_KEY = "this_is_a_super_secret_jwt_key_which_is_very_long_and_secure_123456";
    private static final long EXPIRATION_TIME = 86400000; // 1 day in milliseconds

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // ✅ Generate token
    public String generateToken(String identifier, String role, String type) {
      return Jwts.builder()
          .setSubject(identifier)
          .claim("role", role)
          .claim("type", type)
          .setIssuedAt(new Date(System.currentTimeMillis()))
          .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
          .signWith(key, SignatureAlgorithm.HS256)
          .compact();
    }

    // ✅ Validate token
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ✅ Extract email (subject)
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // ✅ Extract role claim
    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    // ✅ Get expiration date
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // ✅ Core claim parsing methods
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}
