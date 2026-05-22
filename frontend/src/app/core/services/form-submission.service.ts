import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FormSubmissionResponseDto {
  formId: string;
  employeeId: string;
  answerCount: number;
  submittedAt: string;
  responseStatus: 'RESPONDED' | 'NO_RESPONSE';
  score?: number;
  burnoutRiskPreAnalysis?: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface FormSubmissionRequestDto {
  formId: string;
  employeeId: string;
  answers: { questionId: string; value: number; textValue?: string }[];
}

@Injectable({ providedIn: 'root' })
export class FormSubmissionService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/form-submissions`;

  getAll(params?: { employeeId?: string; formId?: string }): Observable<FormSubmissionResponseDto[]> {
    let httpParams = new HttpParams();
    if (params?.employeeId) httpParams = httpParams.set('employeeId', params.employeeId);
    if (params?.formId) httpParams = httpParams.set('formId', params.formId);
    return this.http.get<FormSubmissionResponseDto[]>(this.api, { params: httpParams });
  }

  create(body: FormSubmissionRequestDto): Observable<FormSubmissionResponseDto> {
    return this.http.post<FormSubmissionResponseDto>(this.api, body);
  }
}
