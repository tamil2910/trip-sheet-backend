package com.example.trip_sheet_backend.security;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.trip_sheet_backend.models.UserAccount;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil {

 // Read directly from OS environment
    private static final String SECRET_KEY =
            System.getenv("JWT_SECRET");

    private final Key key;

    public JwtTokenUtil() {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalStateException(
                "Missing env variable: JWT_SECRET"
            );
        }
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    private static final long EXPIRATION_TIME = 86400000; // 1 day in milliseconds


    // ✅ Generate token
    public String generateToken(String identifier, String role, 
        String type, UUID user_id, Set<String> permissions) {
      return Jwts.builder()
        //   .setSubject(identifier)
        .setSubject(user_id.toString()) 
        .claim("role", role)
        .claim("type", type)
        .claim("user_id", user_id.toString())
        .claim("permissions", permissions)         // NEW
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
    }

    public String generateToken(UserAccount user, Set<String> permissions, String type, String identifier) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // or username/phone
                .claim("role", user.getRole() != null ? user.getRole().getName() : null)
                .claim("type", type)
                .claim("user_id", user.getId().toString())
                .claim("permissions", permissions) 
                .claim("identifier", identifier)
                .claim("tenant_id", user.getTenant() != null ? user.getTenant().getId().toString() : null)
                .claim("tenant_name", user.getTenant() != null ? user.getTenant().getTenantName() : null)
                .claim("tenant_type", user.getTenant() != null && user.getTenant().getTenantType() != null
                                ? user.getTenant().getTenantType().name()
                                : null)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateTokenWithoutIdentifier(UserAccount user, Set<String> permissions, String type) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // or username/phone
                .claim("role", user.getRole() != null ? user.getRole().getName() : null)
                .claim("type", type)
                .claim("user_id", user.getId().toString())
                .claim("permissions", permissions) 
                .claim("tenant_id", user.getTenant() != null ? user.getTenant().getId().toString() : null)
                .claim("tenant_name", user.getTenant() != null ? user.getTenant().getTenantName() : null)
                .claim("tenant_type", user.getTenant() != null && user.getTenant().getTenantType() != null
                                ? user.getTenant().getTenantType().name()
                                : null)
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

    // ✅ Extract userId (subject)
    public String getUserIdFromSubject(String token) {
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

    public String getUserIdFromToken(String token) {
        return getAllClaimsFromToken(token).get("user_id", String.class);
    }

    public List<String> getPermissionsFromToken(String token) {
            Claims claims = getAllClaimsFromToken(token);

        Object permissionsObj = claims.get("permissions");

        if (permissionsObj instanceof List<?>) {
            return ((List<?>) permissionsObj).stream()
                    .map(Object::toString) // safe conversion
                    .collect(Collectors.toList());
        }
        return List.of(); // return empty list if none
    }

    public String getTenantIdFromToken(String token) {
        return getAllClaimsFromToken(token).get("tenant_id", String.class);
    }



}
