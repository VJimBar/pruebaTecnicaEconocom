import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sso-callback',
  templateUrl: './sso-callback.component.html',
  styleUrls: ['./sso-callback.component.scss']
})
export class SsoCallbackComponent implements OnInit {
  message = 'Comprobando la respuesta del proveedor SSO...';
  private static readonly SUCCESS_DISPLAY_DURATION_MS = 3000;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const providerError = params.get('error');
    const code = params.get('code');
    const state = params.get('state');

    if (providerError) {
      this.returnToLoginWithError();
      return;
    }
    if (!code || !state) {
      this.returnToLoginWithError();
      return;
    }

    this.authService.ssoCallback(code, state).subscribe({
      next: () => {
        this.message = 'Autenticación completada. Redirigiendo...';
        window.setTimeout(
          () => this.router.navigateByUrl('/main'),
          SsoCallbackComponent.SUCCESS_DISPLAY_DURATION_MS
        );
      },
      error: () => this.returnToLoginWithError()
    });
  }

  private returnToLoginWithError(): void {
    this.router.navigate(['/login'], { queryParams: { ssoError: 'true' } });
  }
}
