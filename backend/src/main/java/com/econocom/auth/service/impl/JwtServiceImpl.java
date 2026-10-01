package com.econocom.auth.service.impl;

import com.econocom.auth.config.JwtProperties;
import com.econocom.auth.exception.InvalidTokenException;
import com.econocom.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementación de {@link JwtService} con la librería JJWT (algoritmo HS256).
 * <p>
 * La clave de firma y los tiempos de vida se leen de {@link JwtProperties}
 * (sección {@code jwt} del {@code application.yml}).
 * </p>
 */
@Service
public class JwtServiceImpl implements JwtService {

    /** Nombre del claim que distingue access de refresh. */
    private static final String CLAIM_TYPE = "type";

    private final Key key;
    private final JwtProperties properties;
    private final ConcurrentMap<String, Long> activeRefreshTokens = new ConcurrentHashMap<>();

    /**
     * Construye la clave HMAC a partir del secreto Base64 configurado.
     *
     * @param properties propiedades JWT (secreto y caducidades)
     */
    public JwtServiceImpl(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    /** {@inheritDoc} */
    @Override
    public String generateAccessToken(String email) {
        return build(email, TYPE_ACCESS, properties.getAccessExpirationMs());
    }

    /** {@inheritDoc} */
    @Override
    public String generateRefreshToken(String email) {
        return build(email, TYPE_REFRESH, properties.getRefreshExpirationMs());
    }

    /** {@inheritDoc} */
    @Override
    public long accessTtlSeconds() {
        return properties.getAccessExpirationMs() / 1000;
    }

    /**
     * Flujo de validación:
     * <ol>
     *   <li>JJWT comprueba la firma y la fecha de caducidad al parsear.</li>
     *   <li>Se comprueba que el claim {@code type} sea el esperado.</li>
     *   <li>Cualquier fallo se traduce a {@link InvalidTokenException} (HTTP 401).</li>
     * </ol>
     */
    @Override
    public Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("Tipo de token incorrecto");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("El token ha caducado");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token inválido");
        }
    }

    @Override
    public void consumeRefreshToken(String tokenId) {
        if (tokenId == null || activeRefreshTokens.remove(tokenId) == null) {
            throw new InvalidTokenException("El refresh token ya ha sido utilizado o revocado");
        }
    }

    /**
     * Construye y firma un JWT.
     *
     * @param subject email del usuario
     * @param type    {@link #TYPE_ACCESS} o {@link #TYPE_REFRESH}
     * @param ttlMs   vida del token en milisegundos
     * @return JWT compacto firmado
     */
    private String build(String subject, String type, long ttlMs) {
        Date now = new Date();
        long expirationTime = now.getTime() + ttlMs;
        String tokenId = TYPE_REFRESH.equals(type) ? UUID.randomUUID().toString() : null;

        String token = Jwts.builder()
                .setSubject(subject)
                .claim(CLAIM_TYPE, type)
                .setId(tokenId)
                .setIssuedAt(now)
                .setExpiration(new Date(expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        if (tokenId != null) {
            removeExpiredRefreshTokens(now.getTime());
            activeRefreshTokens.put(tokenId, expirationTime);
        }
        return token;
    }

    private void removeExpiredRefreshTokens(long now) {
        for (Map.Entry<String, Long> entry : activeRefreshTokens.entrySet()) {
            if (entry.getValue() <= now) {
                activeRefreshTokens.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
