import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TeamResponseDto {
  id: string;
  name: string;
  teamCode?: string;
}

export interface TeamRequestDto {
  name: string;
}

@Injectable({ providedIn: 'root' })
export class TeamService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/teams`;

  getAll(): Observable<TeamResponseDto[]> {
    return this.http.get<TeamResponseDto[]>(this.api);
  }

  getMyTeam(): Observable<TeamResponseDto> {
    return this.http.get<TeamResponseDto>(`${this.api}/my`);
  }

  getByCode(code: string): Observable<TeamResponseDto> {
    const params = new HttpParams().set('code', code);
    return this.http.get<TeamResponseDto>(`${this.api}/by-code`, { params });
  }

  getById(id: string): Observable<TeamResponseDto> {
    return this.http.get<TeamResponseDto>(`${this.api}/${id}`);
  }

  create(body: TeamRequestDto): Observable<TeamResponseDto> {
    return this.http.post<TeamResponseDto>(this.api, body);
  }

  joinByCode(code: string): Observable<TeamResponseDto> {
    return this.http.patch<TeamResponseDto>(`${this.api}/join`, null, {
      params: new HttpParams().set('code', code),
    });
  }

  update(id: string, body: TeamRequestDto): Observable<TeamResponseDto> {
    return this.http.put<TeamResponseDto>(`${this.api}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
