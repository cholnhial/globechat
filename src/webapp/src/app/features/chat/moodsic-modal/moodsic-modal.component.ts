import {
  Component,
  Output,
  EventEmitter,
  OnInit,
  signal,
  inject,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Moodsic } from '../../../core/models';
import { MoodsicService } from '../../../core/services/moodsic.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-moodsic-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal-content">
        <div class="modal-header">
          <h2><lucide-icon name="music" [size]="24"></lucide-icon> Room Moodsic</h2>
          <button class="btn-close" (click)="close.emit()"><lucide-icon name="x" [size]="20"></lucide-icon></button>
        </div>

        <div class="modal-body">
          <!-- Upload Section -->
          <div class="upload-section">
            <h3>Upload New Track</h3>
            <div class="upload-form">
              <div class="file-input-wrapper">
                <input
                  type="file"
                  accept="audio/*"
                  (change)="onFileSelected($event)"
                  #fileInput
                />
                <button class="btn-file" (click)="fileInput.click()">
                  @if (selectedFile) {
                    {{ selectedFile.name }}
                  } @else {
                    <lucide-icon name="folder" [size]="16"></lucide-icon> Choose Audio File
                  }
                </button>
              </div>

              <input
                type="text"
                [(ngModel)]="uploadTitle"
                placeholder="Track title..."
                class="input-title"
              />

              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="isPublic" />
                <span>Public (others can use this track)</span>
              </label>

              @if (uploadProgress() > 0 && uploadProgress() < 100) {
                <div class="progress-bar">
                  <div class="progress-fill" [style.width.%]="uploadProgress()"></div>
                </div>
                <span class="progress-text">{{ uploadProgress() }}%</span>
              }

              <button
                class="btn-upload"
                (click)="uploadTrack()"
                [disabled]="!canUpload() || isUploading()"
              >
                @if (isUploading()) {
                  Uploading...
                } @else {
                  <lucide-icon name="upload" [size]="16"></lucide-icon> Upload & Play
                }
              </button>
            </div>
          </div>

          <div class="divider">
            <span>or select from library</span>
          </div>

          <!-- Filter & Search Section -->
          <div class="filter-section">
            <div class="filter-toggle">
              <button
                class="filter-btn"
                [class.active]="activeFilter() === 'all'"
                (click)="setFilter('all')"
              >
                All
              </button>
              <button
                class="filter-btn"
                [class.active]="activeFilter() === 'mine'"
                (click)="setFilter('mine')"
              >
                My Moodsics
              </button>
            </div>

            <div class="search-wrapper">
              <lucide-icon name="search" [size]="16" class="search-icon"></lucide-icon>
              <input
                type="text"
                [(ngModel)]="searchQuery"
                (ngModelChange)="onSearchChange($event)"
                placeholder="Search tracks..."
                class="search-input"
              />
            </div>
          </div>

          <!-- Track List -->
          <div class="track-list">
            @if (isSearching()) {
              <div class="loading">Searching...</div>
            } @else if (tracks().length === 0) {
              <div class="empty">No tracks found</div>
            } @else {
              @for (track of tracks(); track track.id) {
                <div class="track-item">
                  <div class="track-info">
                    <span class="track-name">{{ track.name }}</span>
                    <span class="track-uploader">by {{ track.uploadedBy.username }}</span>
                  </div>
                  <button class="btn-set-mood" (click)="selectTrack(track)">
                    <lucide-icon name="music" [size]="14"></lucide-icon> Set as Mood
                  </button>
                </div>
              }
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      backdrop-filter: blur(4px);
    }

    .modal-content {
      background: rgba(10, 25, 47, 0.98);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 16px;
      width: 90%;
      max-width: 500px;
      max-height: 80vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 0 40px rgba(0, 255, 136, 0.2);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid rgba(0, 255, 136, 0.2);
    }

    .modal-header h2 {
      margin: 0;
      color: #fff;
      font-size: 20px;
    }

    .btn-close {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #fff;
      font-size: 20px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-close:hover {
      background: rgba(255, 68, 68, 0.2);
      border-color: rgba(255, 68, 68, 0.4);
    }

    .modal-body {
      padding: 20px;
      overflow-y: auto;
      flex: 1;
    }

    .upload-section h3 {
      margin: 0 0 16px;
      color: var(--neon-green, #00ff88);
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .upload-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .file-input-wrapper input[type="file"] {
      display: none;
    }

    .btn-file {
      width: 100%;
      padding: 12px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px dashed rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      color: #888;
      cursor: pointer;
      transition: all 0.2s;
      text-align: left;
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .btn-file:hover {
      background: rgba(0, 255, 136, 0.05);
      border-color: rgba(0, 255, 136, 0.3);
      color: #fff;
    }

    .input-title {
      padding: 12px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      color: #fff;
      font-size: 14px;
    }

    .input-title:focus {
      outline: none;
      border-color: rgba(0, 255, 136, 0.5);
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #aaa;
      font-size: 14px;
      cursor: pointer;
    }

    .checkbox-label input[type="checkbox"] {
      width: 18px;
      height: 18px;
      accent-color: var(--neon-green, #00ff88);
    }

    .progress-bar {
      height: 6px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 3px;
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--neon-green, #00ff88), #00d4ff);
      transition: width 0.3s ease;
    }

    .progress-text {
      text-align: center;
      color: var(--neon-green, #00ff88);
      font-size: 12px;
    }

    .btn-upload {
      padding: 14px 20px;
      background: linear-gradient(135deg, rgba(0, 255, 136, 0.2), rgba(0, 212, 255, 0.2));
      border: 1px solid rgba(0, 255, 136, 0.4);
      border-radius: 8px;
      color: var(--neon-green, #00ff88);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-upload:hover:not(:disabled) {
      background: linear-gradient(135deg, rgba(0, 255, 136, 0.3), rgba(0, 212, 255, 0.3));
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.3);
    }

    .btn-upload:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .divider {
      display: flex;
      align-items: center;
      margin: 24px 0;
      color: #666;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .divider::before,
    .divider::after {
      content: '';
      flex: 1;
      height: 1px;
      background: rgba(255, 255, 255, 0.1);
    }

    .divider span {
      padding: 0 16px;
    }

    .filter-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 16px;
    }

    .filter-toggle {
      display: flex;
      gap: 8px;
    }

    .filter-btn {
      flex: 1;
      padding: 10px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      color: #888;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .filter-btn.active {
      background: rgba(0, 255, 136, 0.15);
      border-color: rgba(0, 255, 136, 0.4);
      color: var(--neon-green, #00ff88);
    }

    .filter-btn:hover:not(.active) {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
    }

    .search-wrapper {
      position: relative;
      display: flex;
      align-items: center;
      width: 100%;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      color: rgba(255, 255, 255, 0.4);
      pointer-events: none;
    }

    .search-input {
      width: 100%;
      padding: 12px 16px 12px 40px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      color: #fff;
      font-size: 14px;
    }

    .search-input:focus {
      outline: none;
      border-color: rgba(0, 255, 136, 0.5);
    }

    .track-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
      min-height: 0;
      max-height: 300px;
      overflow-y: auto;
    }

    .track-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 8px;
      transition: all 0.2s;
    }

    .track-item:hover {
      background: rgba(255, 255, 255, 0.06);
      border-color: rgba(0, 255, 136, 0.2);
    }

    .track-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      overflow: hidden;
    }

    .track-name {
      color: #fff;
      font-size: 14px;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .track-uploader {
      color: #666;
      font-size: 12px;
    }

    .btn-set-mood {
      padding: 8px 14px;
      background: rgba(0, 255, 136, 0.1);
      border: 1px solid rgba(0, 255, 136, 0.3);
      border-radius: 6px;
      color: var(--neon-green, #00ff88);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      white-space: nowrap;
    }

    .btn-set-mood:hover {
      background: rgba(0, 255, 136, 0.2);
      box-shadow: 0 0 12px rgba(0, 255, 136, 0.3);
    }

    .loading,
    .empty {
      text-align: center;
      padding: 24px;
      color: #666;
      font-size: 14px;
    }
  `],
})
export class MoodsicModalComponent implements OnInit, OnDestroy {
  @Output() close = new EventEmitter<void>();
  @Output() moodsicSelected = new EventEmitter<Moodsic>();

  private moodsicService = inject(MoodsicService);
  private toastService = inject(ToastService);

  selectedFile: File | null = null;
  uploadTitle = '';
  isPublic = true;
  searchQuery = '';

  uploadProgress = signal(0);
  isUploading = signal(false);
  activeFilter = signal<'all' | 'mine'>('all');
  tracks = signal<Moodsic[]>([]);
  isSearching = signal(false);

  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  ngOnInit(): void {
    // Set up debounced search
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(query => {
      this.performSearch(query);
    });

    // Initial load
    this.loadTracks();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close.emit();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      // Auto-fill title from filename if empty
      if (!this.uploadTitle) {
        this.uploadTitle = this.selectedFile.name.replace(/\.[^/.]+$/, '');
      }
    }
  }

  canUpload(): boolean {
    return !!this.selectedFile && !!this.uploadTitle.trim();
  }

  uploadTrack(): void {
    if (!this.canUpload() || !this.selectedFile) return;

    this.isUploading.set(true);
    this.uploadProgress.set(0);

    this.moodsicService.uploadWithProgress(
      this.selectedFile,
      this.uploadTitle.trim(),
      this.isPublic
    ).subscribe({
      next: (progress) => {
        this.uploadProgress.set(progress.progress);
        if (progress.moodsic) {
          this.toastService.success('Success', 'Track uploaded successfully!');
          this.moodsicSelected.emit(progress.moodsic);
          this.close.emit();
        }
      },
      error: () => {
        this.toastService.error('Error', 'Failed to upload track');
        this.isUploading.set(false);
        this.uploadProgress.set(0);
      }
    });
  }

  setFilter(filter: 'all' | 'mine'): void {
    this.activeFilter.set(filter);
    this.performSearch(this.searchQuery);
  }

  onSearchChange(query: string): void {
    this.searchSubject.next(query);
  }

  selectTrack(track: Moodsic): void {
    this.moodsicSelected.emit(track);
    this.close.emit();
  }

  private loadTracks(): void {
    this.performSearch('');
  }

  private performSearch(query: string): void {
    this.isSearching.set(true);
    this.moodsicService.search(query, this.activeFilter()).subscribe({
      next: (tracks) => {
        this.tracks.set(tracks);
        this.isSearching.set(false);
      },
      error: () => {
        this.tracks.set([]);
        this.isSearching.set(false);
      }
    });
  }
}
