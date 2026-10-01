package com.econocom.auth.config;

import com.econocom.auth.model.User;
import com.econocom.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos de prueba para la base de datos H2.
 * <p>
 * Inserta el usuario de demostración al arrancar si no existe previamente:
 * <ul>
 *   <li><b>Email:</b> prueba@econocom.com</li>
 *   <li><b>Contraseña:</b> pass1234 (cifrada con BCrypt)</li>
 *   <li><b>Nombre:</b> Usuario Demo</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String demoEmail = "prueba@econocom.com";
        if (!userRepository.existsById(demoEmail)) {
            User demoUser = new User(
                    demoEmail,
                    passwordEncoder.encode("pass1234"),
                    "Usuario Demo"
            );
            userRepository.save(demoUser);
            log.info("Usuario demo inicializado en H2: {}", demoEmail);
        }
    }
}
