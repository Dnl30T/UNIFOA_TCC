import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TeamResultResponseDto {
  id: string;
  teamId: string;
  formId: string;
  averageScore: number;
  riskLevelDistribution: Record<string, number>;
  calculatedAt: string;
}

export interface TeamResultRequestDto {
  teamId: string;
  formId: string;
  averageScore: number;
  riskLevelDistribution?: Record<string, number>;
  calculatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class TeamResultService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/team-results`;

  getAll(params?: { teamId?: string; formId?: string }): Observable<TeamResultResponseDto[]> {
    let httpParams = new HttpParams();
    if (params?.teamId) httpParams = httpParams.set('teamId', params.teamId);
    if (params?.formId) httpParams = httpParams.set('formId', params.formId);
    return this.http.get<TeamResultResponseDto[]>(this.api, { params: httpParams });
  }

  getById(id: string): Observable<TeamResultResponseDto> {
    return this.http.get<TeamResultResponseDto>(`${this.api}/${id}`);
  }

  create(body: TeamResultRequestDto): Observable<TeamResultResponseDto> {
    return this.http.post<TeamResultResponseDto>(this.api, body);
  }

  update(id: string, body: TeamResultRequestDto): Observable<TeamResultResponseDto> {
    return this.http.put<TeamResultResponseDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
