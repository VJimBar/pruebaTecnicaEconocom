import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!request.url.includes('/api/') || this.isAuthenticationRequest(request.url)) {
      return next.handle(request);
    }

    return this.authService.ensureFreshAccessToken().pipe(
      switchMap(token => this.sendWithToken(request, token, next, true)),
      catchError(error => {
        this.authService.expireSession();
        return throwError(() => error);
      })
    );
  }

  private sendWithToken(
    request: HttpRequest<unknown>,
    token: string,
    next: HttpHandler,
    retryAfterUnauthorized: boolean
  ): Observable<HttpEvent<unknown>> {
    const authenticatedRequest = request.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });

    return next.handle(authenticatedRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401 || !retryAfterUnauthorized) {
          return throwError(() => error);
        }
        return this.authService.refresh().pipe(
          switchMap(response => this.sendWithToken(request, response.accessToken, next, false))
        );
      })
    );
  }

  private isAuthenticationRequest(url: string): boolean {
    return /\/api\/auth\/(login|refresh|logout|sso)(\/|$|\?)/.test(url);
  }
}
