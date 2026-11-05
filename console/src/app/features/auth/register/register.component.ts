import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm = this.fb.group({
    organisationName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const val = this.registerForm.getRawValue();
    this.authService.register({
      organisationName: val.organisationName!,
      fullName: val.fullName!,
      email: val.email!,
      password: val.password!
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success) {
          this.router.navigate(['/nodes']);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.error?.message || 'Registration failed. Please check your details.');
      }
    });
  }
}
