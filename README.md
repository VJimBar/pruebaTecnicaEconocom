# Prueba técnica Econocom: Angular + Spring Boot

Aplicación de autenticación con Angular 16.2 y Spring Boot 2.7.18 / Java 8. Los usuarios se cargan en H2 en memoria mediante `DataInitializer`.

## Requisitos

- JDK 8
- Node.js y npm compatibles con Angular CLI 16

## Arranque local

En una terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`. El backend escucha en `http://localhost:8080`.

## Usuario de demostración

- Email: `prueba@econocom.com`
- Contraseña: `pass1234`

La contraseña se almacena con BCrypt. H2 se ejecuta en memoria; los usuarios y los refresh tokens activos se pierden al reiniciar el backend. La consola H2 está disponible en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:authdb`, usuario `sa`, contraseña vacía).

## JWT: caducidad, renovación y logout

- El access token dura  15 segundos y se envía como `Authorization: Bearer ...` a endpoints protegidos.
- El refresh token dura 7 días y solo se acepta en `/api/auth/refresh`. Al renovarlo, se revoca y se emite un nuevo par; no puede reutilizarse.
- Angular renueva el access token antes de que caduque, comparte las peticiones de refresco concurrentes y reintenta una petición protegida una vez ante un `401`.
- Si el refresh token caduca, es revocado o no es válido, el cliente borra la sesión y vuelve al login. Cerrar sesión también revoca el refresh token actual.
- Los tiempos pueden ajustarse en `backend/src/main/resources/application.yml` (`jwt.access-expiration-ms` y `jwt.refresh-expiration-ms`).

## SSO simulado

El botón SSO navega al endpoint `GET /api/auth/sso`. El backend responde con `302` hacia la ruta Angular `/sso/callback`, incluyendo un código de un solo uso y un `state` aleatorio. El callback valida ambos, consume el código (caduca a los 15 segundos) y emite los mismos JWT que el login normal. La identidad simulada corresponde al usuario de demostración; no se contacta con un proveedor externo.

La URL de retorno se configura mediante `auth.sso.redirect-uri` en `application.yml`.

## Pruebas

Desde `backend`:

```powershell
.\mvnw.cmd test
```
    ejecucion:
            .\mvnw.cmd test
            ↓
            Maven carga pom.xml
            ↓
            Compila backend
            ↓
            Compila tests
            ↓
            Maven Surefire
            ↓
            ────────────────────────────────
            AuthApplicationTests
            → Spring Boot inicia
            → JPA/Hibernate
            → H2
            → Security
            → 1 test
            → SUCCESS
            ────────────────────────────────
            ↓
            AuthControllerTest
            → Spring Boot inicia
            → JPA/Hibernate
            → H2
            → MockMvc / DispatcherServlet
            → Security
            → 8 tests
            → SUCCESS
            ────────────────────────────────
            ↓
            JwtServiceImplTest
            → 2 tests
            → SUCCESS
            ────────────────────────────────
            ↓
            TOTAL: 11 tests
            ↓
            11 SUCCESS
            ↓
            BUILD SUCCESS

Desde `frontend`:

```powershell
npm test -- --watch=false
npm run build
```
    ejecucion:
        ng test
        ↓
        Angular genera el bundle de tests
        ↓
        Karma arranca el servidor
        ↓
        Karma abre Chrome
        ↓
        Chrome ejecuta los tests
        ↓
        3 de 3 SUCCESS

