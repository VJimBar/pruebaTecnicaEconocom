import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

export interface LanguageOption {
  code: string;
  label: string;
}

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  public static readonly AVAILABLE_LANGUAGES: LanguageOption[] = [
    { code: 'ES', label: 'Español' },
    { code: 'EN', label: 'English' },
    { code: 'FR', label: 'Français' },
    { code: 'PT', label: 'Português' }
  ];

  private currentLangSubject = new BehaviorSubject<string>('ES');
  public currentLang$: Observable<string> = this.currentLangSubject.asObservable();

  private translations: { [key: string]: any } = {};
  private translationsLoadedSubject = new BehaviorSubject<boolean>(false);
  public translationsLoaded$ = this.translationsLoadedSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadTranslations(this.currentLangSubject.value);
  }

  public get currentLanguage(): string {
    return this.currentLangSubject.value;
  }

  public setLanguage(langCode: string): void {
    const code = langCode.toUpperCase();
    if (this.currentLangSubject.value !== code) {
      this.currentLangSubject.next(code);
      this.loadTranslations(code);
    }
  }

  public loadTranslations(langCode: string): void {
    const lowerCode = langCode.toLowerCase();
    this.http.get<Record<string, any>>(`assets/i18n/${lowerCode}.json`).pipe(
      catchError((err: unknown) => {
        console.warn(`Could not load translations for ${lowerCode}, using fallback`, err);
        return of({});
      })
    ).subscribe((data: Record<string, any>) => {
      this.translations = data;
      this.translationsLoadedSubject.next(true);
    });
  }

  public translate(key: string): string {
    if (!key) return '';
    const keys = key.split('.');
    let value = this.translations;
    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        value = value[k];
      } else {
        return key; // return key as fallback if not resolved
      }
    }
    return typeof value === 'string' ? value : key;
  }
}
