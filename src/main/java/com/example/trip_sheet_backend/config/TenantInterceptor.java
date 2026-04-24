package com.example.trip_sheet_backend.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.TenantRepository;
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

  @Autowired
  private TenantRepository tenantRepository;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return true;
    }

    String token = authHeader.replace("Bearer ", "");
    UUID userId = UUID.fromString(jwtTokenUtil.getUserIdFromToken(token));

    UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    String role = jwtTokenUtil.getRoleFromToken(token);

    // ---------------- SUPER ADMIN ----------------
    if ("SUPER_ADMIN".equals(role)) {
        setCommonAttributes(request, user);
        request.setAttribute("tenantId", null);
        request.setAttribute("tenant", null);
        request.setAttribute("isSuperAdmin", true);
        return true;
    }

    // ---------------- ADMIN WITHOUT TENANT (PRE TENANT) ----------------
    if (user.getTenant() == null) {
        // ✅ Allow admin to create tenant
        setCommonAttributes(request, user);
        request.setAttribute("tenantId", null);
        request.setAttribute("tenant", null);
        return true;
    }

    // ---------------- NORMAL TENANT USERS ----------------
    Tenant tenant = user.getTenant();

    if (tenant == null) {
      String tenantIdFromToken = jwtTokenUtil.getTenantIdFromToken(token);
      if (tenantIdFromToken != null && !tenantIdFromToken.isBlank()) {
        tenant = tenantRepository.findById(UUID.fromString(tenantIdFromToken))
            .orElse(null);
      }
    }

    setCommonAttributes(request, user);
    if (tenant != null) {
      request.setAttribute("tenantId", tenant.getId());
      request.setAttribute("tenant", tenant);
    } else {
      request.setAttribute("tenantId", null);
      request.setAttribute("tenant", null);
    }

    return true;
  }

  private void setCommonAttributes(HttpServletRequest request, UserAccount user) {
    request.setAttribute("user", user);
    request.setAttribute("userId", user.getId());
    request.setAttribute("createdBy", user.getId());
    request.setAttribute("updatedBy", user.getId());
  }

}
