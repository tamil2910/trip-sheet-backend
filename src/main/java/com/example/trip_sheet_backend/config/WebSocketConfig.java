package com.example.trip_sheet_backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final List<String> allowedOriginPatterns;

  public WebSocketConfig(
      @Value("${app.cors.allowed-origin-patterns:http://localhost:4200,http://localhost:4400,http://127.0.0.1:4200,http://127.0.0.1:4400,https://trip-sheet-frontend.vercel.app,https://*.vercel.app}")
      String allowedOriginPatternsProperty) {
    this.allowedOriginPatterns = Arrays.stream(allowedOriginPatternsProperty.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/trips")
        .setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
        .withSockJS();
  }
}
