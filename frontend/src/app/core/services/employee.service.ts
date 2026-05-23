import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmployeeResponseDto {
  id: string;
  name: string;
  appUserId: string;
  teamId: string;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface EmployeeRequestDto {
  name: string;
  appUserId: string;
  teamId: string;
  status?: 'ACTIVE' | 'INACTIVE';
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/employees`;

  getAll(options?: { revealNames?: boolean }): Observable<EmployeeResponseDto[]> {
    const params = options?.revealNames ? new HttpParams().set('revealNames', 'true') : undefined;
    return this.http.get<EmployeeResponseDto[]>(this.api, { params });
  }

  getMe(): Observable<EmployeeResponseDto> {
    return this.http.get<EmployeeResponseDto>(`${this.api}/me`);
  }

  getById(id: string): Observable<EmployeeResponseDto> {
    return this.http.get<EmployeeResponseDto>(`${this.api}/${id}`);
  }

  create(body: EmployeeRequestDto): Observable<EmployeeResponseDto> {
    return this.http.post<EmployeeResponseDto>(this.api, body);
  }

  update(id: string, body: EmployeeRequestDto): Observable<EmployeeResponseDto> {
    return this.http.put<EmployeeResponseDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
