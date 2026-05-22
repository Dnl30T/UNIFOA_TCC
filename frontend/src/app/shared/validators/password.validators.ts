import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const STRONG_PASSWORD_RE =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;

export function strongPassword(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value ?? '';
    if (!value) return null; // let required handle empty
    if (STRONG_PASSWORD_RE.test(value)) return null;
    return {
      strongPassword: {
        hasMinLength: value.length >= 8,
        hasUppercase: /[A-Z]/.test(value),
        hasLowercase: /[a-z]/.test(value),
        hasDigit: /\d/.test(value),
        hasSpecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value),
      },
    };
  };
}

export function passwordStrength(password: string): 0 | 1 | 2 | 3 | 4 {
  if (!password) return 0;
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) score++;
  return score as 0 | 1 | 2 | 3 | 4;
}

export function passwordsMatch(passwordKey: string, confirmKey: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const pass = group.get(passwordKey)?.value;
    const confirm = group.get(confirmKey)?.value;
    if (!confirm) return null; // let required handle empty
    return pass === confirm ? null : { passwordsMismatch: true };
  };
}
