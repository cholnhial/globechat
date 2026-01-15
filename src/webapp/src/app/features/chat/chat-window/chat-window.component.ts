import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  inject,
  signal,
  ViewChild,
  ElementRef,
  AfterViewChecked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Room, RoomMember, ChatMessage, MemberRole } from '../../../core/models';
import { RoomService, ChatService, AuthService, ToastService } from '../../../core/services';
import { Subscription, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { formatDistanceToNow } from 'date-fns';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="chat-window" [class.collapsed]="collapsed">
      <!-- Header -->
      <div class="chat-header">
        @if (collapsed) {
          <button class="btn-expand" (click)="toggleCollapse.emit()">
            💬
          </button>
        } @else {
          <div class="header-info">
            <h3>{{ room.title }}</h3>
            <span class="member-count">👥 {{ roomService.members().length }}</span>
          </div>
          <div class="header-actions">
            <button class="btn-icon" (click)="showMembers.set(!showMembers())" title="Members">
              👥
            </button>
            <button class="btn-icon" (click)="toggleCollapse.emit()" title="Collapse">
              ➡️
            </button>
            <button class="btn-icon" (click)="closeChat.emit()" title="Close">
              ×
            </button>
          </div>
        }
      </div>

      @if (!collapsed) {
        <div class="chat-body">
          <!-- Messages Area -->
          <div class="messages-container" #messagesContainer>
            @if (isDisconnected()) {
              <div class="disconnected-banner">
                <span class="disconnect-icon">🔌</span>
                <span>Room has been destroyed</span>
              </div>
            }

            @for (message of messages(); track message.id ?? message.createdAt) {
              <div class="message" [class]="getMessageClass(message)">
                @if (message.type === 'CHAT') {
                  <div class="message-header">
                    <div class="sender-info">
                      <span class="sender">{{ message.sender?.username }}</span>
                      @if (getMemberRole(message.sender?.username); as role) {
                        <span class="sender-role" [class]="role.toLowerCase()">{{ getRoleLabel(role) }}</span>
                      }
                    </div>
                    <span class="time">{{ formatTime(message.createdAt) }}</span>
                  </div>
                  <div class="message-content">{{ message.content }}</div>
                } @else {
                  <div class="system-message">
                    <span class="system-icon">{{ getSystemIcon(message.type) }}</span>
                    <span>{{ message.content }}</span>
                  </div>
                }
              </div>
            }
          </div>

          <!-- Members Panel -->
          @if (showMembers()) {
            <div class="members-panel">
              <div class="members-header">
                <h4>Members</h4>
                <button class="btn-close" (click)="showMembers.set(false)">×</button>
              </div>
              <div class="members-list">
                @for (member of roomService.members(); track member.user.id) {
                  <div class="member-item">
                    <div class="member-avatar">{{ member.user.username.charAt(0).toUpperCase() }}</div>
                    <div class="member-info">
                      <span class="member-name">{{ member.user.username }}</span>
                      <span class="member-role" [class]="member.role.toLowerCase()">
                        {{ member.role }}
                      </span>
                    </div>
                    @if (canKick(member)) {
                      <button class="btn-kick" (click)="kickMember(member)" title="Kick">
                        👢
                      </button>
                    }
                  </div>
                }
              </div>
            </div>
          }
        </div>

        <!-- Input Area -->
        <div class="chat-input" [class.disabled]="isDisconnected()">
          <input
            type="text"
            [(ngModel)]="messageInput"
            placeholder="Type a message..."
            (keyup.enter)="sendMessage()"
            [disabled]="isDisconnected()"
          />
          <button class="btn-send" (click)="sendMessage()" [disabled]="!messageInput.trim() || isDisconnected()">
            📤
          </button>
        </div>

        <!-- Room Controls (for owner/mod) -->
        @if (userRole() === 'OWNER') {
          <div class="room-controls">
            <button class="btn-control btn-danger" (click)="destroyRoom()">
              🗑️ Destroy Room
            </button>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .chat-window {
      height: 100%;
      display: flex;
      flex-direction: column;
      background: rgba(10, 25, 47, 0.95);
    }

    .chat-window.collapsed {
      width: 60px;
    }

    .chat-header {
      padding: 16px;
      border-bottom: 1px solid rgba(0, 255, 136, 0.2);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .header-info h3 {
      margin: 0;
      color: #fff;
      font-size: 16px;
    }

    .member-count {
      color: var(--neon-green);
      font-size: 14px;
    }

    .header-actions {
      display: flex;
      gap: 8px;
    }

    .btn-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #fff;
      font-size: 16px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
    }

    .btn-icon:hover {
      background: rgba(0, 255, 136, 0.1);
      border-color: rgba(0, 255, 136, 0.3);
    }

    .btn-expand {
      width: 100%;
      height: 50px;
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
    }

    .chat-body {
      flex: 1;
      display: flex;
      overflow: hidden;
      position: relative;
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .disconnected-banner {
      background: rgba(255, 68, 68, 0.1);
      border: 1px solid rgba(255, 68, 68, 0.3);
      border-radius: 10px;
      padding: 16px;
      text-align: center;
      color: #ff4444;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
    }

    .disconnect-icon {
      font-size: 24px;
    }

    .message {
      padding: 12px 16px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.03);
    }

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }

    .sender-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .sender {
      color: var(--neon-green);
      font-weight: 600;
      font-size: 14px;
    }

    .sender-role {
      font-size: 9px;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .sender-role.owner {
      background: rgba(255, 215, 0, 0.2);
      color: #ffd700;
      border: 1px solid rgba(255, 215, 0, 0.4);
      text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
    }

    .sender-role.mod {
      background: rgba(0, 212, 255, 0.2);
      color: #00d4ff;
      border: 1px solid rgba(0, 212, 255, 0.4);
      text-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
    }

    .sender-role.chatter {
      display: none; /* Hide chatter role to reduce noise */
    }

    .time {
      color: rgba(255, 255, 255, 0.4);
      font-size: 12px;
    }

    .message-content {
      color: rgba(255, 255, 255, 0.9);
      word-wrap: break-word;
    }

    .message.own {
      background: rgba(0, 255, 136, 0.1);
      border: 1px solid rgba(0, 255, 136, 0.2);
    }

    .message.system {
      background: transparent;
      padding: 8px 16px;
    }

    .system-message {
      display: flex;
      align-items: center;
      gap: 8px;
      color: rgba(255, 255, 255, 0.5);
      font-size: 13px;
    }

    .system-icon {
      font-size: 16px;
    }

    .message.destroyed {
      background: rgba(255, 68, 68, 0.1);
      border: 1px solid rgba(255, 68, 68, 0.3);
    }

    .message.destroyed .system-message {
      color: #ff4444;
    }

    .members-panel {
      width: 200px;
      border-left: 1px solid rgba(0, 255, 136, 0.2);
      background: rgba(0, 0, 0, 0.2);
      display: flex;
      flex-direction: column;
    }

    .members-header {
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid rgba(0, 255, 136, 0.1);
    }

    .members-header h4 {
      margin: 0;
      color: #fff;
      font-size: 14px;
    }

    .btn-close {
      background: none;
      border: none;
      color: rgba(255, 255, 255, 0.5);
      font-size: 18px;
      cursor: pointer;
    }

    .members-list {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
    }

    .member-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px;
      border-radius: 8px;
    }

    .member-item:hover {
      background: rgba(255, 255, 255, 0.05);
    }

    .member-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--neon-green), var(--neon-blue));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      color: #0a0a1a;
    }

    .member-info {
      flex: 1;
      min-width: 0;
    }

    .member-name {
      display: block;
      color: #fff;
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .member-role {
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .member-role.owner {
      background: rgba(255, 215, 0, 0.2);
      color: gold;
    }

    .member-role.mod {
      background: rgba(0, 212, 255, 0.2);
      color: var(--neon-blue);
    }

    .member-role.chatter {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.5);
    }

    .btn-kick {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      background: rgba(255, 68, 68, 0.1);
      border: none;
      cursor: pointer;
      opacity: 0;
      transition: opacity 0.2s;
    }

    .member-item:hover .btn-kick {
      opacity: 1;
    }

    .chat-input {
      padding: 16px;
      border-top: 1px solid rgba(0, 255, 136, 0.2);
      display: flex;
      gap: 12px;
    }

    .chat-input.disabled {
      opacity: 0.5;
      pointer-events: none;
    }

    .chat-input input {
      flex: 1;
      padding: 12px 16px;
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 12px;
      color: #fff;
      font-size: 14px;
    }

    .chat-input input::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }

    .chat-input input:focus {
      outline: none;
      border-color: var(--neon-green);
    }

    .btn-send {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: var(--neon-green);
      border: none;
      font-size: 20px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-send:hover:not(:disabled) {
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.5);
    }

    .btn-send:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .room-controls {
      padding: 12px 16px;
      border-top: 1px solid rgba(0, 255, 136, 0.1);
    }

    .btn-control {
      width: 100%;
      padding: 10px;
      border-radius: 8px;
      font-size: 13px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .btn-danger {
      background: rgba(255, 68, 68, 0.1);
      border: 1px solid rgba(255, 68, 68, 0.3);
      color: #ff4444;
    }

    .btn-danger:hover {
      background: rgba(255, 68, 68, 0.2);
    }
  `]
})
export class ChatWindowComponent implements OnInit, OnDestroy, OnChanges, AfterViewChecked {
  roomService = inject(RoomService);
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  @Input() room!: Room;
  @Input() collapsed = false;

  @Output() toggleCollapse = new EventEmitter<void>();
  @Output() closeChat = new EventEmitter<void>();
  @Output() kicked = new EventEmitter<{ joinCode: string; type: 'KICK' | 'BAN' }>();
  @Output() memberCountChanged = new EventEmitter<{ joinCode: string; delta: number }>();

  messages = signal<ChatMessage[]>([]);
  showMembers = signal(false);
  isDisconnected = signal(false);
  messageInput = '';

  userRole = this.roomService.role;

  private chatSubscription?: Subscription;
  private refreshSubscription?: Subscription;
  private refreshMembersSubject = new Subject<void>();
  private shouldScrollToBottom = true;

  ngOnInit(): void {
    this.loadMessages();
    this.subscribeToChat();

    // Subscribe to debounced member refresh (500ms debounce)
    this.refreshSubscription = this.refreshMembersSubject.pipe(
      debounceTime(500)
    ).subscribe(() => {
      this.roomService.loadMembers(this.room.joinCode).subscribe();
      this.roomService.getRoom(this.room.joinCode).subscribe();
    });
  }

  ngOnDestroy(): void {
    this.chatSubscription?.unsubscribe();
    this.refreshSubscription?.unsubscribe();
    this.refreshMembersSubject.complete();
    if (this.room) {
      this.chatService.leaveRoom(this.room.joinCode);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['room'] && !changes['room'].firstChange) {
      this.chatSubscription?.unsubscribe();
      this.messages.set([]);
      this.isDisconnected.set(false);
      this.loadMessages();
      this.subscribeToChat();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private loadMessages(): void {
    this.roomService.getMessages(this.room.joinCode).subscribe(msgs => {
      this.messages.set(msgs);
      this.shouldScrollToBottom = true;
    });
  }

  private subscribeToChat(): void {
    const currentUsername = this.authService.user()?.username;

    this.chatSubscription = this.chatService.joinRoom(this.room.joinCode).subscribe(message => {
      if (message.type === 'ROOM_DESTROYED') {
        this.isDisconnected.set(true);
      }

      // Check if current user is kicked or banned
      if ((message.type === 'KICK' || message.type === 'BAN') && message.targetUsername === currentUsername) {
        this.kicked.emit({ joinCode: this.room.joinCode, type: message.type });
        return; // Don't add the message since we're being removed
      }

      // Emit member count changes for the My Rooms panel
      if (message.type === 'JOIN') {
        this.memberCountChanged.emit({ joinCode: this.room.joinCode, delta: 1 });
      } else if (message.type === 'LEAVE' || message.type === 'KICK' || message.type === 'BAN') {
        this.memberCountChanged.emit({ joinCode: this.room.joinCode, delta: -1 });
      }

      // Trigger debounced refresh when someone joins, leaves, gets kicked, or gets banned
      if (message.type === 'JOIN' || message.type === 'LEAVE' || message.type === 'KICK' || message.type === 'BAN') {
        this.refreshMembersSubject.next();
      }

      this.messages.update(msgs => [...msgs, message]);
      this.shouldScrollToBottom = true;
    });
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  sendMessage(): void {
    if (!this.messageInput.trim() || this.isDisconnected()) return;

    this.chatService.sendMessage(this.room.joinCode, this.messageInput.trim());
    this.messageInput = '';
  }

  getMessageClass(message: ChatMessage): string {
    const classes: string[] = [];

    if (message.type === 'CHAT') {
      if (message.sender?.username === this.authService.user()?.username) {
        classes.push('own');
      }
    } else {
      classes.push('system');
      if (message.type === 'ROOM_DESTROYED') {
        classes.push('destroyed');
      }
    }

    return classes.join(' ');
  }

  getSystemIcon(type: string): string {
    const icons: Record<string, string> = {
      JOIN: '➡️',
      LEAVE: '⬅️',
      KICK: '👢',
      BAN: '🚫',
      MOODSIC_CHANGE: '🎵',
      MOODSIC_TOGGLE: '⏯️',
      ROOM_DESTROYED: '💥',
      SYSTEM: 'ℹ️',
    };
    return icons[type] || 'ℹ️';
  }

  getMemberRole(username: string | undefined): string | null {
    if (!username) return null;
    const member = this.roomService.members().find(m => m.user.username === username);
    return member?.role || null;
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      OWNER: '👑 Owner',
      MOD: '🛡️ Mod',
      CHATTER: 'Member',
    };
    return labels[role] || role;
  }

  formatTime(date: string): string {
    return formatDistanceToNow(new Date(date), { addSuffix: true });
  }

  canKick(member: RoomMember): boolean {
    const myRole = this.userRole();
    if (!myRole) return false;
    if (member.user.username === this.authService.user()?.username) return false;

    if (myRole === 'OWNER') {
      return true;
    }

    if (myRole === 'MOD') {
      return member.role === 'CHATTER';
    }

    return false;
  }

  kickMember(member: RoomMember): void {
    this.roomService.kickUser(this.room.joinCode, member.user.username).subscribe({
      next: () => {
        this.roomService.loadMembers(this.room.joinCode).subscribe();
        this.toastService.success('Success', `${member.user.username} has been kicked`);
      }
    });
  }

  destroyRoom(): void {
    if (confirm('Are you sure you want to destroy this room? This cannot be undone.')) {
      this.roomService.deleteRoom(this.room.joinCode).subscribe(() => {
        this.closeChat.emit();
      });
    }
  }
}
