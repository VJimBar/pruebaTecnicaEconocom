package com.econocom.auth.service;

import com.econocom.auth.dto.AuthResponse;
import com.econocom.auth.dto.LoginRequest;
import com.econocom.auth.exception.InvalidCredentialsException;
import com.econocom.auth.exception.InvalidTokenException;

/**
 * Casos de uso de autenticación. El controlador depende de esta interfaz;
 * la lógica vive en {@link com.econocom.auth.service.impl.AuthServiceImpl}.
 */
public interface AuthService {

    /**
     * Autentica al usuario con email y contraseña.
     *
     * @param request credenciales recibidas
     * @return access token y refresh token
     * @throws InvalidCredentialsException si el usuario no existe o la contraseña no coincide
     */
    AuthResponse login(LoginRequest request);

    /**
     * Emite un nuevo par de tokens a partir de un refresh token válido.
     *
     * @param refreshToken refresh token recibido del cliente
     * @return nuevo access token y nuevo refresh token
     * @throws InvalidTokenException si el token no es válido, ha caducado
     *                               o no es de tipo refresh
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Revokes the supplied refresh token at logout.
     *
     * @param refreshToken refresh token received from the client
     */
    void logout(String refreshToken);

    /**
     * Emite tokens para un usuario autenticado vía SSO (sin verificación de contraseña).
     *
     * @param email correo del usuario SSO
     * @return access token y refresh token
     * @throws InvalidCredentialsException si el usuario no existe
     */
    AuthResponse loginByEmail(String email);
}
