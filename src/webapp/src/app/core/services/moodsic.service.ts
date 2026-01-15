import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Moodsic } from '../models';

@Injectable({
  providedIn: 'root',
})
export class MoodsicService {
  private availableMoodsics = signal<Moodsic[]>([]);
  private myMoodsics = signal<Moodsic[]>([]);

  readonly available = this.availableMoodsics.asReadonly();
  readonly mine = this.myMoodsics.asReadonly();

  constructor(private http: HttpClient) {}

  loadAvailable(): Observable<Moodsic[]> {
    return this.http
      .get<Moodsic[]>('/api/moodsics')
      .pipe(tap((moodsics) => this.availableMoodsics.set(moodsics)));
  }

  loadMine(): Observable<Moodsic[]> {
    return this.http
      .get<Moodsic[]>('/api/moodsics/my')
      .pipe(tap((moodsics) => this.myMoodsics.set(moodsics)));
  }

  loadPublic(): Observable<Moodsic[]> {
    return this.http.get<Moodsic[]>('/api/moodsics/public');
  }

  upload(file: File, name: string, isPublic: boolean): Observable<Moodsic> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', name);
    formData.append('isPublic', String(isPublic));

    return this.http.post<Moodsic>('/api/moodsics', formData).pipe(
      tap((moodsic) => {
        this.myMoodsics.update((list) => [...list, moodsic]);
        if (isPublic) {
          this.availableMoodsics.update((list) => [...list, moodsic]);
        }
      })
    );
  }

  toggleVisibility(id: number): Observable<Moodsic> {
    return this.http.patch<Moodsic>(`/api/moodsics/${id}/visibility`, {}).pipe(
      tap((updated) => {
        this.myMoodsics.update((list) =>
          list.map((m) => (m.id === id ? updated : m))
        );
      })
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/moodsics/${id}`).pipe(
      tap(() => {
        this.myMoodsics.update((list) => list.filter((m) => m.id !== id));
        this.availableMoodsics.update((list) => list.filter((m) => m.id !== id));
      })
    );
  }
}
