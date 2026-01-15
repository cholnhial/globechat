import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <div class="logo">
          <span class="logo-icon">🌐</span>
          <h1>GlobeChat</h1>
        </div>
        <h2>Create Account</h2>
        <p class="subtitle">Join the global conversation</p>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="username">Username</label>
            <input
              type="text"
              id="username"
              formControlName="username"
              placeholder="Choose a username"
              autocomplete="username"
            />
            @if (form.get('username')?.touched && form.get('username')?.invalid) {
              <span class="error">Username must be 3-50 characters</span>
            }
          </div>

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
              placeholder="Create a password"
              autocomplete="new-password"
            />
            @if (form.get('password')?.touched && form.get('password')?.invalid) {
              <span class="error">Password must be at least 8 characters</span>
            }
          </div>

          <button type="submit" [disabled]="form.invalid || loading" class="btn-primary">
            @if (loading) {
              <span class="spinner"></span>
              Creating account...
            } @else {
              Create Account
            }
          </button>
        </form>

        <p class="footer-text">
          Already have an account? <a routerLink="/login">Sign in</a>
        </p>
      </div>
    </div>
  `,
  styleUrl: '../login/auth.styles.scss'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = false;

  form: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.authService.register(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
