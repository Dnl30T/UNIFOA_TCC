import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { QuestionResponseDto } from './question.service';

export interface FormResponseDto {
  id: string;
  title: string;
  description: string;
  status: 'CREATED' | 'ACTIVE' | 'ENDED';
  teamIds?: string[];
  questions?: QuestionResponseDto[];
}

export interface FormRequestDto {
  title: string;
  description?: string;
  status: 'CREATED' | 'ACTIVE' | 'ENDED';
  teamIds?: string[];
}

@Injectable({ providedIn: 'root' })
export class FormService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/forms`;

  getAll(status?: 'CREATED' | 'ACTIVE' | 'ENDED'): Observable<FormResponseDto[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<FormResponseDto[]>(this.api, { params });
  }

  getById(id: string): Observable<FormResponseDto> {
    return this.http.get<FormResponseDto>(`${this.api}/${id}`);
  }

  getByTitle(title: string): Observable<FormResponseDto> {
    const params = new HttpParams().set('title', title);
    return this.http.get<FormResponseDto>(`${this.api}/by-title`, { params });
  }

  create(body: FormRequestDto): Observable<FormResponseDto> {
    return this.http.post<FormResponseDto>(this.api, body);
  }

  update(id: string, body: FormRequestDto): Observable<FormResponseDto> {
    return this.http.put<FormResponseDto>(`${this.api}/${id}`, body);
  }

  close(id: string): Observable<FormResponseDto> {
    return this.http.put<FormResponseDto>(`${this.api}/${id}/close`, {});
  }

  publish(id: string): Observable<FormResponseDto> {
    return this.getById(id).pipe(
      switchMap(form => this.update(id, {
        title: form.title,
        description: form.description,
        status: 'ACTIVE',
        teamIds: form.teamIds ?? [],
      }))
    );
  }

  duplicate(id: string, newTitle: string): Observable<FormResponseDto> {
    return this.http.post<FormResponseDto>(`${this.api}/${id}/duplicate`, { newTitle });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
