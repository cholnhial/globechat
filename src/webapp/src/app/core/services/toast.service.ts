import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  type: 'error' | 'success' | 'warning' | 'info';
  title: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private nextId = 0;
  private toastMessages = signal<ToastMessage[]>([]);

  readonly messages = this.toastMessages.asReadonly();

  error(title: string, message: string): void {
    this.show('error', title, message);
  }

  success(title: string, message: string): void {
    this.show('success', title, message);
  }

  warning(title: string, message: string): void {
    this.show('warning', title, message);
  }

  info(title: string, message: string): void {
    this.show('info', title, message);
  }

  private show(type: ToastMessage['type'], title: string, message: string): void {
    const id = this.nextId++;
    const toast: ToastMessage = { id, type, title, message };

    this.toastMessages.update((messages) => [...messages, toast]);

    // Auto-remove after 5 seconds
    setTimeout(() => this.remove(id), 5000);
  }

  remove(id: number): void {
    this.toastMessages.update((messages) => messages.filter((m) => m.id !== id));
  }
}
