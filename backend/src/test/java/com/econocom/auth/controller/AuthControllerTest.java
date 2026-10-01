package com.econocom.auth.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del login: arrancan el contexto completo y lanzan
 * peticiones HTTP simuladas
 * con MockMvc (no se levanta un servidor real). Ejecutar con: {@code mvn test}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController - Pruebas de integración de autenticación")
class AuthControllerTest {

        // NOTA: el usuario test se inserta automáticamente en H2 gracias a
        // DataInitializer
        private static final String VALID_LOGIN = "{\"email\":\"prueba@econocom.com\",\"password\":\"pass1234\"}";

        @Autowired
        private MockMvc mvc;

        @Test
        @DisplayName("Login correcto: devuelve 200 OK con accessToken y refreshToken")
        void loginCorrecto_devuelveTokens() throws Exception {
                mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("Login con clave incorrecta: devuelve 401 Unauthorized y mensaje de error")
        void loginConClaveIncorrecta_devuelve401() throws Exception {
                mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"prueba@econocom.com\",\"password\":\"incorrecta\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
        }

        @Test
        @DisplayName("Login con email inválido: devuelve 400 Bad Request")
        void loginConEmailInvalido_devuelve400() throws Exception {
                mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"no-es-un-email\",\"password\":\"x\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Validación sin token: devuelve 401 Unauthorized al acceder a ruta protegida")
        void validateSinToken_devuelve401() throws Exception {
                mvc.perform(get("/api/auth/validate"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Flujo completo: login -> validación de acceso -> refresco de token -> validación con nuevo token")
        void flujoCompleto_login_validate_refresh() throws Exception {
                // 1) Login
                String body = mvc
                                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(VALID_LOGIN))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                String accessToken = JsonPath.read(body, "$.accessToken");
                String refreshToken = JsonPath.read(body, "$.refreshToken");

                // 2) El access token da acceso a la ruta protegida
                mvc.perform(get("/api/auth/validate").header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("prueba@econocom.com"));

                // 3) El refresh token NO sirve como access token
                mvc.perform(get("/api/auth/validate").header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isUnauthorized());

                // 4) El refresh token da un par nuevo
                mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty());

                // 4b) El refresh token anterior se revoca al rotarlo y no se puede reutilizar
                mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                                .andExpect(status().isUnauthorized());

                // 5) El access token NO sirve como refresh token
                mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("SSO: redirige con code y state y solo acepta el código una vez")
        void ssoCallbackConsumeCodigoUnaSolaVez() throws Exception {
                MvcResult redirect = mvc.perform(get("/api/auth/sso"))
                                .andExpect(status().isFound())
                                .andReturn();
                String location = redirect.getResponse().getHeader("Location");
                String code = UriComponentsBuilder.fromUriString(location).build()
                                .getQueryParams().getFirst("code");
                String state = UriComponentsBuilder.fromUriString(location).build()
                                .getQueryParams().getFirst("state");

                mvc.perform(get("/api/auth/sso/callback").param("code", code).param("state", state))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

                mvc.perform(get("/api/auth/sso/callback").param("code", code).param("state", state))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("SSO: rechaza state incorrecto")
        void ssoCallbackConStateIncorrecto_devuelve401() throws Exception {
                MvcResult redirect = mvc.perform(get("/api/auth/sso"))
                                .andExpect(status().isFound())
                                .andReturn();
                String location = redirect.getResponse().getHeader("Location");
                String code = UriComponentsBuilder.fromUriString(location).build()
                                .getQueryParams().getFirst("code");

                mvc.perform(get("/api/auth/sso/callback").param("code", code).param("state", "incorrecto"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Logout: revoca el refresh token actual")
        void logoutRevocaRefreshToken() throws Exception {
                String body = mvc
                                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(VALID_LOGIN))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                String refreshToken = JsonPath.read(body, "$.refreshToken");

                mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                                .andExpect(status().isNoContent());
                mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                                .andExpect(status().isUnauthorized());
        }
}
