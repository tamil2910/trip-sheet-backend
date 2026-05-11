package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.response_setups.ApiResponse;

@RestController
@RequestMapping("/")
public class DebugController {

    @GetMapping("api/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> data = new HashMap<>();
        if (auth == null) {
            data.put("authenticated", false);
        } else {
            data.put("authenticated", auth.isAuthenticated());
            data.put("principal", auth.getPrincipal());
            data.put("details", auth.getDetails());
            data.put("authorities", auth.getAuthorities() == null ? null : auth.getAuthorities().stream()
                    .map(Object::toString).collect(Collectors.toList()));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "pong", data));
    }
}
