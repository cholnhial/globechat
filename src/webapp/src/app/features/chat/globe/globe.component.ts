import {
  Component,
  Input,
  Output,
  EventEmitter,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { Map, Marker } from 'maplibre-gl';
import { RoomMarker } from '../../../core/models';
import { RoomService } from '../../../core/services';
import { MoodsicService } from '../../../core/services/moodsic.service';

@Component({
  selector: 'app-globe',
  standalone: true,
  template: `
    <div class="globe-container">
      <div #mapContainer class="map" [class.create-mode]="createMode"></div>
      <div class="map-controls">
        <button (click)="onRefresh()" title="Refresh rooms" class="refresh-btn">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
            <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
            <path d="M16 21h5v-5"/>
          </svg>
        </button>
        <button (click)="zoomIn()" title="Zoom in">+</button>
        <button (click)="zoomOut()" title="Zoom out">−</button>
        <button (click)="resetView()" title="Reset view">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
            <path d="M2 12h20"/>
          </svg>
        </button>
      </div>
      @if (createMode) {
        <div class="create-mode-hint">
          <span class="pulse-dot"></span>
          Click anywhere on the map to place your room
        </div>
      }
    </div>
  `,
  styles: [`
    .globe-container {
      width: 100%;
      height: 100%;
      position: relative;
    }

    .map {
      width: 100%;
      height: 100%;
    }

    .map.create-mode {
      cursor: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='32' viewBox='0 0 24 24' fill='%2300ff88'%3E%3Cpath d='M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z'/%3E%3C/svg%3E") 16 32, crosshair;
    }

    .map-controls {
      position: absolute;
      bottom: 30px;
      right: 20px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .map-controls button {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: rgba(10, 25, 47, 0.9);
      border: 1px solid rgba(0, 255, 136, 0.3);
      color: var(--neon-green);
      font-size: 20px;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .map-controls button:hover {
      background: rgba(0, 255, 136, 0.1);
      border-color: var(--neon-green);
      box-shadow: 0 0 15px rgba(0, 255, 136, 0.3);
    }

    .create-mode-hint {
      position: absolute;
      bottom: 30px;
      left: 50%;
      transform: translateX(-50%);
      background: linear-gradient(135deg, rgba(10, 25, 47, 0.98) 0%, rgba(23, 42, 69, 0.98) 100%);
      border: 2px solid #00ff88;
      border-radius: 12px;
      padding: 14px 28px;
      color: #00ff88;
      font-size: 15px;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow:
        0 0 25px rgba(0, 255, 136, 0.4),
        0 4px 20px rgba(0, 0, 0, 0.5);
      animation: fadeIn 0.3s ease;
    }

    .pulse-dot {
      width: 12px;
      height: 12px;
      background: var(--neon-green);
      border-radius: 50%;
      animation: pulse 1.5s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(1.2); }
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateX(-50%) translateY(10px); }
      to { opacity: 1; transform: translateX(-50%) translateY(0); }
    }

    :host ::ng-deep .maplibregl-canvas {
      outline: none;
    }

    :host ::ng-deep .room-marker {
      cursor: pointer;
      transition: transform 0.2s;
    }

    :host ::ng-deep .room-marker:hover {
      transform: scale(1.2);
    }

    :host ::ng-deep .marker-container {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0a192f 0%, #172a45 100%);
      border: 3px solid #00ff88;
      border-radius: 50%;
      box-shadow:
        0 0 15px rgba(0, 255, 136, 0.6),
        0 0 30px rgba(0, 255, 136, 0.3),
        inset 0 0 10px rgba(0, 255, 136, 0.2);
      font-size: 20px;
    }

    :host ::ng-deep .marker-container.selected {
      border-color: #00d4ff;
      box-shadow:
        0 0 20px rgba(0, 212, 255, 0.8),
        0 0 40px rgba(0, 212, 255, 0.4),
        inset 0 0 15px rgba(0, 212, 255, 0.3);
      transform: scale(1.2);
    }

    :host ::ng-deep .marker-container.flashing {
      animation: markerFlash 0.33s ease-in-out 9;
    }

    @keyframes markerFlash {
      0%, 100% {
        border-color: #00ff88;
        box-shadow:
          0 0 15px rgba(0, 255, 136, 0.6),
          0 0 30px rgba(0, 255, 136, 0.3),
          inset 0 0 10px rgba(0, 255, 136, 0.2);
        transform: scale(1);
      }
      50% {
        border-color: #00d4ff;
        box-shadow:
          0 0 25px rgba(0, 212, 255, 1),
          0 0 50px rgba(0, 212, 255, 0.6),
          inset 0 0 20px rgba(0, 212, 255, 0.4);
        transform: scale(1.3);
      }
    }

    /* Mobile styles - clear the tab bar */
    @media (max-width: 767px) {
      .map-controls {
        bottom: 80px;
      }

      .create-mode-hint {
        bottom: 80px;
      }
    }

    /* Vibe Check - Music Notes Animation */
    :host ::ng-deep .music-notes-container {
      position: absolute;
      top: 50%;
      left: 50%;
      pointer-events: none;
      z-index: 10;
    }

    :host ::ng-deep .music-note {
      position: absolute;
      color: #00ff88;
      font-size: 18px;
      animation: float-away 1s ease-out forwards;
      text-shadow: 0 0 8px #00ff88, 0 0 15px rgba(0, 255, 136, 0.5);
    }

    @keyframes float-away {
      0% {
        transform: translate(-50%, -50%);
        opacity: 1;
      }
      100% {
        transform:
          translate(-50%, -50%)
          rotate(var(--angle))
          translateY(calc(-1 * var(--distance)));
        opacity: 0;
      }
    }
  `]
})
export class GlobeComponent implements AfterViewInit, OnDestroy, OnChanges {
  private roomService = inject(RoomService);
  private moodsicService = inject(MoodsicService);

  @ViewChild('mapContainer') mapContainer!: ElementRef;

  @Input() markers: RoomMarker[] = [];
  @Input() selectedMarker: RoomMarker | null = null;
  @Input() createMode = false;

  @Output() markerClicked = new EventEmitter<RoomMarker>();
  @Output() mapClicked = new EventEmitter<{ lat: number; lng: number }>();
  @Output() refreshRooms = new EventEmitter<void>();

  private map!: Map;
  private mapMarkers = new window.Map<string, Marker>();

  // Vibe Check state
  private readonly HOVER_DELAY_MS = 300;
  private readonly MAX_PLAY_DURATION = 10000;
  private previewAudio: HTMLAudioElement | null = null;
  private vibeCheckMarker = signal<string | null>(null);
  private hoverTimeout: ReturnType<typeof setTimeout> | null = null;
  private maxPlayTimeout: ReturnType<typeof setTimeout> | null = null;
  private noteInterval: ReturnType<typeof setInterval> | null = null;

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    this.stopVibeCheck();
    if (this.map) {
      this.map.remove();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['markers'] && this.map) {
      this.updateMarkers();
    }
    if (changes['selectedMarker'] && this.map) {
      this.updateSelectedMarker();
    }
  }

  private initMap(): void {
    this.map = new Map({
      container: this.mapContainer.nativeElement,
      style: {
        version: 8,
        name: 'GlobeChat SciFi',
        sources: {
          'carto-positron': {
            type: 'raster',
            tiles: [
              'https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png',
              'https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png',
              'https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png',
            ],
            tileSize: 256,
            attribution: '© OpenStreetMap contributors © CARTO',
          },
        },
        layers: [
          {
            id: 'background',
            type: 'background',
            paint: {
              'background-color': '#0d1b2a',
            },
          },
          {
            id: 'carto-positron',
            type: 'raster',
            source: 'carto-positron',
            paint: {
              'raster-opacity': 1,
              'raster-saturation': 0.1,
              'raster-brightness-min': 0.15,
              'raster-brightness-max': 0.9,
              'raster-contrast': 0.2,
              'raster-hue-rotate': 160,
            },
          },
        ],
        glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      },
      center: [0, 20],
      zoom: 2,
      minZoom: 1,
      maxZoom: 18,
      attributionControl: false,
    });

    // Add globe projection for 3D effect
    this.map.on('load', () => {

      this.updateMarkers();
    });

    // Handle map clicks
    this.map.on('click', (e) => {
      this.mapClicked.emit({ lat: e.lngLat.lat, lng: e.lngLat.lng });
    });
  }

  private updateMarkers(): void {
    // Remove old markers
    this.mapMarkers.forEach((marker) => marker.remove());
    this.mapMarkers.clear();

    // Add new markers
    this.markers.forEach((roomMarker) => {
      const el = document.createElement('div');
      el.className = 'room-marker';
      el.innerHTML = `
        <div class="marker-container ${this.selectedMarker?.joinCode === roomMarker.joinCode ? 'selected' : ''}">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#00ff88" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/>
          </svg>
        </div>
        <div class="music-notes-container"></div>
      `;

      // Click to select room
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        this.stopVibeCheck();
        this.markerClicked.emit(roomMarker);
      });

      // Vibe check: hover/hold to preview moodsic
      el.addEventListener('pointerenter', () => this.startVibeCheck(roomMarker));
      el.addEventListener('pointerleave', () => this.stopVibeCheck());
      el.addEventListener('pointercancel', () => this.stopVibeCheck());

      const marker = new Marker({ element: el })
        .setLngLat([roomMarker.longitude, roomMarker.latitude])
        .addTo(this.map);

      this.mapMarkers.set(roomMarker.joinCode, marker);
    });
  }

  private updateSelectedMarker(): void {
    this.mapMarkers.forEach((marker, joinCode) => {
      const el = marker.getElement();
      const container = el.querySelector('.marker-container');
      if (container) {
        if (this.selectedMarker?.joinCode === joinCode) {
          container.classList.add('selected');
        } else {
          container.classList.remove('selected');
        }
      }
    });
  }

  zoomIn(): void {
    this.map.zoomIn();
  }

  zoomOut(): void {
    this.map.zoomOut();
  }

  resetView(): void {
    this.map.flyTo({
      center: [0, 20],
      zoom: 2,
      duration: 1500,
    });
  }

  onRefresh(): void {
    this.refreshRooms.emit();
  }

  focusOnRoom(joinCode: string): void {
    const roomMarker = this.markers.find(m => m.joinCode === joinCode);
    if (!roomMarker) return;

    // Fly to the marker location
    this.map.flyTo({
      center: [roomMarker.longitude, roomMarker.latitude],
      zoom: 10,
      duration: 1000,
    });

    // Flash the marker after flying completes
    setTimeout(() => {
      const marker = this.mapMarkers.get(joinCode);
      if (marker) {
        const container = marker.getElement().querySelector('.marker-container');
        if (container) {
          container.classList.add('flashing');
          // Remove flashing class after animation completes (9 iterations * 0.33s = ~3s)
          setTimeout(() => {
            container.classList.remove('flashing');
          }, 3000);
        }
      }
    }, 1000);
  }

  // Vibe Check Methods
  private startVibeCheck(marker: RoomMarker): void {
    this.hoverTimeout = setTimeout(() => {
      this.playVibeCheck(marker);
    }, this.HOVER_DELAY_MS);
  }

  private stopVibeCheck(): void {
    if (this.hoverTimeout) {
      clearTimeout(this.hoverTimeout);
      this.hoverTimeout = null;
    }
    if (this.maxPlayTimeout) {
      clearTimeout(this.maxPlayTimeout);
      this.maxPlayTimeout = null;
    }
    if (this.noteInterval) {
      clearInterval(this.noteInterval);
      this.noteInterval = null;
    }
    this.stopPreviewAudio();
    this.vibeCheckMarker.set(null);
  }

  private playVibeCheck(marker: RoomMarker): void {
    this.roomService.getRoom(marker.joinCode).subscribe(room => {
      if (room.currentMoodsic) {
        this.vibeCheckMarker.set(marker.joinCode);
        this.playPreviewAudio(room.currentMoodsic.id);
        this.spawnMusicNotes(marker.joinCode);

        // Auto-stop after max duration
        this.maxPlayTimeout = setTimeout(() => {
          this.stopVibeCheck();
        }, this.MAX_PLAY_DURATION);
      }
    });
  }

  private playPreviewAudio(moodsicId: number): void {
    this.stopPreviewAudio();
    this.previewAudio = new Audio(this.moodsicService.getStreamUrl(moodsicId));
    this.previewAudio.volume = 0.5;
    this.previewAudio.play().catch(() => {});
  }

  private stopPreviewAudio(): void {
    if (this.previewAudio) {
      this.previewAudio.pause();
      this.previewAudio.src = '';
      this.previewAudio = null;
    }
  }

  private spawnMusicNotes(joinCode: string): void {
    const marker = this.mapMarkers.get(joinCode);
    if (!marker) return;

    const container = marker.getElement().querySelector('.music-notes-container');
    if (!container) return;

    const notes = ['♪', '♫', '♬'];

    this.noteInterval = setInterval(() => {
      const note = document.createElement('div');
      note.className = 'music-note';
      note.textContent = notes[Math.floor(Math.random() * notes.length)];
      note.style.setProperty('--angle', `${Math.random() * 360}deg`);
      note.style.setProperty('--distance', `${40 + Math.random() * 30}px`);
      container.appendChild(note);

      // Remove after animation completes
      setTimeout(() => note.remove(), 1000);
    }, 150);
  }
}
