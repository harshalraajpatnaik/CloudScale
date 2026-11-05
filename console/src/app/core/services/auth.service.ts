import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, Organisation, RegisterRequest, User } from '../models/auth.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: any };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly ACCESS_TOKEN_KEY = 'cloudscale_access_token';
  private readonly REFRESH_TOKEN_KEY = 'cloudscale_refresh_token';
  private readonly USER_KEY = 'cloudscale_user';
  private readonly ORG_KEY = 'cloudscale_org';

  currentUser = signal<User | null>(this.loadStoredUser());
  currentOrg = signal<Organisation | null>(this.loadStoredOrg());
  isAuthenticated = computed(() => !!this.currentUser() && !!this.getAccessToken());
  userRole = computed(() => this.currentUser()?.role || null);

  register(req: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/register', req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.handleAuthSuccess(res.data);
        }
      })
    );
  }

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.handleAuthSuccess(res.data);
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/refresh', { refreshToken }).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.handleAuthSuccess(res.data);
        }
      })
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post('/api/v1/auth/logout', { refreshToken }).subscribe({
        error: () => {} // Ignore failure on logout
      });
    }

    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.ORG_KEY);

    this.currentUser.set(null);
    this.currentOrg.set(null);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private handleAuthSuccess(auth: AuthResponse): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, auth.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, auth.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(auth.user));
    localStorage.setItem(this.ORG_KEY, JSON.stringify(auth.organisation));

    this.currentUser.set(auth.user);
    this.currentOrg.set(auth.organisation);
  }

  private loadStoredUser(): User | null {
    try {
      const data = localStorage.getItem(this.USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  private loadStoredOrg(): Organisation | null {
    try {
      const data = localStorage.getItem(this.ORG_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }
}
