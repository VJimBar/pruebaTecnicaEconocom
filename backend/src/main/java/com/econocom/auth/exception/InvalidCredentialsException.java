package com.econocom.auth.exception;

/**
 * Se lanza cuando el email o la contraseña no son correctos.
 * <p>
 * El mensaje es deliberadamente el mismo para "usuario inexistente" y "contraseña errónea",
 * para no revelar qué correos están registrados. Se traduce a HTTP 401.
 * </p>
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Credenciales incorrectas");
    }
}
