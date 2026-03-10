package com.maestros.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.maestros.exception.AuthenticationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    /**
     * Fixed message returned to the client for every authentication failure.
     * Intentionally vague to prevent user-enumeration and token-oracle attacks.
     */
    private static final String AUTH_ERROR_MSG = "Credenciales inválidas";

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    private void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * Verifies a Google ID token and returns the extracted user profile.
     *
     * <p>
     * Any failure — expired token, bad signature, network error, or unknown
     * audience —
     * is logged internally and surfaced to the caller as the same generic
     * {@link AuthenticationException} to prevent information leakage.
     * </p>
     *
     * @param googleIdToken the raw ID token string received from the client
     * @return {@link GoogleUserInfo} with email, display name, and picture URL
     * @throws AuthenticationException always with a fixed, non-informative message
     */
    public GoogleUserInfo verify(String googleIdToken) {
        try {
            GoogleIdToken idToken = verifier.verify(googleIdToken);

            if (idToken == null) {
                // verify() returns null for invalid/expired tokens
                log.warn("Google ID token verification returned null — token may be expired or tampered");
                throw new AuthenticationException(AUTH_ERROR_MSG);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            return new GoogleUserInfo(email, name, pictureUrl);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            // Covers GeneralSecurityException (bad signature, key fetch failure)
            // and IOException (network errors reaching Google's JWKS endpoint).
            log.error("Unexpected error during Google token verification", e);
            throw new AuthenticationException(AUTH_ERROR_MSG, e);
        }
    }
}
