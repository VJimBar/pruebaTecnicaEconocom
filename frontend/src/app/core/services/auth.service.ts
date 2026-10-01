import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';

/**
 * Interfaz que modela la respuesta de autenticación del backend.
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/**
 * Interfaz que modela la respuesta de error del backend.
 */
export interface AuthError {
  status: number;
  message: string;
  timestamp: number;
}

/**
 * Servicio de autenticación que se comunica con el backend Spring Boot.
 * Gestiona login tradicional, SSO, almacenamiento de tokens y refresco.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  /** URL base de la API de autenticación */
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly accessExpiryKey = 'accessTokenExpiresAt';
  private readonly refreshBufferMs = 60_000;
  private refreshTimer: number | null = null;
  private refreshRequest: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient, private router: Router) {
    this.scheduleRefreshFromStorage();
  }

  /**
   * Envía las credenciales al endpoint /api/auth/login.
   * En caso de éxito, almacena los tokens en sessionStorage.
   *
   * @param email correo electrónico del usuario
   * @param password contraseña del usuario
   * @returns Observable con la respuesta de autenticación
   */
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(response => this.storeTokens(response)),
      catchError(error => throwError(() => this.toAuthError(error, 'Error de conexión con el servidor')))
    );
  }

  /**
   * Inicia el flujo SSO mediante navegación del navegador para que se procese
   * la respuesta HTTP 302 del backend.
   */
  startSsoLogin(): void {
    window.location.assign(`${this.apiUrl}/sso`);
  }

  /**
   * Completa el flujo SSO: envía el código de autorización al callback del backend.
   * En caso de éxito, almacena los tokens.
   *
   * @param code código de autorización recibido del proveedor SSO simulado
   * @returns Observable con la respuesta de autenticación
   */
  ssoCallback(code: string, state: string): Observable<AuthResponse> {
    return this.http.get<AuthResponse>(`${this.apiUrl}/sso/callback`, { params: { code, state } }).pipe(
      tap(response => this.storeTokens(response)),
      catchError(error => throwError(() => this.toAuthError(error, 'Error en callback SSO')))
    );
  }

  /**
   * Devuelve un access token vigente, renovándolo antes de su caducidad.
   */
  ensureFreshAccessToken(): Observable<string> {
    const accessToken = this.getAccessToken();
    if (accessToken && !this.isAccessTokenExpired()) {
      return of(accessToken);
    }
    return this.refresh().pipe(map(response => response.accessToken));
  }

  /**
   * Renueva y rota los tokens. Las llamadas simultáneas comparten el mismo
   * refresco para no intentar reutilizar varias veces el refresh token anterior.
   */
  refresh(): Observable<AuthResponse> {
    if (this.refreshRequest) {
      return this.refreshRequest;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => ({ status: 401, message: 'No hay refresh token', timestamp: Date.now() } as AuthError));
    }

    this.refreshRequest = this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap(response => this.storeTokens(response)),
      catchError(error => {
        this.clearTokens();
        return throwError(() => this.toAuthError(error, 'Error al refrescar token'));
      }),
      finalize(() => this.refreshRequest = null),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    return this.refreshRequest;
  }

  /**
   * Almacena los tokens en sessionStorage.
   */
  private storeTokens(response: AuthResponse): void {
    sessionStorage.setItem('accessToken', response.accessToken);
    sessionStorage.setItem('refreshToken', response.refreshToken);
    const expiresAt = Date.now() + response.expiresIn * 1000;
    sessionStorage.setItem(this.accessExpiryKey, String(expiresAt));
    this.scheduleRefresh(expiresAt);
  }

  /** Obtiene el access token almacenado. */
  getAccessToken(): string | null {
    return sessionStorage.getItem('accessToken');
  }

  /** Obtiene el refresh token almacenado. */
  getRefreshToken(): string | null {
    return sessionStorage.getItem('refreshToken');
  }

  /** Comprueba si el usuario tiene un token almacenado. */
  isAuthenticated(): boolean {
    return !!this.getAccessToken() && !this.isAccessTokenExpired();
  }

  /** Revoca el refresh token, elimina la sesión local y vuelve al login. */
  logout(): void {
    const refreshToken = this.getRefreshToken();
    this.clearTokens();
    this.router.navigateByUrl('/login');
    if (refreshToken) {
      this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).pipe(
        catchError(error => {
          console.error('No se pudo revocar el refresh token en el servidor', error);
          return of(undefined);
        })
      ).subscribe();
    }
  }

  /** Cierra la sesión local cuando el refresh token ya no es válido. */
  expireSession(): void {
    this.clearTokens();
    this.router.navigateByUrl('/login');
  }

  /** Limpia los tokens de sessionStorage. */
  private clearTokens(): void {
    if (this.refreshTimer !== null) {
      window.clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem(this.accessExpiryKey);
  }

  private isAccessTokenExpired(): boolean {
    const expiresAt = Number(sessionStorage.getItem(this.accessExpiryKey));
    return !Number.isFinite(expiresAt) || expiresAt <= Date.now();
  }

  private scheduleRefreshFromStorage(): void {
    if (!this.getRefreshToken()) {
      return;
    }
    const expiresAt = Number(sessionStorage.getItem(this.accessExpiryKey));
    if (!Number.isFinite(expiresAt)) {
      this.refresh().subscribe({
        error: () => this.expireSession()
      });
      return;
    }
    this.scheduleRefresh(expiresAt);
  }

  private scheduleRefresh(expiresAt: number): void {
    if (this.refreshTimer !== null) {
      window.clearTimeout(this.refreshTimer);
    }

    const remainingMs = expiresAt - Date.now();
    const refreshLeadMs = Math.min(this.refreshBufferMs, remainingMs * 0.1);
    const delayMs = Math.max(0, remainingMs - refreshLeadMs);
    this.refreshTimer = window.setTimeout(() => {
      this.refresh().subscribe({
        error: () => this.expireSession()
      });
    }, delayMs);
  }

  private toAuthError(error: unknown, fallbackMessage: string): AuthError {
    if (error instanceof HttpErrorResponse && error.error && typeof error.error.message === 'string') {
      return {
        status: error.status,
        message: error.error.message,
        timestamp: typeof error.error.timestamp === 'number' ? error.error.timestamp : Date.now()
      };
    }
    return {
      status: error instanceof HttpErrorResponse ? error.status : 500,
      message: fallbackMessage,
      timestamp: Date.now()
    };
  }
}
