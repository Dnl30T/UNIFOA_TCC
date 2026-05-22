import { Component, OnInit, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../../core/services/auth.service';
import { environment } from '../../../../../environments/environment';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const next = control.get('next')?.value;
  const confirm = control.get('confirm')?.value;
  return next && confirm && next !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-settings',
  imports: [
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    MatDividerModule,
  ],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private readonly api = environment.apiUrl;

  // ── Profile ──────────────────────────────────────────────
  profileSuccess = signal(false);

  profileForm = this.fb.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    position: [''],
    company: [''],
  });

  profileLoading = signal(false);

  get avatarInitials(): string {
    const name = this.profileForm.get('fullName')?.value ?? '';
    return name.split(' ').map((p: string) => p[0]).slice(0, 2).join('').toUpperCase() || '?';
  }

  // ── Password ─────────────────────────────────────────────
  hideCurrentPw = signal(true);
  hideNewPw     = signal(true);
  hideConfirmPw = signal(true);
  passwordSuccess = signal(false);
  passwordError   = signal<string | null>(null);
  passwordLoading = signal(false);

  passwordForm = this.fb.group({
    current: ['', Validators.required],
    next:    ['', [Validators.required, Validators.minLength(8)]],
    confirm: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  // ── Privacy (persisted to backend) ──────────────────────
  privacy = { anonymize: false };
  privacySuccess = signal(false);
  privacyError   = signal<string | null>(null);
  privacyLoading = signal(false);

  // ── Lifecycle ────────────────────────────────────────────
  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.http.get<{ name: string | null; email: string; company: string | null; jobTitle: string | null }>(
        `${this.api}/users/me/profile`
      ).subscribe({
        next: (res) => {
          this.profileForm.patchValue({
            fullName: res.name ?? this.auth.username() ?? '',
            email: res.email,
            position: res.jobTitle ?? '',
            company: res.company ?? '',
          });
        },
        error: () => {
          const username = this.auth.username();
          if (username) this.profileForm.patchValue({ fullName: username });
        },
      });

      this.http.get<{ fullyAnonymized: boolean }>(`${this.api}/users/me/privacy`)
        .subscribe({
          next: (res) => { this.privacy.anonymize = res.fullyAnonymized; },
          error: () => { /* keep default false */ }
        });
    }
  }

  // ── Actions ──────────────────────────────────────────────
  onSaveProfile(): void {
    if (this.profileForm.invalid) return;
    this.profileLoading.set(true);
    this.http.put<{ name: string | null; email: string; company: string | null; jobTitle: string | null }>(
      `${this.api}/users/me/profile`,
      {
        name: this.profileForm.value.fullName,
        company: this.profileForm.value.company,
        jobTitle: this.profileForm.value.position,
      },
    ).subscribe({
      next: (res) => {
        this.profileForm.patchValue({
          fullName: res.name ?? '',
          email: res.email,
          position: res.jobTitle ?? '',
          company: res.company ?? '',
        });
        this.profileLoading.set(false);
        this.profileSuccess.set(true);
        setTimeout(() => this.profileSuccess.set(false), 3000);
      },
      error: () => {
        this.profileLoading.set(false);
      },
    });
  }

  onChangePassword(): void {
    this.passwordError.set(null);
    if (this.passwordForm.invalid) {
      if (this.passwordForm.hasError('passwordMismatch')) {
        this.passwordError.set('As novas senhas não coincidem.');
      }
      return;
    }
    this.passwordLoading.set(true);
    this.http.put(
      `${this.api}/users/me/password`,
      { currentPassword: this.passwordForm.value.current, newPassword: this.passwordForm.value.next },
      { observe: 'response' },
    ).subscribe({
      next: () => {
        this.passwordLoading.set(false);
        this.passwordSuccess.set(true);
        this.passwordForm.reset();
        setTimeout(() => this.passwordSuccess.set(false), 3000);
      },
      error: (err) => {
        this.passwordLoading.set(false);
        if (err.status === 400) {
          this.passwordError.set('Senha atual incorreta.');
        } else {
          this.passwordError.set('Falha ao alterar a senha. Tente novamente.');
        }
      },
    });
  }

  onSavePrivacy(): void {
    this.privacyError.set(null);
    this.privacyLoading.set(true);
    this.http.put<{ fullyAnonymized: boolean }>(
      `${this.api}/users/me/privacy`,
      { fullyAnonymized: this.privacy.anonymize },
    ).subscribe({
      next: (res) => {
        this.privacy.anonymize = res.fullyAnonymized;
        this.privacyLoading.set(false);
        this.privacySuccess.set(true);
        setTimeout(() => this.privacySuccess.set(false), 3000);
      },
      error: () => {
        this.privacyLoading.set(false);
        this.privacyError.set('Falha ao salvar preferências. Tente novamente.');
      },
    });
  }
}
