import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { RoomListComponent } from '../room-list/room-list.component';
import { GlobeComponent } from '../globe/globe.component';
import { ChatWindowComponent } from '../chat-window/chat-window.component';
import { RoomInfoCardComponent } from '../room-info-card/room-info-card.component';
import { RoomService, ChatService, AuthService, ToastService } from '../../../core/services';
import { Room, RoomMarker } from '../../../core/models';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, RoomListComponent, GlobeComponent, ChatWindowComponent, RoomInfoCardComponent],
  template: `
    <div class="chat-layout">
      <!-- Left Panel - Room List -->
      <aside class="sidebar">
        <app-room-list
          [rooms]="roomService.rooms()"
          [activeRoomCode]="activeRoom()?.joinCode || null"
          (roomSelected)="onRoomSelected($event)"
          (leaveRoom)="onLeaveRoom($event)"
          (joinByCode)="onJoinByCode($event)"
          (locateRoom)="onLocateRoom($event)"
          (logout)="onLogout()"
        />
      </aside>

      <!-- Main Content - Globe -->
      <main class="main-content">
        <!-- Top Bar with CREATE ROOM button -->
        <div class="top-bar">
          <div class="app-title">
            <lucide-icon name="globe" [size]="24" class="globe-icon"></lucide-icon>
            <span>GlobeChat</span>
          </div>
          @if (!createMode()) {
            <button class="create-room-btn" (click)="enterCreateMode()">
              <lucide-icon name="plus" [size]="18" class="plus-icon"></lucide-icon>
              CREATE ROOM
            </button>
          } @else {
            <button class="cancel-btn" (click)="cancelCreateMode()">
              <lucide-icon name="x" [size]="18" class="x-icon"></lucide-icon>
              CANCEL
            </button>
          }
        </div>

        <app-globe
          [markers]="roomService.markers()"
          [selectedMarker]="selectedMarker()"
          [createMode]="createMode()"
          (markerClicked)="onMarkerClicked($event)"
          (mapClicked)="onMapClicked($event)"
          (refreshRooms)="onRefreshRooms()"
        />

        <!-- Room Info Card Popup -->
        @if (showInfoCard() && selectedMarker()) {
          <app-room-info-card
            [marker]="selectedMarker()!"
            [isJoined]="isMarkerJoined()"
            (close)="closeInfoCard()"
            (join)="onJoinRoom($event)"
            (open)="onOpenRoom($event)"
          />
        }

        <!-- Create Room Modal -->
        @if (showCreateModal()) {
          <div class="modal-overlay" (click)="cancelCreateMode()">
            <div class="create-room-modal" (click)="$event.stopPropagation()">
              <div class="modal-header">
                <h2>Create New Room</h2>
                <button class="close-btn" (click)="cancelCreateMode()"><lucide-icon name="x" [size]="20"></lucide-icon></button>
              </div>
              <form (ngSubmit)="submitCreateRoom()">
                <div class="form-group">
                  <label for="title">Room Title *</label>
                  <input
                    type="text"
                    id="title"
                    [(ngModel)]="newRoom.title"
                    name="title"
                    placeholder="Enter room title..."
                    required
                  />
                </div>
                <div class="form-group">
                  <label for="description">Description</label>
                  <textarea
                    id="description"
                    [(ngModel)]="newRoom.description"
                    name="description"
                    placeholder="Describe your room..."
                    rows="3"
                  ></textarea>
                </div>
                <div class="form-group">
                  <label for="rules">Rules (optional)</label>
                  <textarea
                    id="rules"
                    [(ngModel)]="newRoom.rules"
                    name="rules"
                    placeholder="• Be respectful&#10;• No spam&#10;• Have fun!"
                    rows="4"
                  ></textarea>
                </div>
                <div class="location-info">
                  <lucide-icon name="map-pin" [size]="18" class="location-icon"></lucide-icon>
                  <span>Location: {{ createCoords()?.lat?.toFixed(4) }}, {{ createCoords()?.lng?.toFixed(4) }}</span>
                </div>
                <div class="modal-actions">
                  <button type="button" class="btn-secondary" (click)="cancelCreateMode()">Cancel</button>
                  <button type="submit" class="btn-primary" [disabled]="!newRoom.title">Create Room</button>
                </div>
              </form>
            </div>
          </div>
        }
      </main>

      <!-- Right Panel - Chat Window (Desktop) -->
      @if (activeRoom()) {
        <aside class="chat-panel" [class.collapsed]="chatCollapsed()">
          <app-chat-window
            [room]="activeRoom()!"
            [collapsed]="chatCollapsed()"
            (toggleCollapse)="toggleChatCollapse()"
            (closeChat)="closeChat()"
            (kicked)="onKicked($event)"
            (memberCountChanged)="onMemberCountChanged($event)"
            (locateRoom)="onLocateRoom($event)"
          />
        </aside>
      }

      <!-- Mobile Tab Bar -->
      <nav class="mobile-tab-bar">
        <button
          class="tab-btn"
          [class.active]="showMobileRooms()"
          (click)="toggleMobileRooms()"
        >
          <lucide-icon name="home" [size]="20" class="tab-icon"></lucide-icon>
          <span class="tab-label">Rooms</span>
          @if (roomService.rooms().length > 0) {
            <span class="tab-badge">{{ roomService.rooms().length }}</span>
          }
        </button>
        <button
          class="tab-btn"
          [class.active]="showMobileChat()"
          [class.disabled]="!activeRoom()"
          (click)="toggleMobileChat()"
          [disabled]="!activeRoom()"
        >
          <lucide-icon name="message-circle" [size]="20" class="tab-icon"></lucide-icon>
          <span class="tab-label">Chat</span>
        </button>
      </nav>

      <!-- Mobile Room Sheet -->
      @if (showMobileRooms()) {
        <div class="mobile-backdrop" (click)="closeMobileRooms()"></div>
        <div class="mobile-room-sheet">
          <div class="sheet-handle"></div>
          <app-room-list
            [rooms]="roomService.rooms()"
            [activeRoomCode]="activeRoom()?.joinCode || null"
            (roomSelected)="onMobileRoomSelected($event)"
            (leaveRoom)="onLeaveRoom($event)"
            (joinByCode)="onMobileJoinByCode($event)"
            (locateRoom)="onMobileLocateRoom($event)"
            (logout)="onLogout()"
          />
        </div>
      }

      <!-- Mobile Chat Overlay -->
      @if (showMobileChat() && activeRoom()) {
        <div class="mobile-chat-overlay">
          <app-chat-window
            [room]="activeRoom()!"
            [collapsed]="false"
            [isMobile]="true"
            (toggleCollapse)="closeMobileChat()"
            (closeChat)="onMobileCloseChat()"
            (kicked)="onKicked($event)"
            (memberCountChanged)="onMemberCountChanged($event)"
            (locateRoom)="onMobileLocateRoom($event)"
          />
        </div>
      }
    </div>
  `,
  styles: [`
    .chat-layout {
      display: flex;
      height: 100vh;
      background: #0a0a1a;
      overflow: hidden;
    }

    .sidebar {
      width: 320px;
      min-width: 320px;
      background: rgba(10, 25, 47, 0.95);
      border-right: 1px solid rgba(0, 255, 136, 0.2);
      display: flex;
      flex-direction: column;
      z-index: 10;
    }

    .main-content {
      flex: 1;
      position: relative;
      overflow: hidden;
    }

    .top-bar {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 60px;
      background: linear-gradient(to bottom, rgba(10, 25, 47, 0.95), rgba(10, 25, 47, 0));
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 20px;
      z-index: 100;
    }

    .app-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 20px;
      font-weight: 600;
      color: var(--neon-green);
      text-shadow: 0 0 10px rgba(0, 255, 136, 0.5);
    }

    .globe-icon {
      font-size: 24px;
    }

    .create-room-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 20px;
      background: transparent;
      border: 2px solid var(--neon-green);
      border-radius: 8px;
      color: var(--neon-green);
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 1px;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .create-room-btn:hover {
      background: rgba(0, 255, 136, 0.15);
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.4), inset 0 0 20px rgba(0, 255, 136, 0.1);
      transform: translateY(-2px);
    }

    .create-room-btn .plus-icon {
      font-size: 18px;
      font-weight: bold;
    }

    .cancel-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 20px;
      background: transparent;
      border: 2px solid #ff4757;
      border-radius: 8px;
      color: #ff4757;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 1px;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .cancel-btn:hover {
      background: rgba(255, 71, 87, 0.15);
      box-shadow: 0 0 20px rgba(255, 71, 87, 0.4);
    }

    .chat-panel {
      width: 400px;
      min-width: 400px;
      background: rgba(10, 25, 47, 0.95);
      border-left: 1px solid rgba(0, 255, 136, 0.2);
      z-index: 10;
      transition: width 0.3s ease, min-width 0.3s ease;
    }

    .chat-panel.collapsed {
      width: 60px;
      min-width: 60px;
    }

    /* Create Room Modal */
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease;
    }

    .create-room-modal {
      background: rgba(10, 25, 47, 0.98);
      border: 1px solid var(--neon-green);
      border-radius: 16px;
      padding: 24px;
      width: 90%;
      max-width: 500px;
      box-shadow: 0 0 40px rgba(0, 255, 136, 0.3);
      animation: slideUp 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .modal-header h2 {
      color: var(--neon-green);
      font-size: 20px;
      margin: 0;
    }

    .close-btn {
      background: none;
      border: none;
      color: #666;
      font-size: 20px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: all 0.2s;
    }

    .close-btn:hover {
      color: #ff4757;
      background: rgba(255, 71, 87, 0.1);
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      color: var(--neon-green);
      font-size: 14px;
      margin-bottom: 8px;
    }

    .form-group input,
    .form-group textarea {
      width: 100%;
      padding: 12px 16px;
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 8px;
      color: #fff;
      font-size: 14px;
      font-family: inherit;
      transition: all 0.2s;
      box-sizing: border-box;
    }

    .form-group input:focus,
    .form-group textarea:focus {
      outline: none;
      border-color: var(--neon-green);
      box-shadow: 0 0 10px rgba(0, 255, 136, 0.2);
    }

    .form-group input::placeholder,
    .form-group textarea::placeholder {
      color: #666;
    }

    .location-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.3);
      border-radius: 8px;
      color: var(--neon-blue);
      font-size: 14px;
      margin-bottom: 24px;
    }

    .modal-actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
    }

    .btn-secondary {
      padding: 12px 24px;
      background: transparent;
      border: 1px solid #666;
      border-radius: 8px;
      color: #999;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-secondary:hover {
      border-color: #999;
      color: #fff;
    }

    .btn-primary {
      padding: 12px 24px;
      background: var(--neon-green);
      border: none;
      border-radius: 8px;
      color: #0a0a1a;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-primary:hover:not(:disabled) {
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.5);
      transform: translateY(-2px);
    }

    .btn-primary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* Mobile Tab Bar */
    .mobile-tab-bar {
      display: none;
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      height: 60px;
      background: rgba(10, 25, 47, 0.98);
      border-top: 1px solid rgba(0, 255, 136, 0.2);
      z-index: 100;
      padding: 0 20px;
      gap: 20px;
      align-items: center;
      justify-content: center;
    }

    .tab-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      padding: 8px 24px;
      background: transparent;
      border: none;
      color: rgba(255, 255, 255, 0.5);
      cursor: pointer;
      transition: all 0.2s;
      position: relative;
      border-radius: 12px;
    }

    .tab-btn:hover:not(:disabled) {
      color: rgba(255, 255, 255, 0.8);
    }

    .tab-btn.active {
      color: var(--neon-green);
      background: rgba(0, 255, 136, 0.1);
    }

    .tab-btn.disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }

    .tab-icon {
      font-size: 20px;
    }

    .tab-label {
      font-size: 11px;
      font-weight: 500;
    }

    .tab-badge {
      position: absolute;
      top: 4px;
      right: 16px;
      min-width: 18px;
      height: 18px;
      background: var(--neon-green);
      color: #0a0a1a;
      font-size: 10px;
      font-weight: 700;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 4px;
    }

    /* Mobile Backdrop */
    .mobile-backdrop {
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 60px;
      background: rgba(0, 0, 0, 0.5);
      z-index: 89;
      animation: fadeIn 0.2s ease;
    }

    /* Mobile Room Sheet */
    .mobile-room-sheet {
      display: none;
      position: fixed;
      bottom: 60px;
      left: 0;
      right: 0;
      height: 65vh;
      background: rgba(10, 25, 47, 0.98);
      border-radius: 20px 20px 0 0;
      border-top: 1px solid rgba(0, 255, 136, 0.3);
      z-index: 90;
      animation: slideUpSheet 0.3s ease;
      overflow: hidden;
    }

    .sheet-handle {
      width: 40px;
      height: 4px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 2px;
      margin: 12px auto;
    }

    @keyframes slideUpSheet {
      from {
        transform: translateY(100%);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }

    /* Mobile Chat Overlay */
    .mobile-chat-overlay {
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(10, 25, 47, 0.98);
      z-index: 200;
      animation: slideInRight 0.3s ease;
    }

    @keyframes slideInRight {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    /* Mobile Media Queries */
    @media (max-width: 767px) {
      .sidebar {
        display: none;
      }

      .chat-panel {
        display: none;
      }

      .mobile-tab-bar {
        display: flex;
      }

      .mobile-backdrop {
        display: block;
      }

      .mobile-room-sheet {
        display: flex;
        flex-direction: column;
      }

      .mobile-chat-overlay {
        display: flex;
        flex-direction: column;
      }

      .main-content {
        padding-bottom: 60px;
      }

      .top-bar {
        padding: 0 12px;
      }

      .app-title span:not(.globe-icon) {
        display: none;
      }

      .create-room-btn {
        padding: 8px 12px;
        font-size: 12px;
      }

      .create-room-btn .plus-icon {
        font-size: 16px;
      }

      .cancel-btn {
        padding: 8px 12px;
        font-size: 12px;
      }
    }
  `]
})
export class ChatLayoutComponent implements OnInit, OnDestroy {
  @ViewChild(GlobeComponent) globeComponent!: GlobeComponent;

