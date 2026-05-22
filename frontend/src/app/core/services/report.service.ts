import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReportResponseDto {
  id: string;
  formId: string;
  teamId: string;
  name: string;
  respondentIds: string[];
  respondentCount: number;
  averageScore: number;
  generalRisk: 'LOW' | 'MEDIUM' | 'HIGH';
  riskDistribution: Record<string, number>;
  generatedAt: string;
  createdBy: string;
}

export interface GenerateReportRequestDto {
  formId: string;
  teamId: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/reports`;

  generate(body: GenerateReportRequestDto): Observable<ReportResponseDto> {
    return this.http.post<ReportResponseDto>(this.api, body);
  }

  listByTeamId(teamId: string): Observable<ReportResponseDto[]> {
    const params = new HttpParams().set('teamId', teamId);
    return this.http.get<ReportResponseDto[]>(this.api, { params });
  }

  listByFormId(formId: string): Observable<ReportResponseDto[]> {
    const params = new HttpParams().set('formId', formId);
    return this.http.get<ReportResponseDto[]>(this.api, { params });
  }

  getById(teamId: string, reportId: string): Observable<ReportResponseDto> {
    return this.http.get<ReportResponseDto>(`${this.api}/${teamId}/${reportId}`);
  }

  /** For managers: list reports for own team (backend scopes automatically) */
  listForManager(): Observable<ReportResponseDto[]> {
    return this.http.get<ReportResponseDto[]>(this.api);
  }
}
