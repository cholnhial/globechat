import { User } from './user.model';

export type MessageType =
  | 'CHAT'
  | 'JOIN'
  | 'LEAVE'
  | 'KICK'
  | 'BAN'
  | 'MOODSIC_CHANGE'
  | 'MOODSIC_TOGGLE'
  | 'ROOM_DESTROYED'
  | 'SYSTEM';

export interface ChatMessage {
  id: number | null;
  content: string;
  type: MessageType;
  sender: User | null;
  roomJoinCode: string;
  createdAt: string;
  targetUsername?: string | null;
}

export interface SendMessageRequest {
  content: string;
}
