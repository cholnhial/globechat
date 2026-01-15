import { User } from './user.model';

export interface Moodsic {
  id: number;
  name: string;
  contentType: string;
  isPublic: boolean;
  playCount: number;
  uploadedBy: User;
  createdAt: string;
}
