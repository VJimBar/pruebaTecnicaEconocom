package com.econocom.auth.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición {@code POST /api/auth/login}.
 * <p>
 * Ejemplo: {@code {"email":"demo@test.com","password":"demo1234"}}
 * </p>
 */
@Getter
@Setter
public class LoginRequest {

    /** Correo del usuario. Obligatorio y con formato válido. */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    private String email;

    /** Contraseña en claro (solo viaja en esta petición, sobre HTTPS en un entorno real). */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
