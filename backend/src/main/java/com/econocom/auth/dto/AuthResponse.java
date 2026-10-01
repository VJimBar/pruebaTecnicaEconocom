package com.econocom.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta de {@code /login} y {@code /refresh}: el par de tokens emitido.
 */
@Getter
@AllArgsConstructor
public class AuthResponse {

    /** JWT de corta duración para acceder a los recursos protegidos. */
    private final String accessToken;

    /** JWT de larga duración que solo sirve para obtener un nuevo par de tokens. */
    private final String refreshToken;

    /** Esquema de autorización. Siempre {@code "Bearer"}. */
    private final String tokenType;

    /** Segundos de vida del access token (el frontend lo usa para programar el refresco). */
    private final long expiresIn;
}
