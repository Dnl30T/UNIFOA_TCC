import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type QuestionType =
  | 'SCALE'
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'LIKERT'
  | 'RANKING'
  | 'MATRIX'
  | 'SLIDER'
  | 'TEXT'
  | 'NUMERIC'
  | 'BOOLEAN'
  | 'DATE'
  | 'MAP';

export interface QuestionResponseDto {
  id: string;
  formId: string;
  text: string;
  type: QuestionType;
  required: boolean;
  config: Record<string, string>;
  order: number;
}

export interface QuestionRequestDto {
  formId?: string;
  text: string;
  type: QuestionType;
  required?: boolean;
  config?: Record<string, string>;
  order?: number;
}

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/questions`;

  getAll(type?: QuestionType): Observable<QuestionResponseDto[]> {
    const params = type ? new HttpParams().set('type', type) : undefined;
    return this.http.get<QuestionResponseDto[]>(this.api, { params });
  }

  getById(id: string): Observable<QuestionResponseDto> {
    return this.http.get<QuestionResponseDto>(`${this.api}/${id}`);
  }

  create(body: QuestionRequestDto): Observable<QuestionResponseDto> {
    return this.http.post<QuestionResponseDto>(this.api, body);
  }

  update(id: string, body: QuestionRequestDto): Observable<QuestionResponseDto> {
    return this.http.put<QuestionResponseDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
