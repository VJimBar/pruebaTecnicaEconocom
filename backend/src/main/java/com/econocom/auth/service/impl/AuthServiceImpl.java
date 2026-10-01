package com.econocom.auth.service.impl;

import com.econocom.auth.dto.AuthResponse;
import com.econocom.auth.dto.LoginRequest;
import com.econocom.auth.exception.InvalidCredentialsException;
import com.econocom.auth.exception.InvalidTokenException;
import com.econocom.auth.model.User;
import com.econocom.auth.repository.UserRepository;
import com.econocom.auth.service.AuthService;
import com.econocom.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link AuthService}.
 * <p>
 * Colaboradores (inyectados por constructor gracias a {@code @RequiredArgsConstructor}):
 * repositorio de usuarios, codificador de contraseñas y servicio JWT.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Flujo de ejecución:
     * <ol>
     *   <li>Busca el usuario por email.</li>
     *   <li>Compara la contraseña recibida con el hash BCrypt almacenado.</li>
     *   <li>Si algo falla, lanza siempre la misma excepción (no revela qué campo falló).</li>
     *   <li>Si todo es correcto, emite el par de tokens.</li>
     * </ol>
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user.getEmail());
    }

    /**
     * Flujo de ejecución:
     * <ol>
     *   <li>Valida el refresh token (firma, caducidad y tipo "refresh").</li>
     *   <li>Comprueba que el usuario del token siga existiendo.</li>
     *   <li>Emite un par de tokens nuevo.</li>
     * </ol>
     */
    @Override
    public AuthResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken, JwtService.TYPE_REFRESH);
        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new InvalidTokenException("Usuario no válido"));
        jwtService.consumeRefreshToken(claims.getId());
        return issueTokens(user.getEmail());
    }

    /**
     * Finaliza la sesión del usuario consumiendo el refresh token.
     *
     * @param refreshToken el token de refresco a consumir
     */
    @Override
    public void logout(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken, JwtService.TYPE_REFRESH);
        jwtService.consumeRefreshToken(claims.getId());
    }

    /**
     * Login SSO: emite tokens directamente para el email indicado si el usuario existe.
     * No requiere contraseña porque el proveedor SSO ya ha autenticado al usuario.
     */
    @Override
    public AuthResponse loginByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user.getEmail());
    }

    /** Genera access + refresh token para el email indicado. */
    private AuthResponse issueTokens(String email) {
        return new AuthResponse(
                jwtService.generateAccessToken(email),
                jwtService.generateRefreshToken(email),
                "Bearer",
                jwtService.accessTtlSeconds());
    }
}
