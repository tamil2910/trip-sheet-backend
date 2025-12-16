package com.example.trip_sheet_backend.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import com.example.trip_sheet_backend.security.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantInterceptor implements HandlerInterceptor {

  @Autowired
  private JwtTokenUtil jwtTokenUtil;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return true; // Skip for unauthenticated endpoints
    }

    String token = authHeader.replace("Bearer ", "");
    UUID userId = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Get roles from token
    String role = jwtTokenUtil.getRoleFromToken(token);

    if (role.equals("SUPER_ADMIN")) {
      request.setAttribute("tenantId", null);
      request.setAttribute("tenant", null);
      request.setAttribute("isSuperAdmin", true);
      return true;
    }

        // Normal Users → set tenantId
    Tenant tenant = user.getTenant();
    if (tenant == null) {
        throw new RuntimeException("User does not belong to any tenant");
    }
    // Store tenant in request attribute for easy access in controllers
    request.setAttribute("tenantId", user.getTenant().getId());
    request.setAttribute("tenant", user.getTenant());
    request.setAttribute("userId", user.getId());
    request.setAttribute("user", user);
    request.setAttribute("createdBy", user.getId());
    request.setAttribute("updatedBy", user.getId());

    return true;
  }
}
