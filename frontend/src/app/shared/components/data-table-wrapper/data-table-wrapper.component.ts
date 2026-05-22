import { Component, Input, Output, EventEmitter, ViewChild, AfterViewInit, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SelectionModel } from '@angular/cdk/collections';

export interface TableColumn {
  key: string;
  label: string;
  type?: 'text' | 'checkbox' | 'actions' | 'custom';
  sortable?: boolean;
  width?: string;
}

export interface TableAction {
  id: string;
  label: string;
  icon?: string;
}

@Component({
  selector: 'app-data-table-wrapper',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="data-table-wrapper">
      <!-- Loading state -->
      <div *ngIf="isLoading" class="data-table-wrapper__loading">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Loading data...</p>
      </div>

      <!-- Empty state -->
      <div *ngIf="!isLoading && dataSource.data.length === 0" class="data-table-wrapper__empty">
        <mat-icon>inbox</mat-icon>
        <p>{{ emptyMessage }}</p>
      </div>

      <!-- Table -->
      <div *ngIf="!isLoading && dataSource.data.length > 0" class="data-table-wrapper__table-container">
        <table mat-table [dataSource]="dataSource" matSort (matSortChange)="onSortChange($event)" class="data-table-wrapper__table">
          <!-- Checkbox column -->
          <ng-container matColumnDef="select" *ngIf="selectable">
            <th mat-header-cell *matHeaderCellDef class="data-table-wrapper__checkbox-cell">
              <mat-checkbox
                [checked]="selection.hasValue() && isAllSelected()"
                [indeterminate]="selection.hasValue() && !isAllSelected()"
                (change)="$event ? toggleAllRows() : null"
                aria-label="Select all rows"
              ></mat-checkbox>
            </th>
            <td mat-cell *matCellDef="let row" class="data-table-wrapper__checkbox-cell">
              <mat-checkbox
                [checked]="selection.isSelected(row)"
                (change)="$event ? selection.toggle(row) : null"
                [attr.aria-label]="'Select row ' + row.id"
              ></mat-checkbox>
            </td>
          </ng-container>

          <!-- Dynamic columns -->
          <ng-container *ngFor="let column of displayedColumns" [matColumnDef]="column.key">
            <th
              mat-header-cell
              *matHeaderCellDef
              [ngClass]="{ 'data-table-wrapper__sortable': column.sortable }"
            >
              <div *ngIf="column.sortable" mat-sort-header>
                {{ column.label }}
              </div>
              <div *ngIf="!column.sortable">
                {{ column.label }}
              </div>
            </th>
            <td mat-cell *matCellDef="let row" [style.width]="column.width">
              <ng-container *ngIf="column.type !== 'actions'">
                {{ row[column.key] }}
              </ng-container>
            </td>
          </ng-container>

          <!-- Actions column -->
          <ng-container matColumnDef="actions" *ngIf="hasActions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let row" class="data-table-wrapper__actions-cell">
              <button
                *ngFor="let action of actions"
                mat-icon-button
                (click)="onActionClick(action.id, row)"
                [title]="action.label"
              >
                <mat-icon>{{ action.icon || 'more_vert' }}</mat-icon>
              </button>
            </td>
          </ng-container>

          <!-- Table header -->
          <tr mat-header-row *matHeaderRowDef="getAllColumns(); sticky: true"></tr>

          <!-- Table rows -->
          <tr
            mat-row
            *matRowDef="let row; columns: getAllColumns()"
            class="data-table-wrapper__row"
            [class.data-table-wrapper__row--selected]="selection.isSelected(row)"
          ></tr>
        </table>

        <!-- Paginator -->
        <mat-paginator
          [length]="totalItems"
          [pageSize]="pageSize"
          [pageSizeOptions]="pageSizeOptions"
          (page)="onPageChange($event)"
          showFirstLastButtons
        ></mat-paginator>
      </div>

      <!-- Selection summary -->
      <div *ngIf="selection.hasValue()" class="data-table-wrapper__selection-summary">
        {{ selection.selected.length }} row(s) selected
        <button mat-button (click)="clearSelection()">
          <mat-icon>clear</mat-icon>
          Clear
        </button>
      </div>
    </div>
  `,
  styles: [`
    .data-table-wrapper {
      width: 100%;
    }

    .data-table-wrapper__loading,
    .data-table-wrapper__empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 48px 24px;
      color: #6b7280;
      text-align: center;
    }

    .data-table-wrapper__empty mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #d1d5db;
    }

    .data-table-wrapper__table-container {
      overflow-x: auto;
      border-radius: 8px;
      border: 1px solid #e5e7eb;
    }

    .data-table-wrapper__table {
      width: 100%;
      border-collapse: collapse;
    }

    .data-table-wrapper__table thead {
      background-color: #f9fafb;
    }

    .data-table-wrapper__table th {
      font-weight: 600;
      color: #111827;
      padding: 12px 16px;
      text-align: left;
      font-size: 13px;
      border-bottom: 1px solid #e5e7eb;
    }

    .data-table-wrapper__sortable {
      cursor: pointer;
      user-select: none;

      &:hover {
        background-color: #f3f4f6;
      }
    }

    .data-table-wrapper__table td {
      padding: 12px 16px;
      border-bottom: 1px solid #e5e7eb;
      font-size: 14px;
      color: #111827;
    }

    .data-table-wrapper__row {
      &:hover {
        background-color: #f9fafb;
      }
    }

    .data-table-wrapper__row--selected {
      background-color: #f0fdf4;
    }

    .data-table-wrapper__checkbox-cell {
      width: 48px;
      text-align: center;
    }

    .data-table-wrapper__actions-cell {
      text-align: right;
    }

    .data-table-wrapper__selection-summary {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      background-color: #d1fae5;
      color: #15803d;
      font-size: 14px;
      border-radius: 0 0 8px 8px;
    }

    @media (max-width: 768px) {
      .data-table-wrapper__table {
        font-size: 12px;
      }

      .data-table-wrapper__table th,
      .data-table-wrapper__table td {
        padding: 8px 12px;
        font-size: 12px;
      }
    }
  `]
})
export class DataTableWrapperComponent<T extends { id: string | number }> {
  @Input() columns: TableColumn[] = [];
  @Input() data: T[] = [];
  @Input() isLoading = false;
  @Input() totalItems = 0;
  @Input() pageSize = 10;
  @Input() pageSizeOptions = [5, 10, 25, 50];
  @Input() selectable = false;
  @Input() actions: TableAction[] = [];
  @Input() emptyMessage = 'No data available';

  @Output() pageChange = new EventEmitter<PageEvent>();
  @Output() sortChange = new EventEmitter<Sort>();
  @Output() selectionChange = new EventEmitter<T[]>();
  @Output() actionClick = new EventEmitter<{ actionId: string; row: T }>();

  dataSource = new MatTableDataSource<T>();
  selection = new SelectionModel<T>(true, []);

  ngOnInit(): void {
    this.dataSource.data = this.data;
  }

  ngOnChanges(): void {
    this.dataSource.data = this.data;
  }

  get displayedColumns(): TableColumn[] {
    return this.columns.filter(col => col.type !== 'checkbox');
  }

  get hasActions(): boolean {
    return this.actions.length > 0;
  }

  getAllColumns(): string[] {
    const cols: string[] = [];
    if (this.selectable) cols.push('select');
    cols.push(...this.displayedColumns.map(c => c.key));
    if (this.hasActions) cols.push('actions');
    return cols;
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.selection.select(...this.dataSource.data);
    }
    this.emitSelection();
  }

  private emitSelection(): void {
    this.selectionChange.emit(this.selection.selected);
  }

  clearSelection(): void {
    this.selection.clear();
    this.emitSelection();
  }

  onPageChange(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  onSortChange(sort: Sort): void {
    this.sortChange.emit(sort);
  }

  onActionClick(actionId: string, row: T): void {
    this.actionClick.emit({ actionId, row });
  }
}
