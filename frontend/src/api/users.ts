import { request } from './client';
import type { User } from '../types/api';

export function me(): Promise<User> {
  return request<User>('/api/users/me');
}

export function searchByUsername(username: string): Promise<User[]> {
  return request<User[]>(`/api/users?username=${encodeURIComponent(username)}`);
}