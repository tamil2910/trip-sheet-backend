package com.example.trip_sheet_backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service // ✅ This makes Spring detect and inject it automatically
public class GoogleAuthService {

    @Value("${GOOGLE_AUTH_CLIENT_ID}")
    private String clientId;

    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList(clientId))
            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                System.out.println("✅ Parsed Audience: " + payload.getAudience());
                System.out.println("✅ Issuer: " + payload.getIssuer());
                System.out.println("✅ Email: " + payload.getEmail());
                return payload;
            } else {
                throw new RuntimeException("Invalid Google ID Token");
            }

        } catch (Exception e) {
          System.out.println("❌ Raw token: " + idTokenString);
          System.out.println("❌ ClientId in backend: " + clientId);
          try {
              GoogleIdToken parsed = GoogleIdToken.parse(JacksonFactory.getDefaultInstance(), idTokenString);
              System.out.println("Parsed Audience: " + parsed.getPayload().getAudience());
              System.out.println("Parsed Issuer: " + parsed.getPayload().getIssuer());
          } catch (Exception ignored) {}
          throw new RuntimeException("Google token verification failed: " + e.getMessage());
        }
    }
}
