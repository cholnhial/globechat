import { User } from './user.model';
import { Moodsic } from './moodsic.model';

export interface Room {
  id: number;
  joinCode: string;
  title: string;
  description: string;
  rules: string;
  owner: User;
  currentMoodsic: Moodsic | null;
  moodsicPaused: boolean;
  latitude: number;
  longitude: number;
  memberCount: number;
  createdAt: string;
}

export interface RoomMarker {
  joinCode: string;
  title: string;
  latitude: number;
  longitude: number;
  memberCount: number;
}

export interface RoomMember {
  user: User;
  role: MemberRole;
  joinedAt: string;
}

export type MemberRole = 'OWNER' | 'MOD' | 'CHATTER';

export interface CreateRoomRequest {
  title: string;
  description?: string;
  rules?: string;
  latitude: number;
  longitude: number;
}

export interface UpdateRoomRequest {
  title?: string;
  description?: string;
  rules?: string;
}

export interface BanRequest {
  username: string;
  reason?: string;
}

export interface SetMoodsicRequest {
  moodsicId: number;
}
