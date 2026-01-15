import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { User, AuthResponse, LoginRequest, RegisterRequest } from '../models';

const TOKEN_KEY = 'globechat_token';
const USER_KEY = 'globechat_user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUser = signal<User | null>(null);
  private token = signal<string | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this.token());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadStoredAuth();
  }

  private loadStoredAuth(): void {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    const storedUser = localStorage.getItem(USER_KEY);

    if (storedToken && storedUser) {
      this.token.set(storedToken);
      this.currentUser.set(JSON.parse(storedUser));
    }
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap((response) => this.handleAuthSuccess(response)),
      catchError((error) => throwError(() => error))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((response) => this.handleAuthSuccess(response)),
      catchError((error) => throwError(() => error))
    );
  }

  logout(): void {
    this.token.set(null);
    this.currentUser.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.token();
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this.token.set(response.token);
    this.currentUser.set(response.user);
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
  }
}
