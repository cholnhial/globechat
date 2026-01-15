import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { RoomMarker } from '../../../core/models';
import { RoomService } from '../../../core/services';
import { formatDistanceToNow } from 'date-fns';

@Component({
  selector: 'app-room-info-card',
  standalone: true,
  template: `
    <div class="card-overlay" (click)="close.emit()">
      <div class="info-card" (click)="$event.stopPropagation()">
        <button class="close-btn" (click)="close.emit()">×</button>

        <div class="card-header">
          <span class="room-icon">💬</span>
          <h2>{{ marker.title }}</h2>
        </div>

        @if (room()) {
          <div class="card-content">
            @if (room()!.description) {
              <p class="description">{{ room()!.description }}</p>
            }

            @if (room()!.rules) {
              <div class="rules">
                <h4>📋 Rules</h4>
                <div class="rules-content">{{ room()!.rules }}</div>
              </div>
            }

            <div class="stats">
              <div class="stat">
                <span class="stat-icon">👥</span>
                <span class="stat-value">{{ marker.memberCount }}</span>
                <span class="stat-label">Members</span>
              </div>
              <div class="stat">
                <span class="stat-icon">⏱️</span>
                <span class="stat-value">{{ getUptime() }}</span>
                <span class="stat-label">Active</span>
              </div>
            </div>

            <div class="join-code-section">
              <span class="label">Join Code</span>
              <code class="join-code">{{ marker.joinCode }}</code>
            </div>
          </div>
        } @else {
          <div class="loading">Loading...</div>
        }

        <div class="card-footer">
          @if (isJoined) {
            <button class="btn-primary" (click)="open.emit(marker.joinCode)">
              Open Chat
            </button>
          } @else {
            <button class="btn-primary" (click)="join.emit(marker.joinCode)">
              Join Room
            </button>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
      animation: fadeIn 0.2s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .info-card {
      position: relative;
      background: rgba(10, 25, 47, 0.98);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 20px;
      width: 90%;
      max-width: 400px;
      max-height: 80vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 0 40px rgba(0, 255, 136, 0.2);
      animation: slideUp 0.3s ease-out;
    }

    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .close-btn {
      position: absolute;
      top: 16px;
      right: 16px;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      color: #fff;
      font-size: 20px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-btn:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .card-header {
      padding: 24px;
      text-align: center;
      border-bottom: 1px solid rgba(0, 255, 136, 0.1);
    }

    .room-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 12px;
    }

    h2 {
      margin: 0;
      color: #fff;
      font-size: 24px;
    }

    .card-content {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }

    .description {
      color: rgba(255, 255, 255, 0.8);
      margin: 0 0 20px;
      line-height: 1.6;
    }

    .rules {
      margin-bottom: 20px;
    }

    .rules h4 {
      color: var(--neon-green);
      margin: 0 0 12px;
      font-size: 14px;
    }

    .rules-content {
      background: rgba(0, 0, 0, 0.3);
      padding: 12px 16px;
      border-radius: 10px;
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
      white-space: pre-line;
    }

    .stats {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
    }

    .stat {
      flex: 1;
      background: rgba(0, 255, 136, 0.05);
      border: 1px solid rgba(0, 255, 136, 0.2);
      border-radius: 12px;
      padding: 16px;
      text-align: center;
    }

    .stat-icon {
      font-size: 24px;
      display: block;
      margin-bottom: 8px;
    }

    .stat-value {
      display: block;
      color: var(--neon-green);
      font-size: 24px;
      font-weight: 700;
    }

    .stat-label {
      display: block;
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
      margin-top: 4px;
    }

    .join-code-section {
      text-align: center;
    }

    .label {
      display: block;
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
      margin-bottom: 8px;
    }

    .join-code {
      display: inline-block;
      background: rgba(0, 0, 0, 0.4);
      padding: 12px 24px;
      border-radius: 10px;
      font-family: monospace;
      font-size: 20px;
      color: var(--neon-blue);
      letter-spacing: 4px;
    }

    .loading {
      padding: 40px;
      text-align: center;
      color: rgba(255, 255, 255, 0.5);
    }

    .card-footer {
      padding: 20px 24px;
      border-top: 1px solid rgba(0, 255, 136, 0.1);
    }

    .btn-primary {
      width: 100%;
      padding: 14px;
      background: linear-gradient(135deg, var(--neon-green), #00aa66);
      border: none;
      border-radius: 12px;
      color: #0a0a1a;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 5px 20px rgba(0, 255, 136, 0.4);
    }
  `]
})
export class RoomInfoCardComponent implements OnInit {
  private roomService = inject(RoomService);

  @Input() marker!: RoomMarker;
  @Input() isJoined = false;

  @Output() close = new EventEmitter<void>();
  @Output() join = new EventEmitter<string>();
  @Output() open = new EventEmitter<string>();

  room = this.roomService.currentRoom;

  ngOnInit(): void {
    this.roomService.getRoom(this.marker.joinCode).subscribe();
  }

  getUptime(): string {
    const room = this.room();
    if (!room) return '...';
    return formatDistanceToNow(new Date(room.createdAt), { addSuffix: false });
  }
}
