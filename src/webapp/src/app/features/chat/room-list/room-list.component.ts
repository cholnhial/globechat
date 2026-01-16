import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Room } from '../../../core/models';
import { AuthService } from '../../../core/services';

type RoomFilter = 'all' | 'owned';

@Component({
  selector: 'app-room-list',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="room-list">
      <!-- Header -->
      <div class="header">
        <div class="logo">
          <span class="logo-icon">🌐</span>
          <span class="logo-text">GlobeChat</span>
        </div>
        <button class="btn-icon" (click)="logout.emit()" title="Logout">
          🚪
        </button>
      </div>

      <!-- User Info -->
      <div class="user-info">
        <div class="avatar">{{ authService.user()?.username?.charAt(0)?.toUpperCase() }}</div>
        <span class="username">{{ authService.user()?.username }}</span>
      </div>

      <!-- Join by Code -->
      <div class="join-section">
        <div class="input-group">
          <input
            type="text"
            [(ngModel)]="joinCode"
            placeholder="Enter join code..."
            (keyup.enter)="onJoinByCode()"
          />
          <button class="btn-join" (click)="onJoinByCode()" [disabled]="!joinCode">
            Join
          </button>
        </div>
      </div>

      <!-- Room List -->
      <div class="section-header">
        <span class="section-title">My Rooms</span>
        <div class="filter-toggle">
          <button
            class="filter-btn"
            [class.active]="filter() === 'all'"
            (click)="filter.set('all')"
          >All</button>
          <button
            class="filter-btn"
            [class.active]="filter() === 'owned'"
            (click)="filter.set('owned')"
          >Owned</button>
        </div>
      </div>
      <div class="rooms-container">
        @if (filteredRooms.length === 0) {
          <div class="empty-state">
            <span>🔍</span>
            @if (filter() === 'owned') {
              <p>No owned rooms</p>
              <small>Create a room to see it here</small>
            } @else {
              <p>No rooms yet</p>
              <small>Click on the globe to create or join a room</small>
            }
          </div>
        } @else {
          @for (room of filteredRooms; track room.joinCode) {
            <div
              class="room-item"
              [class.active]="activeRoomCode === room.joinCode"
              (click)="roomSelected.emit(room)"
            >
              <div class="room-icon">💬</div>
              <div class="room-info">
                <div class="room-title">{{ room.title }}</div>
                <div class="room-meta">
                  <span class="member-count">👥 {{ room.memberCount }}</span>
                  <span class="join-code">{{ room.joinCode }}</span>
                </div>
              </div>
              <div class="room-actions">
                <button
                  class="btn-locate"
                  (click)="onLocateRoom($event, room.joinCode)"
                  title="Show on map"
                >
                  🎯
                </button>
                <button
                  class="btn-leave"
                  (click)="onLeaveRoom($event, room.joinCode)"
                  title="Leave room"
                >
                  ×
                </button>
              </div>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
    }

    .room-list {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20px;
      border-bottom: 1px solid rgba(0, 255, 136, 0.1);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .logo-icon {
      font-size: 28px;
    }

    .logo-text {
      font-size: 20px;
      font-weight: 700;
      background: linear-gradient(135deg, var(--neon-green), var(--neon-blue));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .btn-icon {
      background: none;
      border: none;
      font-size: 20px;
      cursor: pointer;
      opacity: 0.7;
      transition: opacity 0.2s;
    }

    .btn-icon:hover {
      opacity: 1;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 20px;
      background: rgba(0, 255, 136, 0.05);
    }

    .avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--neon-green), var(--neon-blue));
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: #0a0a1a;
    }

    .username {
      color: #fff;
      font-weight: 500;
    }

    .join-section {
      padding: 16px 20px;
      border-bottom: 1px solid rgba(0, 255, 136, 0.1);
    }

    .input-group {
      display: flex;
      gap: 8px;
    }

    .input-group input {
      flex: 1;
      padding: 10px 14px;
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 8px;
      color: #fff;
      font-size: 14px;
    }

    .input-group input::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }

    .input-group input:focus {
      outline: none;
      border-color: var(--neon-green);
    }

    .btn-join {
      padding: 10px 16px;
      background: var(--neon-green);
      border: none;
      border-radius: 8px;
      color: #0a0a1a;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-join:hover:not(:disabled) {
      box-shadow: 0 0 15px rgba(0, 255, 136, 0.4);
    }

    .btn-join:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 20px 8px;
    }

    .section-title {
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .filter-toggle {
      display: flex;
      gap: 4px;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 6px;
      padding: 2px;
    }

    .filter-btn {
      padding: 4px 10px;
      font-size: 11px;
      border: none;
      border-radius: 4px;
      background: transparent;
      color: rgba(255, 255, 255, 0.5);
      cursor: pointer;
      transition: all 0.2s;
    }

    .filter-btn:hover {
      color: rgba(255, 255, 255, 0.8);
    }

    .filter-btn.active {
      background: var(--neon-green);
      color: #0a0a1a;
      font-weight: 600;
    }

    .rooms-container {
      flex: 1;
      overflow-y: auto;
      padding: 0 12px 12px;
    }

    .empty-state {
      text-align: center;
      padding: 40px 20px;
      color: rgba(255, 255, 255, 0.5);
    }

    .empty-state span {
      font-size: 48px;
      display: block;
      margin-bottom: 16px;
    }

    .empty-state p {
      margin: 0 0 8px;
      color: #fff;
    }

    .empty-state small {
      font-size: 12px;
    }

    .room-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;
      margin-bottom: 4px;
    }

    .room-item:hover {
      background: rgba(0, 255, 136, 0.1);
    }

    .room-item.active {
      background: rgba(0, 255, 136, 0.15);
      border: 1px solid rgba(0, 255, 136, 0.3);
    }

    .room-icon {
      font-size: 24px;
    }

    .room-info {
      flex: 1;
      min-width: 0;
    }

    .room-title {
      color: #fff;
      font-weight: 500;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .room-meta {
      display: flex;
      gap: 12px;
      margin-top: 4px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.5);
    }

    .member-count {
      color: var(--neon-green);
    }

    .join-code {
      font-family: monospace;
    }

    .room-actions {
      display: flex;
      gap: 4px;
      opacity: 0;
      transition: opacity 0.2s;
    }

    .room-item:hover .room-actions {
      opacity: 1;
    }

    .btn-locate {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.3);
      color: #00d4ff;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .btn-locate:hover {
      background: rgba(0, 212, 255, 0.2);
      box-shadow: 0 0 10px rgba(0, 212, 255, 0.4);
    }

    .btn-leave {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: rgba(255, 68, 68, 0.1);
      border: 1px solid rgba(255, 68, 68, 0.3);
      color: #ff4444;
      font-size: 18px;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }


    .btn-leave:hover {
      background: rgba(255, 68, 68, 0.2);
    }
  `]
})
export class RoomListComponent {
  authService = inject(AuthService);

  @Input() rooms: Room[] = [];
  @Input() activeRoomCode: string | null = null;

  @Output() roomSelected = new EventEmitter<Room>();
  @Output() leaveRoom = new EventEmitter<string>();
  @Output() joinByCode = new EventEmitter<string>();
  @Output() locateRoom = new EventEmitter<string>();
  @Output() logout = new EventEmitter<void>();

  joinCode = '';
  filter = signal<RoomFilter>('all');

  get filteredRooms(): Room[] {
    const currentUsername = this.authService.user()?.username;
    if (this.filter() === 'owned') {
      return this.rooms.filter(room => room.owner.username === currentUsername);
    }
    return this.rooms;
  }

  onJoinByCode(): void {
    if (this.joinCode.trim()) {
      this.joinByCode.emit(this.joinCode.trim().toUpperCase());
      this.joinCode = '';
    }
  }

  onLeaveRoom(event: Event, joinCode: string): void {
    event.stopPropagation();
    this.leaveRoom.emit(joinCode);
  }

  onLocateRoom(event: Event, joinCode: string): void {
    event.stopPropagation();
    this.locateRoom.emit(joinCode);
  }
}
