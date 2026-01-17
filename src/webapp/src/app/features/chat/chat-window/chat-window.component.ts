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
import { LucideAngularModule } from 'lucide-angular';
import { Room, RoomMember, ChatMessage, MemberRole, Moodsic } from '../../../core/models';
import { RoomService, ChatService, AuthService, ToastService } from '../../../core/services';
import { MoodsicService } from '../../../core/services/moodsic.service';
import { Subscription, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { formatDistanceToNow } from 'date-fns';
import { MoodsicModalComponent } from '../moodsic-modal/moodsic-modal.component';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, MoodsicModalComponent],
  styleUrl: './chat-window.component.scss',
  template: `
    <div class="chat-window" [class.collapsed]="collapsed" [class.mobile]="isMobile">
      <!-- Header -->
      <div class="chat-header">
        @if (collapsed) {
          <button class="btn-expand" (click)="toggleCollapse.emit()">
            <lucide-icon name="message-circle" [size]="24"></lucide-icon>
          </button>
        } @else {
          @if (isMobile) {
            <button class="btn-back" (click)="toggleCollapse.emit()" title="Minimize">
              <lucide-icon name="chevron-down" [size]="20"></lucide-icon>
            </button>
          }
          <div class="header-info">
            <h3>{{ room.title }}</h3>
            <span class="member-count"><lucide-icon name="users" [size]="14"></lucide-icon> {{ roomService.members().length }}</span>
          </div>
          <div class="header-actions">
            @if (userRole() === 'OWNER') {
              <button class="btn-icon btn-moodsic" (click)="showMoodsicModal.set(true)" title="Room Moodsic">
                <lucide-icon name="music" [size]="18"></lucide-icon>
              </button>
            }
            <button class="btn-icon" (click)="showMembers.set(!showMembers())" title="Members">
              <lucide-icon name="users" [size]="18"></lucide-icon>
            </button>
            @if (!isMobile) {
              <button class="btn-icon btn-locate" (click)="locateRoom.emit(room.joinCode)" title="Show on map">
                <lucide-icon name="target" [size]="18"></lucide-icon>
              </button>
              <button class="btn-icon" (click)="toggleCollapse.emit()" title="Collapse">
                <lucide-icon name="arrow-right" [size]="18"></lucide-icon>
              </button>
            }
            <button class="btn-icon" (click)="closeChat.emit()" title="Close">
              <lucide-icon name="x" [size]="18"></lucide-icon>
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
                <lucide-icon name="plug" [size]="24" class="disconnect-icon"></lucide-icon>
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
                    <lucide-icon [name]="getSystemIcon(message.type)" [size]="16" class="system-icon"></lucide-icon>
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
                <button class="btn-close" (click)="showMembers.set(false)"><lucide-icon name="x" [size]="18"></lucide-icon></button>
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
                        <lucide-icon name="user-x" [size]="16"></lucide-icon>
                      </button>
                    }
                  </div>
                }
              </div>
            </div>
          }
        </div>

        <!-- Now Playing Notch -->
        @if (room.currentMoodsic) {
          <div class="now-playing-notch">
            <div class="now-playing-info">
              <lucide-icon name="music" [size]="18" class="music-icon"></lucide-icon>
              <div class="track-title-container">
                <span class="track-title" [class.scrolling]="room.currentMoodsic.name.length > 25">
                  {{ room.currentMoodsic.name }}
                </span>
              </div>
            </div>
            <div class="player-controls">
              <button class="btn-player" (click)="togglePlayPause()" [title]="isPlaying() ? 'Pause' : 'Play'">
                <lucide-icon [name]="isPlaying() ? 'pause' : 'play'" [size]="14"></lucide-icon>
              </button>
              <input
                type="range"
                class="volume-slider"
                min="0"
                max="1"
                step="0.1"
                [value]="volume()"
                (input)="onVolumeChange($event)"
                title="Volume"
              />
              <button class="btn-player" (click)="toggleMute()" [title]="isMuted() ? 'Unmute' : 'Mute'">
                <lucide-icon [name]="isMuted() ? 'volume-x' : 'volume-2'" [size]="14"></lucide-icon>
              </button>
              @if (userRole() === 'OWNER') {
                <button class="btn-player btn-stop" (click)="clearMoodsic()" title="Stop Music">
                  <lucide-icon name="square" [size]="14"></lucide-icon>
                </button>
              }
            </div>
            <audio
              #audioPlayer
              [src]="getStreamUrl(room.currentMoodsic.id)"
              loop
              (ended)="onAudioEnded()"
            ></audio>
          </div>
        }

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
            <lucide-icon name="send" [size]="20"></lucide-icon>
          </button>
        </div>

        <!-- Room Controls (for owner/mod) -->
        @if (userRole() === 'OWNER') {
          <div class="room-controls">
            <button class="btn-control btn-danger" (click)="destroyRoom()">
              <lucide-icon name="trash-2" [size]="16"></lucide-icon> Destroy Room
            </button>
          </div>
        }
      }
    </div>

    <!-- Moodsic Modal -->
    @if (showMoodsicModal()) {
      <app-moodsic-modal
        (close)="showMoodsicModal.set(false)"
        (moodsicSelected)="onMoodsicSelected($event)"
      />
    }
  `
})
export class ChatWindowComponent implements OnInit, OnDestroy, OnChanges, AfterViewChecked {
  roomService = inject(RoomService);
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private moodsicService = inject(MoodsicService);

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  @Input() room!: Room;
  @Input() collapsed = false;
  @Input() isMobile = false;

