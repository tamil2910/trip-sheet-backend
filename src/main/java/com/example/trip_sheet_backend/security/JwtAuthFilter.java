package com.example.trip_sheet_backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String userId = jwtTokenUtil.getUserIdFromSubject(token);
                String role = jwtTokenUtil.getRoleFromToken(token);
                String accountId = jwtTokenUtil.getUserIdFromToken(token);
                List<String> permissions = jwtTokenUtil.getPermissionsFromToken(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // Permissions as authorities
                    if (permissions != null) {
                        for (String p : permissions) {
                            authorities.add(new SimpleGrantedAuthority(p));
                        }
                    }

                    String tenantId = jwtTokenUtil.getTenantIdFromToken(token);
                    if (tenantId != null) {
                        authorities.add(new SimpleGrantedAuthority("TENANT_" + tenantId));
                    }
                    // Role as ROLE_XXX for hasRole() support
                    if (role != null  && !role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(accountId, null, authorities);

                    authToken.setDetails(userId);
                    // authToken.setDetails(accountId);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (Exception e) {
                System.out.println("❌ Invalid JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }    
}
