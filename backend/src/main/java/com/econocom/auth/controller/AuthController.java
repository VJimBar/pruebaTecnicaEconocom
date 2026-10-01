package com.econocom.auth.controller;

import com.econocom.auth.dto.AuthResponse;
import com.econocom.auth.dto.LoginRequest;
import com.econocom.auth.dto.RefreshRequest;
import com.econocom.auth.service.AuthService;
import com.econocom.auth.service.SsoAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints REST de autenticación bajo {@code /api/auth}.
 * <p>
 * El controlador solo traduce HTTP a llamadas al servicio: no contiene lógica de negocio.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SsoAuthorizationService ssoAuthorizationService;

    /**
     * {@code POST /api/auth/login}: autentica con email y contraseña.
     * <ul>
     *   <li>200: devuelve access y refresh token.</li>
     *   <li>400: cuerpo inválido (email vacío o con formato incorrecto, etc.).</li>
     *   <li>401: credenciales incorrectas.</li>
     * </ul>
     *
     * @param request credenciales (validadas con {@code @Valid})
     * @return par de tokens
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * {@code POST /api/auth/refresh}: intercambia un refresh token válido por un par nuevo.
     * <ul>
     *   <li>200: nuevo par de tokens.</li>
     *   <li>401: refresh token inválido, caducado o de tipo incorrecto.</li>
     * </ul>
     *
     * @param request cuerpo con el refresh token
     * @return nuevo par de tokens
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    /**
     * {@code POST /api/auth/logout}: finaliza la sesión del usuario.
     * <ul>
     *   <li>204: sesión finalizada con éxito.</li>
     *   <li>401: refresh token inválido, caducado o de tipo incorrecto.</li>
     * </ul>
     *
     * @param request cuerpo con el refresh token
     * @return respuesta 204
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /api/auth/validate}: ruta protegida que sirve para comprobar un access token.
     * Si la petición llega hasta aquí es que el filtro JWT ya validó el token.
     * <ul>
     *   <li>200: token válido, devuelve el email autenticado.</li>
     *   <li>401: sin token, caducado o inválido.</li>
     * </ul>
     *
     * @param authentication usuario autenticado por {@code JwtAuthenticationFilter}
     * @return {@code {"valid":true,"email":"..."}}
     */
    @GetMapping("/validate")
    public Map<String, Object> validate(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", true);
        body.put("email", authentication.getName());
        return body;
    }

    /**
     * {@code GET /api/auth/sso}: inicia el flujo SSO simulado.
     * <p>
     * Genera un código de autorización de un solo uso y un parámetro {@code state},
     * y responde con un 302 al callback frontend configurado.
     * </p>
     *
     * @return respuesta 302 con cabecera {@code Location}
     */
    @GetMapping("/sso")
    public ResponseEntity<Void> ssoRedirect() {
        URI redirectUri = URI.create(ssoAuthorizationService.createRedirectUrl());
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    /**
     * {@code GET /api/auth/sso/callback}: recibe el callback del proveedor SSO simulado.
     * <ul>
     *   <li>200: código y state válidos, devuelve tokens JWT para el usuario demo.</li>
     *   <li>401: código/state ausente, inválido, caducado o ya utilizado.</li>
     * </ul>
     *
     * @param code código de autorización simulado recibido del proveedor SSO
     * @return par de tokens si el código es válido
     */
    @GetMapping("/sso/callback")
    public AuthResponse ssoCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state) {
        return authService.loginByEmail(ssoAuthorizationService.exchange(code, state));
    }
}