  @Output() toggleCollapse = new EventEmitter<void>();
  @Output() closeChat = new EventEmitter<void>();
  @Output() kicked = new EventEmitter<{ joinCode: string; type: 'KICK' | 'BAN' }>();
  @Output() memberCountChanged = new EventEmitter<{ joinCode: string; delta: number }>();
  @Output() locateRoom = new EventEmitter<string>();

  messages = signal<ChatMessage[]>([]);
  showMembers = signal(false);
  isDisconnected = signal(false);
  showMoodsicModal = signal(false);
  isPlaying = signal(false);
  volume = signal(0.7);
  isMuted = signal(false);
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

      // Handle moodsic changes from WebSocket - refresh room to get updated moodsic
      if (message.type === 'MOODSIC_CHANGE') {
        this.roomService.getRoom(this.room.joinCode).subscribe(updatedRoom => {
          this.room = updatedRoom;
          // Auto-play for all clients when moodsic changes
          if (updatedRoom.currentMoodsic) {
            setTimeout(() => this.playAudio(), 100);
          } else {
            this.isPlaying.set(false);
          }
        });
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

    // Scroll to bottom after sending (message will arrive via WebSocket)
    setTimeout(() => this.scrollToBottom(), 0);
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
      JOIN: 'log-in',
      LEAVE: 'log-out',
      KICK: 'user-x',
      BAN: 'ban',
      MOODSIC_CHANGE: 'music',
      MOODSIC_TOGGLE: 'play',
      ROOM_DESTROYED: 'zap',
      SYSTEM: 'info',
    };
    return icons[type] || 'info';
  }

  getMemberRole(username: string | undefined): string | null {
    if (!username) return null;
    const member = this.roomService.members().find(m => m.user.username === username);
    return member?.role || null;
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      OWNER: 'Owner',
      MOD: 'Mod',
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

  // Moodsic player methods
  getStreamUrl(id: number): string {
    return this.moodsicService.getStreamUrl(id);
  }

  onMoodsicSelected(moodsic: Moodsic): void {
    this.roomService.setMoodsic(this.room.joinCode, { moodsicId: moodsic.id }).subscribe({
      next: (updatedRoom) => {
        this.room = updatedRoom;
        this.toastService.success('Success', 'Moodsic set successfully!');
        // Auto-play when new moodsic is selected
        setTimeout(() => this.playAudio(), 100);
      },
      error: () => {
        this.toastService.error('Error', 'Failed to set moodsic');
      }
    });
  }

  togglePlayPause(): void {
    const audio = this.audioPlayer?.nativeElement;
    if (!audio) return;

    if (audio.paused) {
      this.playAudio();
    } else {
      audio.pause();
      this.isPlaying.set(false);
    }
  }

  private playAudio(): void {
    const audio = this.audioPlayer?.nativeElement;
    if (!audio) return;

    audio.volume = this.isMuted() ? 0 : this.volume();
    audio.play().then(() => {
      this.isPlaying.set(true);
    }).catch(err => {
      console.warn('Audio playback failed:', err);
    });
  }

  onVolumeChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const newVolume = parseFloat(target.value);
    this.volume.set(newVolume);

    if (newVolume > 0) {
      this.isMuted.set(false);
    }

    const audio = this.audioPlayer?.nativeElement;
    if (audio) {
      audio.volume = newVolume;
    }
  }

  toggleMute(): void {
    const audio = this.audioPlayer?.nativeElement;
    if (!audio) return;

    if (this.isMuted()) {
      this.isMuted.set(false);
      audio.volume = this.volume();
    } else {
      this.isMuted.set(true);
      audio.volume = 0;
    }
  }

  clearMoodsic(): void {
    this.roomService.clearMoodsic(this.room.joinCode).subscribe({
      next: (updatedRoom) => {
        this.room = updatedRoom;
        this.isPlaying.set(false);
        this.toastService.success('Success', 'Moodsic cleared');
      },
      error: () => {
        this.toastService.error('Error', 'Failed to clear moodsic');
      }
    });
  }

  onAudioEnded(): void {
    // Audio loops, so this shouldn't be called, but just in case
    this.isPlaying.set(false);
  }
}
