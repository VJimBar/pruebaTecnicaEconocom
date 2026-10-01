package com.econocom.auth.service;

import com.econocom.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;

/**
 * Contrato para emitir y validar JSON Web Tokens.
 * <p>
 * Se emiten dos tipos de token, distinguidos por el claim {@code type}:
 * </p>
 * <ul>
 *   <li><b>access</b>: vida corta, se envía en cada petición ({@code Authorization: Bearer ...}).</li>
 *   <li><b>refresh</b>: vida larga, solo sirve para obtener un nuevo par de tokens.</li>
 * </ul>
 */
public interface JwtService {

    /** Valor del claim {@code type} para los access tokens. */
    String TYPE_ACCESS = "access";

    /** Valor del claim {@code type} para los refresh tokens. */
    String TYPE_REFRESH = "refresh";

    /**
     * Genera un access token firmado.
     *
     * @param email correo del usuario (queda como {@code subject} del token)
     * @return JWT compacto
     */
    String generateAccessToken(String email);

    /**
     * Genera un refresh token firmado.
     *
     * @param email correo del usuario (queda como {@code subject} del token)
     * @return JWT compacto
     */
    String generateRefreshToken(String email);

    /**
     * @return segundos de vida configurados para el access token
     */
    long accessTtlSeconds();

    /**
     * Valida un token (firma, caducidad y tipo) y devuelve su contenido.
     *
     * @param token        JWT recibido del cliente
     * @param expectedType {@link #TYPE_ACCESS} o {@link #TYPE_REFRESH}
     * @return los claims del token si es válido
     * @throws InvalidTokenException si está caducado, mal formado, con firma inválida
     *                               o es de un tipo distinto al esperado
     */
    Claims parse(String token, String expectedType);

    /**
     * Consume a refresh token so it cannot be replayed after rotation or logout.
     *
     * @param tokenId the refresh token's unique JWT ID
     * @throws InvalidTokenException if the token has already been consumed
     */
    void consumeRefreshToken(String tokenId);
}
