import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslationService, LanguageOption } from '../../core/services/translation.service';
import { AuthService, AuthError } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  hidePassword = true;
  languages: LanguageOption[] = TranslationService.AVAILABLE_LANGUAGES;

  /** Mensaje de error recibido del backend para mostrar en la interfaz. */
  errorMessage: string | null = null;

  /** Indica si hay una petición en curso (para deshabilitar botones). */
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    public translationService: TranslationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.initForm();
    if (this.route.snapshot.queryParamMap.has('ssoError')) {
      this.errorMessage = 'No se pudo completar el inicio de sesión SSO. Inténtalo de nuevo.';
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { ssoError: null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }
  }

  private initForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  /**
   * Envía las credenciales al backend mediante el AuthService.
   * En caso de éxito navega a /main; en caso de error muestra el mensaje.
   */
  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;

      const { email, password } = this.loginForm.value;
      this.authService.login(email, password).subscribe({
        next: (response) => {
          console.log('Login exitoso:', response);
          this.isLoading = false;
          this.router.navigate(['/main']);
        },
        error: (err: AuthError) => {
          console.error('Error de login:', err);
          this.errorMessage = err.message || 'Error de autenticación';
          this.isLoading = false;
        }
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }

  /**
   * Inicia el flujo SSO: solicita la URL de redirección al backend
   * y redirige al usuario al proveedor SSO simulado.
   */
  onSsoLogin(): void {
    this.errorMessage = null;
    this.isLoading = true;
    this.authService.startSsoLogin();
  }

  changeLanguage(langCode: string): void {
    this.translationService.setLanguage(langCode);
  }
}
