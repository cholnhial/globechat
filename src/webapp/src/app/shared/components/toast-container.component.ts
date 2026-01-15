import { Component, inject } from '@angular/core';
import { ToastService, ToastMessage } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="toast-container">
      @for (toast of toastService.messages(); track toast.id) {
        <div class="toast toast-{{ toast.type }}" (click)="toastService.remove(toast.id)">
          <div class="toast-icon">
            @switch (toast.type) {
              @case ('error') { ⚠️ }
              @case ('success') { ✅ }
              @case ('warning') { ⚡ }
              @case ('info') { ℹ️ }
            }
          </div>
          <div class="toast-content">
            <div class="toast-title">{{ toast.title }}</div>
            <div class="toast-message">{{ toast.message }}</div>
          </div>
          <button class="toast-close">×</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 400px;
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 16px;
      border-radius: 12px;
      background: rgba(10, 25, 47, 0.95);
      border: 1px solid var(--neon-green);
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.3);
      cursor: pointer;
      animation: slideIn 0.3s ease-out;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateX(100%);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .toast-error {
      border-color: #ff4444;
      box-shadow: 0 0 20px rgba(255, 68, 68, 0.3);
    }

    .toast-success {
      border-color: var(--neon-green);
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.3);
    }

    .toast-warning {
      border-color: #ffaa00;
      box-shadow: 0 0 20px rgba(255, 170, 0, 0.3);
    }

    .toast-info {
      border-color: var(--neon-blue);
      box-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
    }

    .toast-icon {
      font-size: 24px;
    }

    .toast-content {
      flex: 1;
    }

    .toast-title {
      font-weight: 600;
      color: #fff;
      margin-bottom: 4px;
    }

    .toast-message {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
    }

    .toast-close {
      background: none;
      border: none;
      color: rgba(255, 255, 255, 0.5);
      font-size: 20px;
      cursor: pointer;
      padding: 0;
      line-height: 1;
    }

    .toast-close:hover {
      color: #fff;
    }
  `]
})
export class ToastContainerComponent {
  toastService = inject(ToastService);
}
