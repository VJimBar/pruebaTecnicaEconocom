package com.econocom.auth.service.impl;

import com.econocom.auth.exception.InvalidTokenException;
import com.econocom.auth.service.SsoAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Proveedor SSO de demostración: emite códigos de autorización de un solo uso
 * y los valida en el callback.
 */
@Service
public class SsoAuthorizationServiceImpl implements SsoAuthorizationService {

    private static final long AUTHORIZATION_CODE_TTL_MS = 120_000L;
    private static final String DEMO_EMAIL = "prueba@econocom.com";

    private final String redirectUri;
    private final ConcurrentMap<String, AuthorizationGrant> grants = new ConcurrentHashMap<>();

    public SsoAuthorizationServiceImpl(
            @Value("${auth.sso.redirect-uri:http://localhost:4200/sso/callback}") String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @Override
    public String createRedirectUrl() {
        long now = System.currentTimeMillis();
        removeExpiredGrants(now);

        String code = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();
        grants.put(code, new AuthorizationGrant(DEMO_EMAIL, state, now + AUTHORIZATION_CODE_TTL_MS));

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public String exchange(String code, String state) {
        if (code == null || state == null) {
            throw new InvalidTokenException("Falta el código o el parámetro state de SSO");
        }

        AuthorizationGrant grant = grants.get(code);
        if (grant == null || grant.expiresAt <= System.currentTimeMillis()) {
            grants.remove(code);
            throw new InvalidTokenException("El código SSO no existe o ha caducado");
        }
        if (!grant.state.equals(state)) {
            throw new InvalidTokenException("El parámetro state de SSO no es válido");
        }
        if (!grants.remove(code, grant)) {
            throw new InvalidTokenException("El código SSO ya ha sido utilizado");
        }
        return grant.email;
    }

    private void removeExpiredGrants(long now) {
        for (Map.Entry<String, AuthorizationGrant> entry : grants.entrySet()) {
            if (entry.getValue().expiresAt <= now) {
                grants.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class AuthorizationGrant {
        private final String email;
        private final String state;
        private final long expiresAt;

        private AuthorizationGrant(String email, String state, long expiresAt) {
            this.email = email;
            this.state = state;
            this.expiresAt = expiresAt;
        }
    }
}