  roomService = inject(RoomService);
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  activeRoom = signal<Room | null>(null);
  selectedMarker = signal<RoomMarker | null>(null);
  showInfoCard = signal(false);
  chatCollapsed = signal(false);
  createMode = signal(false);
  showCreateModal = signal(false);
  createCoords = signal<{ lat: number; lng: number } | null>(null);

  // Mobile state
  isMobile = signal(typeof window !== 'undefined' && window.innerWidth < 768);
  showMobileRooms = signal(false);
  showMobileChat = signal(false);

  @HostListener('window:resize')
  onResize(): void {
    this.isMobile.set(window.innerWidth < 768);
    // Close mobile panels when switching to desktop
    if (!this.isMobile()) {
      this.showMobileRooms.set(false);
      this.showMobileChat.set(false);
    }
  }

  newRoom = {
    title: '',
    description: '',
    rules: ''
  };

  isMarkerJoined = computed(() => {
    const marker = this.selectedMarker();
    if (!marker) return false;
    return this.roomService.rooms().some(r => r.joinCode === marker.joinCode);
  });

  ngOnInit(): void {
    this.loadData();
    this.chatService.connect();
  }

  ngOnDestroy(): void {
    this.chatService.disconnect();
  }

  private loadData(): void {
    this.roomService.loadMyRooms().subscribe();
    this.roomService.loadRoomMarkers().subscribe();
  }

