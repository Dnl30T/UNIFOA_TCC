import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FormResponseResponseDto {
  id: string;
  formId: string;
  questionId: string;
  employeeId: string;
  value: number;
  responseTimestamp: string;
}

export interface FormResponseRequestDto {
  formId: string;
  questionId: string;
  employeeId: string;
  value: number;
  responseTimestamp?: string;
}

@Injectable({ providedIn: 'root' })
export class FormResponseService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/form-responses`;

  getAll(params?: { employeeId?: string; questionId?: string; formId?: string }): Observable<FormResponseResponseDto[]> {
    let httpParams = new HttpParams();
    if (params?.employeeId) httpParams = httpParams.set('employeeId', params.employeeId);
    if (params?.questionId) httpParams = httpParams.set('questionId', params.questionId);
    if (params?.formId) httpParams = httpParams.set('formId', params.formId);
    return this.http.get<FormResponseResponseDto[]>(this.api, { params: httpParams });
  }

  getById(id: string): Observable<FormResponseResponseDto> {
    return this.http.get<FormResponseResponseDto>(`${this.api}/${id}`);
  }

  create(body: FormResponseRequestDto): Observable<FormResponseResponseDto> {
    return this.http.post<FormResponseResponseDto>(this.api, body);
  }

  update(id: string, body: FormResponseRequestDto): Observable<FormResponseResponseDto> {
    return this.http.put<FormResponseResponseDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
