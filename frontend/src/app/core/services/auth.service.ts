import { Injectable, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, switchMap, map } from 'rxjs';
import { environment } from '../../../environments/environment';

interface TokenPayload {
  sub: string;
  role: string;
  exp: number;
}

interface TokenResponse {
  token: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);

  private readonly TOKEN_KEY = 'psytrack_token';
  private readonly api = environment.apiUrl;

  private readonly _token = signal<string | null>(this.loadToken());
  readonly token = this._token.asReadonly();

  readonly username = computed(() => this.extractClaim(this._token(), 'sub'));
  readonly backendRole = computed(() => this.extractClaim(this._token(), 'role'));
  readonly isLoggedIn = computed(() => {
    const t = this._token();
    if (!t) return false;
    try {
      const payload = this.decodePayload(t);
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  });

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<TokenResponse>(`${this.api}/auth/login`, { email, password })
      .pipe(
        tap(r => this.storeToken(r.token)),
        map(() => void 0),
      );
  }

  registerEmployee(
    username: string,
    email: string,
    password: string,
    teamCode: string,
    name: string,
  ): Observable<void> {
    return this.http
      .post<TokenResponse>(`${this.api}/auth/register/employee`, {
        username,
        email,
        password,
        teamCode,
        name,
      })
      .pipe(
        tap(r => this.storeToken(r.token)),
        map(() => void 0),
      );
  }

  registerManager(username: string, email: string, password: string, name: string): Observable<void> {
    return this.http
      .post<TokenResponse>(`${this.api}/auth/register/staff`, { username, email, password, name })
      .pipe(
        tap(r => this.storeToken(r.token)),
        switchMap(() =>
          this.http.post<TokenResponse>(`${this.api}/auth/claim-role`, { role: 'MANAGER' }),
        ),
        tap(r => this.storeToken(r.token)),
        map(() => void 0),
      );
  }

  registerCounselor(username: string, email: string, password: string, name: string): Observable<void> {
    return this.http
      .post<TokenResponse>(`${this.api}/auth/register/staff`, { username, email, password, name })
      .pipe(
        tap(r => this.storeToken(r.token)),
        switchMap(() =>
          this.http.post<TokenResponse>(`${this.api}/auth/claim-role`, { role: 'COUNSELOR' }),
        ),
        tap(r => this.storeToken(r.token)),
        map(() => void 0),
      );
  }

  logout(): void {
    this._token.set(null);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.TOKEN_KEY);
    }
  }

  private storeToken(token: string): void {
    this._token.set(token);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  private loadToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(this.TOKEN_KEY);
    }
    return null;
  }

  private decodePayload(token: string): TokenPayload {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64)) as TokenPayload;
  }

  private extractClaim(token: string | null, claim: keyof TokenPayload): string | null {
    if (!token) return null;
    try {
      return String(this.decodePayload(token)[claim]);
    } catch {
      return null;
    }
  }
}