  enterCreateMode(): void {
    this.createMode.set(true);
    this.selectedMarker.set(null);
    this.showInfoCard.set(false);
  }

  cancelCreateMode(): void {
    this.createMode.set(false);
    this.showCreateModal.set(false);
    this.createCoords.set(null);
    this.newRoom = { title: '', description: '', rules: '' };
  }

  onRoomSelected(room: Room): void {
    this.activeRoom.set(room);
    this.roomService.getRoom(room.joinCode).subscribe();
    this.roomService.loadMembers(room.joinCode).subscribe();
    this.roomService.getUserRole(room.joinCode).subscribe();
    this.selectedMarker.set(null);
    this.showInfoCard.set(false);
  }

  onLeaveRoom(joinCode: string): void {
    this.roomService.leaveRoom(joinCode).subscribe(() => {
      if (this.activeRoom()?.joinCode === joinCode) {
        this.activeRoom.set(null);
      }
    });
  }

  onJoinByCode(joinCode: string): void {
    this.roomService.joinRoom(joinCode).subscribe(room => {
      this.activeRoom.set(room);
    });
  }

  onMarkerClicked(marker: RoomMarker): void {
    if (this.createMode()) {
      return; // Ignore marker clicks in create mode
    }
    // Fetch fresh marker data from backend to get updated member count
    this.roomService.getRoomMarker(marker.joinCode).subscribe(freshMarker => {
      this.selectedMarker.set(freshMarker);
      this.showInfoCard.set(true);
    });
  }

