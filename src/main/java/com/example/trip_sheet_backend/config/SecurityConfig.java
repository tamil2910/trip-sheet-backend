package com.example.trip_sheet_backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.trip_sheet_backend.security.JwtAuthFilter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ✅ Enable @PreAuthorize / @PostAuthorize
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;
  private final List<String> allowedOriginPatterns;

  public SecurityConfig(
      JwtAuthFilter jwtAuthFilter,
      @Value("${app.cors.allowed-origin-patterns:http://localhost:4200,http://localhost:4400,http://127.0.0.1:4200,http://127.0.0.1:4400,https://trip-sheet-frontend.vercel.app,https://*.vercel.app}")
      String allowedOriginPatternsProperty) {
      this.jwtAuthFilter = jwtAuthFilter;
      this.allowedOriginPatterns =
          Arrays.stream(allowedOriginPatternsProperty.split(","))
              .map(String::trim)
              .filter(value -> !value.isEmpty())
              .toList();
  }

    // ✅ AuthenticationManager bean (needed for login endpoint if you add one later)
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
    // disable csrf for apis (you don't need it for json request)
      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/","/auth/register", "/auth/google-signup", "/auth/login", "/ping", "/api/ping", "/auth/**", "/feedback/**", "/ws/**").permitAll() // "/roles/**", "/accounts",
        .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
          .httpBasic(httpBasic -> httpBasic.disable())
          .formLogin(form -> form.disable());

    return http.build();
  }

  @Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
      var config = new org.springframework.web.cors.CorsConfiguration();
      config.setAllowedOriginPatterns(allowedOriginPatterns);
      config.addAllowedHeader("*");
      config.addAllowedMethod("*");
      config.setExposedHeaders(List.of("Authorization"));
      config.setAllowCredentials(true);

      var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", config);
      return source;
  }

}
