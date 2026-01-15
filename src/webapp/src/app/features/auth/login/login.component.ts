import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <div class="logo">
          <span class="logo-icon">🌐</span>
          <h1>GlobeChat</h1>
        </div>
        <h2>Welcome Back</h2>
        <p class="subtitle">Sign in to continue to your chats</p>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              type="email"
              id="email"
              formControlName="email"
              placeholder="Enter your email"
              autocomplete="email"
            />
            @if (form.get('email')?.touched && form.get('email')?.invalid) {
              <span class="error">Please enter a valid email</span>
            }
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              type="password"
              id="password"
              formControlName="password"
              placeholder="Enter your password"
              autocomplete="current-password"
            />
            @if (form.get('password')?.touched && form.get('password')?.invalid) {
              <span class="error">Password is required</span>
            }
          </div>

          <button type="submit" [disabled]="form.invalid || loading" class="btn-primary">
            @if (loading) {
              <span class="spinner"></span>
              Signing in...
            } @else {
              Sign In
            }
          </button>
        </form>

        <p class="footer-text">
          Don't have an account? <a routerLink="/register">Sign up</a>
        </p>
      </div>
    </div>
  `,
  styleUrl: './auth.styles.scss'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = false;

  form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