  onRefreshRooms(): void {
    this.roomService.loadRoomMarkers().subscribe();
  }

  onMapClicked(coords: { lat: number; lng: number }): void {
    if (this.createMode()) {
      // In create mode, show the create modal with the coordinates
      this.createCoords.set(coords);
      this.showCreateModal.set(true);
    } else {
      this.selectedMarker.set(null);
      this.showInfoCard.set(false);
    }
  }

  submitCreateRoom(): void {
    const coords = this.createCoords();
    if (!this.newRoom.title || !coords) return;

    this.roomService.createRoom({
      title: this.newRoom.title,
      description: this.newRoom.description || undefined,
      rules: this.newRoom.rules || undefined,
      latitude: coords.lat,
      longitude: coords.lng
    }).subscribe(room => {
      this.cancelCreateMode();
      this.activeRoom.set(room);
      this.roomService.loadRoomMarkers().subscribe();
    });
  }

  closeInfoCard(): void {
    this.showInfoCard.set(false);
    this.selectedMarker.set(null);
  }

  onJoinRoom(joinCode: string): void {
    this.roomService.joinRoom(joinCode).subscribe(room => {
      this.activeRoom.set(room);
      this.showInfoCard.set(false);
    });
  }

  onOpenRoom(joinCode: string): void {
    const room = this.roomService.rooms().find(r => r.joinCode === joinCode);
    if (room) {
      this.onRoomSelected(room);
    }
    this.showInfoCard.set(false);
  }

