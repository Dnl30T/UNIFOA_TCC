import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface ActionCardButton {
  id: string;
  label: string;
  icon?: string;
  type: 'primary' | 'secondary' | 'tertiary';
  disabled?: boolean;
  tooltip?: string;
}

@Component({
  selector: 'app-action-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="action-card">
      <div class="action-card__primary">
        <button
          mat-raised-button
          color="primary"
          *ngIf="primaryAction"
          (click)="onActionClick(primaryAction)"
          [disabled]="primaryAction.disabled"
          [matTooltip]="primaryAction.tooltip || ''"
        >
          <mat-icon *ngIf="primaryAction.icon" class="action-card__icon">
            {{ primaryAction.icon }}
          </mat-icon>
          {{ primaryAction.label }}
        </button>
      </div>

      <div class="action-card__secondary" *ngIf="secondaryActions.length">
        <button
          mat-stroked-button
          *ngFor="let action of secondaryActions"
          (click)="onActionClick(action)"
          [disabled]="action.disabled"
          [matTooltip]="action.tooltip || ''"
          class="action-card__secondary-btn"
        >
          <mat-icon *ngIf="action.icon" class="action-card__icon">
            {{ action.icon }}
          </mat-icon>
          {{ action.label }}
        </button>
      </div>

      <div class="action-card__tertiary" *ngIf="tertiaryActions.length">
        <button
          mat-button
          *ngFor="let action of tertiaryActions"
          (click)="onActionClick(action)"
          [disabled]="action.disabled"
          [matTooltip]="action.tooltip || ''"
          class="action-card__tertiary-btn"
        >
          <mat-icon *ngIf="action.icon" class="action-card__icon">
            {{ action.icon }}
          </mat-icon>
          {{ action.label }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .action-card {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      align-items: center;

      @media (max-width: 640px) {
        flex-direction: column;
        align-items: stretch;
      }
    }

    .action-card__primary {
      display: flex;
      gap: 8px;

      @media (max-width: 640px) {
        flex: 1;

        button {
          width: 100%;
        }
      }
    }

    .action-card__secondary {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      @media (max-width: 640px) {
        flex-direction: column;
        width: 100%;
      }
    }

    .action-card__secondary-btn {
      @media (max-width: 640px) {
        width: 100%;
      }
    }

    .action-card__tertiary {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      @media (max-width: 640px) {
        flex-direction: column;
        width: 100%;
      }
    }

    .action-card__tertiary-btn {
      @media (max-width: 640px) {
        width: 100%;
        text-align: left;
      }
    }

    .action-card__icon {
      margin-right: 8px;
    }
  `]
})
export class ActionCardComponent {
  @Input() actions: ActionCardButton[] = [];
  @Output() actionTriggered = new EventEmitter<string>();

  get primaryAction(): ActionCardButton | undefined {
    return this.actions.find(a => a.type === 'primary');
  }

  get secondaryActions(): ActionCardButton[] {
    return this.actions.filter(a => a.type === 'secondary');
  }

  get tertiaryActions(): ActionCardButton[] {
    return this.actions.filter(a => a.type === 'tertiary');
  }

  onActionClick(action: ActionCardButton): void {
    if (!action.disabled) {
      this.actionTriggered.emit(action.id);
    }
  }
}
