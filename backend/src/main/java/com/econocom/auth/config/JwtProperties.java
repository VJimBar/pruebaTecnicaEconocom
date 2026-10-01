package com.econocom.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades JWT enlazadas desde la sección {@code jwt} del {@code application.yml}.
 * <p>
 * En Spring Boot la propiedad {@code jwt.access-expiration-ms} se enlaza
 * automáticamente al campo {@code accessExpirationMs} (relaxed binding).
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    // Clave de firma HMAC codificada en Base64 (mínimo 32 bytes decodificados).
    private String secret;

    // Vida del access token en milisegundos.
    private long accessExpirationMs;

    // Vida del refresh token en milisegundos.
    private long refreshExpirationMs;
}
