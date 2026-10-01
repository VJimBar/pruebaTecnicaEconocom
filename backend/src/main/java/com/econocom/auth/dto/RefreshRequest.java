package com.econocom.auth.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición {@code POST /api/auth/refresh}.
 * <p>
 * Ejemplo: {@code {"refreshToken":"eyJhbGciOi..."}}
 * </p>
 */
@Getter
@Setter
public class RefreshRequest {

    /** Refresh token obtenido en el login (o en un refresco anterior). */
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
