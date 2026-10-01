package com.econocom.auth.exception;

/**
 * Se lanza cuando un JWT no es válido: firma incorrecta, mal formado, caducado
 * o de un tipo distinto al esperado (por ejemplo, un access token usado como refresh).
 * Se traduce a HTTP 401.
 */
public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message motivo del rechazo, apto para mostrarlo al cliente
     */
    public InvalidTokenException(String message) {
        super(message);
    }
}
