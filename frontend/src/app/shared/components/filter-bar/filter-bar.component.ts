import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { debounceTime, distinctUntilChanged } from 'rxjs';

export interface FilterOption {
  label: string;
  value: string;
}

export interface FilterControl {
  key: string;
  type: 'search' | 'select' | 'multi-select';
  label: string;
  placeholder?: string;
  options?: FilterOption[];
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule
  ],
  template: `
    <div class="filter-bar">
      <form [formGroup]="filterForm" class="filter-bar__form">
        <div class="filter-bar__controls">
          <mat-form-field *ngFor="let control of filterControls" class="filter-bar__field" appearance="outline">
            <mat-label>{{ control.label }}</mat-label>

            <!-- Search input -->
            <input
              *ngIf="control.type === 'search'"
              matInput
              [formControlName]="control.key"
              [placeholder]="control.placeholder || ''"
              type="text"
            />

            <!-- Single select -->
            <mat-select
              *ngIf="control.type === 'select'"
              [formControlName]="control.key"
              [compareWith]="compareOptions"
            >
              <mat-option value="">{{ 'All' }}</mat-option>
              <mat-option *ngFor="let opt of control.options" [value]="opt.value">
                {{ opt.label }}
              </mat-option>
            </mat-select>

            <!-- Multi-select -->
            <mat-select
              *ngIf="control.type === 'multi-select'"
              [formControlName]="control.key"
              multiple
            >
              <mat-option *ngFor="let opt of control.options" [value]="opt.value">
                {{ opt.label }}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </form>

      <!-- Active filters display -->
      <div class="filter-bar__active-filters" *ngIf="hasActiveFilters()">
        <mat-chip-set>
          <mat-chip
            *ngFor="let chip of getActiveFilterChips()"
            (removed)="removeFilter(chip.key)"
          >
            {{ chip.label }}
            <button matChipRemove>
              <mat-icon>close</mat-icon>
            </button>
          </mat-chip>
        </mat-chip-set>
        <button mat-button (click)="clearFilters()" class="filter-bar__clear">
          <mat-icon>clear_all</mat-icon>
          Clear All
        </button>
      </div>

      <!-- Results count -->
      <div class="filter-bar__results" *ngIf="resultsCount !== undefined">
        {{ resultsCount }} results
      </div>
    </div>
  `,
  styles: [`
    .filter-bar {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 16px;
      background-color: #f9fafb;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .filter-bar__form {
      display: flex;
    }

    .filter-bar__controls {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      flex: 1;
    }

    .filter-bar__field {
      min-width: 200px;
      flex: 1;

      @media (max-width: 768px) {
        min-width: 100%;
        flex: 1 1 100%;
      }
    }

    .filter-bar__active-filters {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .filter-bar__clear {
      margin-left: auto;

      @media (max-width: 640px) {
        margin-left: 0;
        width: 100%;
        text-align: center;
      }
    }

    .filter-bar__results {
      font-size: 13px;
      color: #6b7280;
      text-align: right;

      @media (max-width: 640px) {
        text-align: left;
      }
    }

    ::ng-deep .mat-form-field {
      width: 100%;
    }

    /* Override Angular Material green primary colors for filter fields */
    ::ng-deep .filter-bar__field {
      --mdc-outlined-text-field-focus-outline-color: #6b7280;
      --mdc-outlined-text-field-focus-label-text-color: #374151;
      --mat-select-focused-arrow-color: #6b7280;
      --mat-form-field-focus-select-arrow-color: #6b7280;
      --mdc-outlined-text-field-caret-color: #374151;

      .mat-mdc-select-arrow {
        color: #6b7280;
      }

      &.mat-focused .mdc-notched-outline__leading,
      &.mat-focused .mdc-notched-outline__notch,
      &.mat-focused .mdc-notched-outline__trailing {
        border-color: #6b7280 !important;
      }
    }
  `]
})
export class FilterBarComponent implements OnInit {
  @Input() filterControls: FilterControl[] = [];
  @Input() resultsCount?: number;
  @Output() filtersChanged = new EventEmitter<Record<string, any>>();

  filterForm = new FormGroup({});

  ngOnInit(): void {
    this.initializeForm();
    this.setupFilterListeners();
  }

  private initializeForm(): void {
    const controls: Record<string, FormControl> = {};
    this.filterControls.forEach(control => {
      controls[control.key] = new FormControl('');
    });
    this.filterForm = new FormGroup(controls);
  }

  private setupFilterListeners(): void {
    this.filterForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.emitFilterChanges();
      });
  }

  private emitFilterChanges(): void {
    const activeFilters = this.getActiveFilters();
    this.filtersChanged.emit(activeFilters);
  }

  private getActiveFilters(): Record<string, any> {
    const result: Record<string, any> = {};
    const formValue = this.filterForm.getRawValue() as Record<string, any>;
    Object.keys(formValue).forEach(key => {
      const value = formValue[key];
      if (value !== '' && value !== null && (Array.isArray(value) ? value.length > 0 : true)) {
        result[key] = value;
      }
    });
    return result;
  }

  hasActiveFilters(): boolean {
    return Object.keys(this.getActiveFilters()).length > 0;
  }

  getActiveFilterChips(): { key: string; label: string }[] {
    const chips: { key: string; label: string }[] = [];
    const activeFilters = this.getActiveFilters();

    Object.keys(activeFilters).forEach(key => {
      const control = this.filterControls.find(c => c.key === key);
      const value = activeFilters[key];

      if (control) {
        if (Array.isArray(value)) {
          value.forEach(v => {
            const label = control.options?.find(opt => opt.value === v)?.label || v;
            chips.push({ key, label });
          });
        } else {
          const label = control.options?.find(opt => opt.value === value)?.label || value;
          chips.push({ key, label });
        }
      }
    });

    return chips;
  }

  removeFilter(key: string): void {
    const control = this.filterForm.get(key);
    if (control) {
      control.setValue('');
    }
  }

  clearFilters(): void {
    this.filterForm.reset();
  }

  compareOptions(c1: string | null, c2: string | null): boolean {
    return c1 === c2;
  }
}
