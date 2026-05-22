import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TherapistEvaluationDto {
  formId: string;
  employeeId: string;
  occupationalContext?: string;
  teamDynamics?: string;
  psychosomaticSymptoms?: string;
  cognitiveEmotionalChanges?: string;
  exhaustionScore?: number;
  exhaustionJustification?: string;
  depersonalizationScore?: number;
  depersonalizationJustification?: string;
  differentialDiagnosis?: string;
  interventionPlan?: string;
  internalNote?: string;
  hrSummary?: string;
  closingCommentary?: string;
  stressScore?: number;
  sleepScore?: number;
  overloadScore?: number;
  fatigueScore?: number;
  disengagementScore?: number;
  isolationScore?: number;
  status?: 'DRAFT' | 'PUBLISHED';
  publishedAt?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type TherapistEvaluationRequestDto = Omit<TherapistEvaluationDto,
  'formId' | 'employeeId' | 'createdBy' | 'createdAt' | 'updatedAt'>;

@Injectable({ providedIn: 'root' })
export class TherapistEvaluationService {
  private http = inject(HttpClient);

  private url(formId: string, employeeId: string) {
    return `${environment.apiUrl}/therapist-evaluations/${formId}/${employeeId}`;
  }

  get(formId: string, employeeId: string): Observable<TherapistEvaluationDto> {
    return this.http.get<TherapistEvaluationDto>(this.url(formId, employeeId));
  }

  upsert(formId: string, employeeId: string, body: TherapistEvaluationRequestDto): Observable<TherapistEvaluationDto> {
    return this.http.put<TherapistEvaluationDto>(this.url(formId, employeeId), body);
  }

  publish(formId: string, employeeId: string): Observable<TherapistEvaluationDto> {
    return this.http.post<TherapistEvaluationDto>(`${this.url(formId, employeeId)}/publish`, {});
  }

  publishBatch(formId: string, employeeIds: string[]): Observable<TherapistEvaluationDto[]> {
    return this.http.post<TherapistEvaluationDto[]>(
      `${environment.apiUrl}/therapist-evaluations/${formId}/publish-batch`,
      { employeeIds },
    );
  }

  employeeView(formId: string): Observable<TherapistEvaluationDto> {
    return this.http.get<TherapistEvaluationDto>(`${environment.apiUrl}/therapist-evaluations/${formId}/employee-view`);
  }

  getAllByForm(formId: string): Observable<TherapistEvaluationDto[]> {
    return this.http.get<TherapistEvaluationDto[]>(`${environment.apiUrl}/therapist-evaluations/form/${formId}`);
  }

  getPublishedByForm(formId: string): Observable<TherapistEvaluationDto[]> {
    return this.http.get<TherapistEvaluationDto[]>(`${environment.apiUrl}/therapist-evaluations/form/${formId}/published`);
  }
}
