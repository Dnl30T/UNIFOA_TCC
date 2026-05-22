import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export interface Step {
  id: string;
  label: string;
  status: 'complete' | 'active' | 'pending';
  description?: string;
}

@Component({
  selector: 'app-step-indicator',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="step-indicator">
      <div class="step-indicator__steps">
        <div
          *ngFor="let step of steps; let i = index; let last = last"
          class="step-indicator__step"
          [class.step-indicator__step--complete]="step.status === 'complete'"
          [class.step-indicator__step--active]="step.status === 'active'"
          [class.step-indicator__step--pending]="step.status === 'pending'"
        >
          <!-- Connector line -->
          <div
            *ngIf="!last"
            class="step-indicator__connector"
            [class.step-indicator__connector--complete]="step.status === 'complete'"
          ></div>

          <!-- Step circle -->
          <div class="step-indicator__circle">
            <mat-icon *ngIf="step.status === 'complete'" class="step-indicator__icon">
              check_circle
            </mat-icon>
            <span *ngIf="step.status !== 'complete'" class="step-indicator__number">
              {{ i + 1 }}
            </span>
          </div>

          <!-- Step label and description -->
          <div class="step-indicator__content">
            <div class="step-indicator__label">{{ step.label }}</div>
            <div *ngIf="step.description" class="step-indicator__description">
              {{ step.description }}
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .step-indicator {
      width: 100%;
    }

    .step-indicator__steps {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .step-indicator__step {
      display: flex;
      gap: 16px;
      position: relative;
      align-items: flex-start;
    }

    .step-indicator__connector {
      position: absolute;
      left: 20px;
      top: 56px;
      width: 2px;
      height: 24px;
      background-color: #d1d5db;
      transition: background-color 0.3s ease;
    }

    .step-indicator__connector--complete {
      background-color: #15803d;
    }

    .step-indicator__circle {
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 40px;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background-color: #e5e7eb;
      color: #6b7280;
      font-weight: 600;
      position: relative;
      z-index: 1;
      transition: all 0.3s ease;
      flex-shrink: 0;
    }

    .step-indicator__step--complete .step-indicator__circle {
      background-color: #d1fae5;
      color: #15803d;
    }

    .step-indicator__step--active .step-indicator__circle {
      background-color: #d1fae5;
      color: #15803d;
      box-shadow: 0 0 0 4px rgba(21, 128, 61, 0.1);
    }

    .step-indicator__step--pending .step-indicator__circle {
      background-color: #f3f4f6;
      color: #9ca3af;
    }

    .step-indicator__icon {
      width: 24px;
      height: 24px;
      font-size: 24px;
      line-height: 24px;
    }

    .step-indicator__number {
      font-size: 14px;
      font-weight: 600;
    }

    .step-indicator__content {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-top: 4px;
    }

    .step-indicator__label {
      font-size: 15px;
      font-weight: 500;
      color: #111827;
    }

    .step-indicator__step--pending .step-indicator__label {
      color: #9ca3af;
    }

    .step-indicator__description {
      font-size: 13px;
      color: #6b7280;
      line-height: 1.4;
    }

    @media (max-width: 640px) {
      .step-indicator__steps {
        gap: 20px;
      }

      .step-indicator__circle {
        min-width: 36px;
        width: 36px;
        height: 36px;
      }

      .step-indicator__label {
        font-size: 14px;
      }
    }
  `]
})
export class StepIndicatorComponent {
  @Input() steps: Step[] = [];
}
