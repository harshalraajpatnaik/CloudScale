import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  let authReq = req;
  if (token && !req.url.includes('/api/v1/auth/login') && !req.url.includes('/api/v1/auth/register')) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If 401 Unauthorized or 403 Forbidden on secured endpoint, attempt refresh or redirect to login
      if ((error.status === 401 || error.status === 403) && !req.url.includes('/api/v1/auth/')) {
        const refreshToken = authService.getRefreshToken();
        if (refreshToken) {
          return authService.refreshToken().pipe(
            switchMap(res => {
              if (res.success && res.data) {
                const retryReq = req.clone({
                  setHeaders: {
                    Authorization: `Bearer ${res.data.accessToken}`
                  }
                });
                return next(retryReq);
              }
              authService.logout();
              return throwError(() => error);
            }),
            catchError(refreshErr => {
              authService.logout();
              return throwError(() => refreshErr);
            })
          );
        } else {
          // No refresh token available - session is expired, redirect to login
          authService.logout();
          return throwError(() => error);
        }
      }
      return throwError(() => error);
    })
  );
};
