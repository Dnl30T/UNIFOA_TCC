import { Component, signal, inject } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);

  hidePassword = signal(true);
  loading = signal(false);
  errorMsg = signal<string | null>(null);

  form = this.fb.group({
    email: ['', [Validators.required]],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.errorMsg.set(null);
    const { email, password } = this.form.value;
    this.auth.login(email!, password!).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.errorMsg.set('Credenciais inválidas. Tente novamente.');
        this.loading.set(false);
      },
    });
  }
}
