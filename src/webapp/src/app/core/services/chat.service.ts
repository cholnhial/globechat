import { Injectable, signal, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ChatMessage, SendMessageRequest } from '../models';
import { AuthService } from './auth.service';
import { Subject, Observable } from 'rxjs';


@Injectable({
  providedIn: 'root',
})
export class ChatService implements OnDestroy {
  private client: Client | null = null;
  private subscriptions = new Map<string, StompSubscription>();
  private messageSubjects = new Map<string, Subject<ChatMessage>>();
  private connected = signal(false);
  private connectionError = signal<string | null>(null);

  readonly isConnected = this.connected.asReadonly();
  readonly error = this.connectionError.asReadonly();

  constructor(private authService: AuthService) {}

  connect(): void {
    if (this.client?.connected) {
      return;
    }

    const token = this.authService.getToken();
    if (!token) {
      this.connectionError.set('Not authenticated');
      return;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.debug('[STOMP]', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.connected.set(true);
        this.connectionError.set(null);
        console.log('WebSocket connected');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        this.connectionError.set(frame.headers['message'] || 'Connection error');
        this.connected.set(false);
      },
      onWebSocketClose: () => {
        this.connected.set(false);
        console.log('WebSocket disconnected');
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected.set(false);
    }

    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    this.messageSubjects.forEach((subject) => subject.complete());
    this.messageSubjects.clear();
  }

  joinRoom(joinCode: string): Observable<ChatMessage> {
    if (!this.messageSubjects.has(joinCode)) {
      this.messageSubjects.set(joinCode, new Subject<ChatMessage>());
    }

    const subject = this.messageSubjects.get(joinCode)!;

    if (!this.subscriptions.has(joinCode) && this.client?.connected) {
      const subscription = this.client.subscribe(
        `/topic/room/${joinCode}`,
        (message: IMessage) => {
          const chatMessage: ChatMessage = JSON.parse(message.body);
          subject.next(chatMessage);
        }
      );
      this.subscriptions.set(joinCode, subscription);

      // Send join notification
      this.client.publish({
        destination: `/app/chat/${joinCode}/join`,
        body: '{}',
      });
    }

    return subject.asObservable();
  }

  leaveRoom(joinCode: string): void {
    // Send leave notification
    if (this.client?.connected) {
      this.client.publish({
        destination: `/app/chat/${joinCode}/leave`,
        body: '{}',
      });
    }

    const subscription = this.subscriptions.get(joinCode);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(joinCode);
    }

    const subject = this.messageSubjects.get(joinCode);
    if (subject) {
      subject.complete();
      this.messageSubjects.delete(joinCode);
    }
  }

  sendMessage(joinCode: string, content: string): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected');
      return;
    }

    const request: SendMessageRequest = { content };
    this.client.publish({
      destination: `/app/chat/${joinCode}/send`,
      body: JSON.stringify(request),
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
