package com.econocom.auth.security;

import com.econocom.auth.exception.InvalidTokenException;
import com.econocom.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtro que se ejecuta una vez por petición y autentica al usuario a partir del JWT.
 * <p>
 * Flujo de ejecución:
 * </p>
 * <ol>
 *   <li>Lee la cabecera {@code Authorization: Bearer <token>}.</li>
 *   <li>Si el access token es válido, deja al usuario autenticado en el {@code SecurityContext}.</li>
 *   <li>Si no hay token o no es válido, no autentica: las rutas protegidas responderán 401
 *       mediante el {@code authenticationEntryPoint} de {@link com.econocom.auth.config.SecurityConfig}.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()),
                        JwtService.TYPE_ACCESS);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException e) {
                // Token caducado o inválido: se deja el contexto sin autenticar (-> 401 si la ruta es protegida).
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
