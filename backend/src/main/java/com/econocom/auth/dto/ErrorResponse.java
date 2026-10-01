package com.econocom.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Formato único de error que devuelve la API.
 * <p>
 * Ejemplo: {@code {"status":401,"message":"Credenciales incorrectas","timestamp":1727600000000}}
 * </p>
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    /** Código HTTP de la respuesta. */
    private final int status;

    /** Mensaje legible para el cliente. */
    private final String message;

    /** Momento del error en milisegundos desde epoch. */
    private final long timestamp;
}
