import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { UpperCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { FilterBarComponent } from '../../../../shared/components/filter-bar/filter-bar.component';


interface CounselorAssessment {
  id: string;
  title: string;
  responses: number;
  total: number;
  status: 'draft' | 'published' | 'closed';
}

@Component({
  selector: 'app-counselor-assessments',
  imports: [
    RouterLink,
    UpperCasePipe,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    FilterBarComponent,
  ],
  templateUrl: './counselor-assessments.html',
  styleUrl: './counselor-assessments.scss',
})
export class CounselorAssessments implements OnInit {
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private submissionService = inject(FormSubmissionService);
  private router = inject(Router);

  columns = ['title', 'responses', 'status', 'actions'];
  items = signal<CounselorAssessment[]>([]);
  activeFilters = signal<{ search?: string; status?: string }>({});

  filteredItems = () => {
    const filters = this.activeFilters();
    return this.items().filter(item => {
      const matchesSearch = !filters.search || 
        item.title.toLowerCase().includes(filters.search.toLowerCase());
      const matchesStatus = !filters.status || item.status === filters.status;
      return matchesSearch && matchesStatus;
    });
  };
  confirmDeleteId = signal<string | null>(null);
  deleting = signal(false);
  closingId = signal<string | null>(null);
  duplicatingId = signal<string | null>(null);
  duplicateNewTitle = signal('');

  ngOnInit(): void {
    forkJoin({
      forms: this.formService.getAll(),
      employees: this.employeeService.getAll().pipe(catchError(() => of([]))),
      submissions: this.submissionService.getAll().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ forms, employees, submissions }) => {
        const totalMembers = (employees as any[]).length;
        const countByForm = new Map<string, number>();
        for (const s of submissions) {
          countByForm.set(s.formId, (countByForm.get(s.formId) ?? 0) + 1);
        }
        this.items.set((forms as FormResponseDto[]).map(f => ({
          id: f.id,
          title: f.title,
          responses: countByForm.get(f.id) ?? 0,
          total: totalMembers,
          status: f.status === 'ACTIVE' ? 'published' : f.status === 'ENDED' ? 'closed' : 'draft',
        })));
      },
    });
  }

  closeAssessment(id: string): void {
    if (this.closingId()) return;
    const confirmed = window.confirm('Fechar esta avaliação? Funcionários sem resposta ficarão com status sem resposta.');
    if (!confirmed) return;

    this.closingId.set(id);
    this.formService.close(id).subscribe({
      next: () => {
        this.items.update(list => list.map(item => item.id === id ? { ...item, status: 'closed' } : item));
        this.closingId.set(null);
      },
      error: () => {
        this.closingId.set(null);
      },
    });
  }

  completionRate(item: CounselorAssessment): number {
    if (item.total === 0) return 0;
    return Math.round((item.responses / item.total) * 100);
  }

  requestDelete(id: string): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  onAssessmentAction(actionId: string, item: CounselorAssessment): void {
    switch (actionId) {
      case 'publish':
        this.router.navigate(['/dashboard/forms/publish', item.id]);
        break;
      case 'edit':
        this.router.navigate(['/dashboard/forms/update', item.id]);
        break;
      case 'view':
        this.router.navigate(['/dashboard/forms/view', item.id]);
        break;
      case 'duplicate':
        this.requestDuplicate(item.id, item.title);
        break;
      case 'close':
        this.closeAssessment(item.id);
        break;
      case 'delete':
        this.requestDelete(item.id);
        break;
    }
  }

  onFiltersChanged(filters: Record<string, string>): void {
    this.activeFilters.set({
      search: filters['search'] || undefined,
      status: filters['status'] || undefined,
    });
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (!id || this.deleting()) return;
    this.deleting.set(true);
    this.formService.delete(id).subscribe({
      next: () => {
        this.items.update(list => list.filter(i => i.id !== id));
        this.confirmDeleteId.set(null);
        this.deleting.set(false);
      },
      error: () => {
        this.deleting.set(false);
      },
    });
  }

  requestDuplicate(id: string, currentTitle: string): void {
    this.duplicatingId.set(id);
    this.duplicateNewTitle.set(`${currentTitle} (cópia)`);
  }

  cancelDuplicate(): void {
    this.duplicatingId.set(null);
    this.duplicateNewTitle.set('');
  }

  confirmDuplicate(): void {
    const id = this.duplicatingId();
    const title = this.duplicateNewTitle().trim();
    if (!id || !title) return;
    this.formService.duplicate(id, title).subscribe({
      next: newForm => {
        this.router.navigate(['/dashboard/forms/update', newForm.id]);
        this.duplicatingId.set(null);
        this.duplicateNewTitle.set('');
      },
      error: () => {
        this.duplicatingId.set(null);
      },
    });
  }
}
