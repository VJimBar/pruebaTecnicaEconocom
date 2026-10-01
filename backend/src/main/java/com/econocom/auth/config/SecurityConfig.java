package com.econocom.auth.config;

import com.econocom.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;

/**
 * Configuración de Spring Security: API stateless con JWT, CORS para Angular
 * y codificación de contraseñas con BCrypt.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Origen del frontend Angular en desarrollo. */
    private static final String ANGULAR_ORIGIN = "http://localhost:4200";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Codificador de contraseñas. BCrypt incorpora sal y es lento a propósito
     * para dificultar ataques de fuerza bruta.
     *
     * @return codificador BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define un gestor de detalles de usuario en memoria vacío para deshabilitar
     * el usuario autogenerado de Spring Security y limpiar los logs de arranque.
     */
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
    }

    /**
     * Cadena de filtros de seguridad.
     * <ul>
     *   <li>CSRF desactivado y sin sesión HTTP: la autenticación va en el token, no en cookies.</li>
     *   <li>{@code /api/auth/login} y {@code /api/auth/refresh} son públicos
     *       (aquí se añadirán después las rutas del SSO).</li>
     *   <li>Cualquier otra ruta exige un access token válido.</li>
     *   <li>Sin autenticación se responde 401 con un JSON.</li>
     * </ul>
     *
     * @param http constructor de la configuración de seguridad
     * @return cadena de filtros configurada
     * @throws Exception si la configuración falla
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .headers().frameOptions().sameOrigin().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                        "/api/auth/sso", "/api/auth/sso/callback").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated().and()
            .exceptionHandling().authenticationEntryPoint((request, response, ex) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":401,\"message\":\"No autenticado\"}");
            }).and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Política CORS: permite al frontend Angular (localhost:4200) llamar a {@code /api/**}
     * enviando las cabeceras {@code Authorization} y {@code Content-Type}.
     *
     * @return fuente de configuración CORS (Spring la detecta por el nombre del bean)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList(ANGULAR_ORIGIN));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
