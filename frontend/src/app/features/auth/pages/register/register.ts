import { Component, signal, inject, OnInit } from '@angular/core';
import { merge } from 'rxjs';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../../core/services/auth.service';
import {
  strongPassword,
  passwordStrength,
  passwordsMatch,
} from '../../../../shared/validators/password.validators';
import { fullName } from '../../../../shared/validators/full-name.validator';

@Component({
  selector: 'app-register',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatRadioModule,
    MatCheckboxModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);

  hidePassword = signal(true);
  hideConfirmPassword = signal(true);
  loading = signal(false);
  errorMsg = signal<string | null>(null);

  form = this.fb.group(
    {
      role: ['employee', Validators.required],
      fullName: ['', [Validators.required, fullName()]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, strongPassword()]],
      confirmPassword: ['', Validators.required],
      teamCode: [''],
      agreedToPolicy: [false, Validators.requiredTrue],
    },
    { validators: passwordsMatch('password', 'confirmPassword') },
  );

  passwordStrengthScore = signal<0 | 1 | 2 | 3 | 4>(0);
  passwordMatchStatus = signal<'idle' | 'match' | 'mismatch'>('idle');

  readonly strengthLabels = ['', 'Fraca', 'Razoável', 'Boa', 'Forte'] as const;
  readonly strengthClasses = ['', 'weak', 'fair', 'good', 'strong'] as const;

  ngOnInit(): void {
    this.form.get('password')!.valueChanges.subscribe((val) => {
      this.passwordStrengthScore.set(passwordStrength(val ?? ''));
    });

    merge(
      this.form.get('password')!.valueChanges,
      this.form.get('confirmPassword')!.valueChanges,
    ).subscribe(() => {
      const pass = this.form.get('password')!.value ?? '';
      const confirm = this.form.get('confirmPassword')!.value ?? '';
      if (!confirm) {
        this.passwordMatchStatus.set('idle');
      } else {
        this.passwordMatchStatus.set(pass === confirm ? 'match' : 'mismatch');
      }
    });
  }

  get isEmployee(): boolean {
    return this.form.get('role')?.value === 'employee';
  }

  onRoleChange(role: string): void {
    const teamCode = this.form.get('teamCode');
    if (role === 'employee') {
      teamCode?.setValidators(Validators.required);
    } else {
      teamCode?.clearValidators();
      teamCode?.setValue('');
    }
    teamCode?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.errorMsg.set(null);
    const { role, fullName: name, email, password, teamCode } = this.form.value;
    const username = email!.toLowerCase();

    const request$ =
      role === 'employee'
        ? this.auth.registerEmployee(username, email!, password!, teamCode!, name!)
        : role === 'counselor'
          ? this.auth.registerCounselor(username, email!, password!, name!)
          : this.auth.registerManager(username, email!, password!, name!);

    request$.subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        const msg = err?.error?.message ?? 'Falha no cadastro. Tente novamente.';
        this.errorMsg.set(msg);
        this.loading.set(false);
      },
    });
  }
}
