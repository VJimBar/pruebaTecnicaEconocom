package com.econocom.auth.exception;

import com.econocom.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones de los controladores a respuestas JSON con formato uniforme
 * ({@link ErrorResponse}), evitando que el cliente reciba trazas o páginas de error de Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Credenciales o token inválidos: HTTP 401.
     *
     * @param ex excepción capturada
     * @return respuesta 401 con el mensaje de la excepción
     */
    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Fallo de validación de un DTO ({@code @Valid}): HTTP 400 con los mensajes de cada campo.
     *
     * @param ex excepción capturada
     * @return respuesta 400 con los errores de validación unidos por "; "
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Cuerpo ausente o JSON mal formado: HTTP 400.
     *
     * @param ex excepción capturada
     * @return respuesta 400 genérica
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    /**
     * Errores no controlados: HTTP 500 con formato JSON homogéneo.
     *
     * @param ex excepción inesperada
     * @return respuesta 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno en el servidor");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, System.currentTimeMillis()));
    }
}