  toggleChatCollapse(): void {
    this.chatCollapsed.update(v => !v);
  }

  closeChat(): void {
    this.activeRoom.set(null);
    this.roomService.clearActiveRoom();
  }

  onKicked(event: { joinCode: string; type: 'KICK' | 'BAN' }): void {
    const roomTitle = this.activeRoom()?.title || 'the room';

    // Close the chat window
    this.activeRoom.set(null);
    this.roomService.clearActiveRoom();

    // Remove the room from the user's room list
    this.roomService.removeRoomLocally(event.joinCode);

    // Show toast notification
    if (event.type === 'KICK') {
      this.toastService.error('Kicked', `You have been kicked from "${roomTitle}"`);
    } else {
      this.toastService.error('Banned', `You have been banned from "${roomTitle}"`);
    }
  }

  onMemberCountChanged(event: { joinCode: string; delta: number }): void {
    this.roomService.updateRoomMemberCount(event.joinCode, event.delta);
  }

  onLocateRoom(joinCode: string): void {
    this.globeComponent?.focusOnRoom(joinCode);
  }

  onLogout(): void {
    this.authService.logout();
  }

  // Mobile methods
  toggleMobileRooms(): void {
    this.showMobileRooms.update(v => !v);
    if (this.showMobileRooms()) {
      this.showMobileChat.set(false);
    }
  }

  toggleMobileChat(): void {
    if (!this.activeRoom()) return;
    this.showMobileChat.update(v => !v);
    if (this.showMobileChat()) {
      this.showMobileRooms.set(false);
    }
  }

  closeMobileRooms(): void {
    this.showMobileRooms.set(false);
  }

  closeMobileChat(): void {
    this.showMobileChat.set(false);
  }

  onMobileRoomSelected(room: Room): void {
    this.onRoomSelected(room);
    this.closeMobileRooms();
    this.showMobileChat.set(true);
  }

  onMobileJoinByCode(joinCode: string): void {
    this.roomService.joinRoom(joinCode).subscribe(room => {
      this.activeRoom.set(room);
      this.closeMobileRooms();
      this.showMobileChat.set(true);
    });
  }

  onMobileLocateRoom(joinCode: string): void {
    this.closeMobileRooms();
    this.closeMobileChat();
    this.onLocateRoom(joinCode);
  }

  onMobileCloseChat(): void {
    this.closeMobileChat();
    this.closeChat();
  }
}
