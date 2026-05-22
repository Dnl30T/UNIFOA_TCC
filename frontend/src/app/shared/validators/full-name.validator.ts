import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Accepts two or more words made of letters (including accented), hyphens and apostrophes.
const FULL_NAME_RE = /^[A-Za-zÀ-ÖØ-öø-ÿ'\-]+( [A-Za-zÀ-ÖØ-öø-ÿ'\-]+)+$/;

export function fullName(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = (control.value ?? '').trim();
    if (!value) return null; // let required handle empty
    return FULL_NAME_RE.test(value) ? null : { fullName: true };
  };
}
