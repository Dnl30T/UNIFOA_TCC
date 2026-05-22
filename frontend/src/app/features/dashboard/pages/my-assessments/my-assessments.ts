import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';

interface Assessment {
  id: string;
  title: string;
  status: 'pending' | 'completed';
}

@Component({
  selector: 'app-my-assessments',
  imports: [RouterLink, MatCardModule, MatTableModule, MatButtonModule, MatIconModule],
  templateUrl: './my-assessments.html',
  styleUrl: './my-assessments.scss',
})
export class MyAssessments implements OnInit {
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private submissionService = inject(FormSubmissionService);

  columns = ['title', 'status', 'action'];
  items = signal<Assessment[]>([]);

  ngOnInit(): void {
    this.employeeService.getMe().pipe(catchError(() => of(null))).subscribe(employee => {
      forkJoin({
        forms: this.formService.getAll('ACTIVE'),
        submissions: employee
          ? this.submissionService.getAll({ employeeId: employee.id }).pipe(catchError(() => of([])))
          : of([]),
      }).subscribe({
        next: ({ forms, submissions }) => {
          const submittedFormIds = new Set(submissions.map(s => s.formId));
          this.items.set((forms as FormResponseDto[]).map(f => ({
            id: f.id,
            title: f.title,
            status: submittedFormIds.has(f.id) ? 'completed' : 'pending',
          })));
        },
      });
    });
  }

  statusLabel(s: string) {
    return ({ pending: 'Pendente', completed: 'Respondido' } as Record<string, string>)[s] ?? s;
  }

  actionLabel(s: string) {
    return s === 'completed' ? 'Ver' : 'Responder';
  }
}
