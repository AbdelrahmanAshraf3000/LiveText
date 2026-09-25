export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface User {
  id: number;
  username: string;
  email: string;
}

export interface AuthResult {
  user: User;
  accessToken: string;
  refreshToken: string;
}

export type Role = 'OWNER' | 'EDITOR' | 'VIEWER';

export interface DocumentItem {
  id: string;
  title: string;
  ownerId: number;
  ownerUsername: string;
  viewerRole: Role;
  createdAt: string;
  updatedAt: string;
}

export interface Collaborator {
  userId: number;
  username: string;
  role: Role;
  grantedByUsername: string;
  grantedAt: string;
}

export interface VersionItem {
  id: number;
  versionNo: number;
  byteSize: number;
  createdByUsername: string;
  createdAt: string;
}

export interface AssetDto {
  id: string;
  fileName: string;
  mimeType: string;
  byteSize: number;
  url: string;
  createdAt: string;
}