import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  Room,
  RoomMarker,
  RoomMember,
  MemberRole,
  CreateRoomRequest,
  UpdateRoomRequest,
  BanRequest,
  SetMoodsicRequest,
} from '../models';
import { ChatMessage } from '../models/message.model';

@Injectable({
  providedIn: 'root',
})
export class RoomService {
  private myRooms = signal<Room[]>([]);
  private roomMarkers = signal<RoomMarker[]>([]);
  private activeRoom = signal<Room | null>(null);
  private activeRoomMembers = signal<RoomMember[]>([]);
  private userRole = signal<MemberRole | null>(null);

  readonly rooms = this.myRooms.asReadonly();
  readonly markers = this.roomMarkers.asReadonly();
  readonly currentRoom = this.activeRoom.asReadonly();
  readonly members = this.activeRoomMembers.asReadonly();
  readonly role = this.userRole.asReadonly();

  constructor(private http: HttpClient) {}

  loadMyRooms(): Observable<Room[]> {
    return this.http.get<Room[]>('/api/rooms/my').pipe(tap((rooms) => this.myRooms.set(rooms)));
  }

  loadRoomMarkers(): Observable<RoomMarker[]> {
    return this.http
      .get<RoomMarker[]>('/api/rooms/markers')
      .pipe(tap((markers) => this.roomMarkers.set(markers)));
  }

  getRoomMarker(joinCode: string): Observable<RoomMarker> {
    return this.http.get<RoomMarker>(`/api/rooms/markers/${joinCode}`).pipe(
      tap((marker) => {
        // Update the marker in the markers list
        this.roomMarkers.update((markers) =>
          markers.map((m) => (m.joinCode === joinCode ? marker : m))
        );
      })
    );
  }

  getRoom(joinCode: string): Observable<Room> {
    return this.http.get<Room>(`/api/rooms/${joinCode}`).pipe(
      tap((room) => {
        this.activeRoom.set(room);
        // Sync member count to myRooms
        this.myRooms.update((rooms) =>
          rooms.map((r) =>
            r.joinCode === joinCode ? { ...r, memberCount: room.memberCount } : r
          )
        );
      })
    );
  }

  createRoom(request: CreateRoomRequest): Observable<Room> {
    return this.http.post<Room>('/api/rooms', request).pipe(
      tap((room) => {
        this.myRooms.update((rooms) => [...rooms, room]);
        this.roomMarkers.update((markers) => [
          ...markers,
          {
            joinCode: room.joinCode,
            title: room.title,
            latitude: room.latitude,
            longitude: room.longitude,
            memberCount: room.memberCount,
          },
        ]);
      })
    );
  }

  updateRoom(joinCode: string, request: UpdateRoomRequest): Observable<Room> {
    return this.http.put<Room>(`/api/rooms/${joinCode}`, request).pipe(
      tap((room) => {
        this.activeRoom.set(room);
        this.myRooms.update((rooms) =>
          rooms.map((r) => (r.joinCode === joinCode ? room : r))
        );
      })
    );
  }

  deleteRoom(joinCode: string): Observable<void> {
    return this.http.delete<void>(`/api/rooms/${joinCode}`).pipe(
      tap(() => {
        this.myRooms.update((rooms) => rooms.filter((r) => r.joinCode !== joinCode));
        this.roomMarkers.update((markers) => markers.filter((m) => m.joinCode !== joinCode));
        if (this.activeRoom()?.joinCode === joinCode) {
          this.activeRoom.set(null);
        }
      })
    );
  }

  joinRoom(joinCode: string): Observable<Room> {
    return this.http.post<Room>(`/api/rooms/${joinCode}/join`, {}).pipe(
      tap((room) => {
        this.myRooms.update((rooms) => [...rooms, room]);
        this.activeRoom.set(room);
      })
    );
  }

  leaveRoom(joinCode: string): Observable<void> {
    return this.http.post<void>(`/api/rooms/${joinCode}/leave`, {}).pipe(
      tap(() => {
        this.myRooms.update((rooms) => rooms.filter((r) => r.joinCode !== joinCode));
        if (this.activeRoom()?.joinCode === joinCode) {
          this.activeRoom.set(null);
        }
      })
    );
  }

  loadMembers(joinCode: string): Observable<RoomMember[]> {
    return this.http
      .get<RoomMember[]>(`/api/rooms/${joinCode}/members`)
      .pipe(tap((members) => this.activeRoomMembers.set(members)));
  }

  getUserRole(joinCode: string): Observable<MemberRole> {
    return this.http
      .get<MemberRole>(`/api/rooms/${joinCode}/role`)
      .pipe(tap((role) => this.userRole.set(role)));
  }

  kickUser(joinCode: string, username: string): Observable<void> {
    return this.http.post<void>(`/api/rooms/${joinCode}/kick/${username}`, {});
  }

  banUser(joinCode: string, request: BanRequest): Observable<void> {
    return this.http.post<void>(`/api/rooms/${joinCode}/ban`, request);
  }

  unbanUser(joinCode: string, username: string): Observable<void> {
    return this.http.delete<void>(`/api/rooms/${joinCode}/ban/${username}`);
  }

  promoteMod(joinCode: string, username: string): Observable<void> {
    return this.http.post<void>(`/api/rooms/${joinCode}/mods/${username}`, {});
  }

  demoteMod(joinCode: string, username: string): Observable<void> {
    return this.http.delete<void>(`/api/rooms/${joinCode}/mods/${username}`);
  }

  setMoodsic(joinCode: string, request: SetMoodsicRequest): Observable<Room> {
    return this.http.put<Room>(`/api/rooms/${joinCode}/moodsic`, request).pipe(
      tap((room) => this.activeRoom.set(room))
    );
  }

  clearMoodsic(joinCode: string): Observable<Room> {
    return this.http.delete<Room>(`/api/rooms/${joinCode}/moodsic`).pipe(
      tap((room) => this.activeRoom.set(room))
    );
  }

  toggleMoodsicPause(joinCode: string): Observable<Room> {
    return this.http.post<Room>(`/api/rooms/${joinCode}/moodsic/toggle`, {}).pipe(
      tap((room) => this.activeRoom.set(room))
    );
  }

  getMessages(joinCode: string, limit = 100): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`/api/rooms/${joinCode}/messages`, {
      params: { limit: limit.toString() },
    });
  }

  clearActiveRoom(): void {
    this.activeRoom.set(null);
    this.activeRoomMembers.set([]);
    this.userRole.set(null);
  }

  /**
   * Remove a room from the local state without making an API call.
   * Used when the user is kicked/banned from a room.
   */
  removeRoomLocally(joinCode: string): void {
    this.myRooms.update((rooms) => rooms.filter((r) => r.joinCode !== joinCode));
    if (this.activeRoom()?.joinCode === joinCode) {
      this.activeRoom.set(null);
      this.activeRoomMembers.set([]);
      this.userRole.set(null);
    }
  }

  /**
   * Update member count for a room locally without making an API call.
   * Used when users join/leave the room via WebSocket messages.
   */
  updateRoomMemberCount(joinCode: string, delta: number): void {
    this.myRooms.update((rooms) =>
      rooms.map((r) =>
        r.joinCode === joinCode
          ? { ...r, memberCount: Math.max(0, r.memberCount + delta) }
          : r
      )
    );
  }
}
