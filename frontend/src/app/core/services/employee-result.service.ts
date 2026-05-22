import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmployeeResultResponseDto {
  id: string;
  employeeId: string;
  formId: string;
  score: number;
  helperScore: number;
  finalScore?: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  calculatedAt: string;
}

export interface EmployeeResultRequestDto {
  employeeId: string;
  formId: string;
  score: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  calculatedAt?: string;
}

export interface EmployeeResultFinalizeRequestDto {
  employeeId: string;
  formId: string;
  finalScore: number;
}

@Injectable({ providedIn: 'root' })
export class EmployeeResultService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/employee-results`;

  getAll(params?: { employeeId?: string; formId?: string }): Observable<EmployeeResultResponseDto[]> {
    let httpParams = new HttpParams();
    if (params?.employeeId) httpParams = httpParams.set('employeeId', params.employeeId);
    if (params?.formId) httpParams = httpParams.set('formId', params.formId);
    return this.http.get<EmployeeResultResponseDto[]>(this.api, { params: httpParams });
  }

  getById(id: string): Observable<EmployeeResultResponseDto> {
    return this.http.get<EmployeeResultResponseDto>(`${this.api}/${id}`);
  }

  create(body: EmployeeResultRequestDto): Observable<EmployeeResultResponseDto> {
    return this.http.post<EmployeeResultResponseDto>(this.api, body);
  }

  update(id: string, body: EmployeeResultRequestDto): Observable<EmployeeResultResponseDto> {
    return this.http.put<EmployeeResultResponseDto>(`${this.api}/${id}`, body);
  }

  finalize(body: EmployeeResultFinalizeRequestDto): Observable<EmployeeResultResponseDto> {
    return this.http.put<EmployeeResultResponseDto>(`${this.api}/finalize`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
