package com.econocom.auth.service.impl;

import com.econocom.auth.config.JwtProperties;
import com.econocom.auth.exception.InvalidTokenException;
import com.econocom.auth.service.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias para {@link JwtServiceImpl}.
 * JwtServiceImplTest
 */
class JwtServiceImplTest {

    private static final String SECRET =
            "Y2xhdmUtc2VjcmV0YS1wYXJhLWxhLXBydWViYS10ZWNuaWNhLWVjb25vY29tLTIwMjYtbm8tdXNhci1lbi1wcm9kdWNjaW9u";

    @Test
    void accessTokenCaducadoEsRechazado() throws InterruptedException {
        JwtService service = createJwtService(50L, 1_000L);
        String token = service.generateAccessToken("prueba@econocom.com");

        Thread.sleep(100L);

        assertThrows(InvalidTokenException.class, () -> service.parse(token, JwtService.TYPE_ACCESS));
    }

    @Test
    void refreshTokenCaducadoEsRechazado() throws InterruptedException {
        JwtService service = createJwtService(1_000L, 50L);
        String token = service.generateRefreshToken("prueba@econocom.com");

        Thread.sleep(100L);

        assertThrows(InvalidTokenException.class, () -> service.parse(token, JwtService.TYPE_REFRESH));
    }

    private JwtService createJwtService(long accessTtl, long refreshTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessExpirationMs(accessTtl);
        properties.setRefreshExpirationMs(refreshTtl);
        return new JwtServiceImpl(properties);
    }
}
