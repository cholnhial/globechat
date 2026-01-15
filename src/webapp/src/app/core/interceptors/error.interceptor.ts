import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';
      let errorTitle = 'Error';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = error.error.message;
        errorTitle = 'Client Error';
      } else {
        // Server-side error
        switch (error.status) {
          case 0:
            errorTitle = 'Connection Error';
            errorMessage = 'Unable to connect to the server. Please check your connection.';
            break;
          case 400:
            errorTitle = 'Bad Request';
            errorMessage = error.error?.message || 'Invalid request';
            break;
          case 401:
            errorTitle = 'Unauthorized';
            errorMessage = 'Your session has expired. Please log in again.';
            authService.logout();
            break;
          case 403:
            errorTitle = 'Forbidden';
            errorMessage = error.error?.message || 'You do not have permission to perform this action';
            break;
          case 404:
            errorTitle = 'Not Found';
            errorMessage = error.error?.message || 'The requested resource was not found';
            break;
          case 500:
            errorTitle = 'Server Error';
            errorMessage = 'An internal server error occurred. Please try again later.';
            break;
          default:
            errorMessage = error.error?.message || `Error: ${error.status}`;
        }
      }

      toastService.error(errorTitle, errorMessage);
      return throwError(() => error);
    })
  );
};
