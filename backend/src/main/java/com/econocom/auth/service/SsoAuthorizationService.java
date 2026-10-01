package com.econocom.auth.service;

import com.econocom.auth.exception.InvalidTokenException;

/**
 * Contrato para iniciar y validar el flujo de autorización SSO simulado.
 */
public interface SsoAuthorizationService {

    /**
     * Crea el URL de retorno del cliente con un código de autorización y un state.
     *
     * @return URL al que se redirigirá al usuario para completar el flujo SSO
     */
    String createRedirectUrl();

    /**
     * Valida y consume el código SSO, que solo puede intercambiarse una vez.
     *
     * @param code código de autorización recibido en el callback
     * @param state valor state asociado al inicio del flujo
     * @return email autenticado por el proveedor simulado
     * @throws InvalidTokenException si faltan o no son válidos el código o el state
     */
    String exchange(String code, String state);
}
