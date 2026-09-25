import { request } from './client';
import type { Collaborator, Role } from '../types/api';

export function listCollaborators(docId: string): Promise<Collaborator[]> {
  return request<Collaborator[]>(`/api/documents/${docId}/permissions`);
}

export function share(docId: string, username: string, role: Role): Promise<Collaborator> {
  return request<Collaborator>(`/api/documents/${docId}/permissions`, {
    method: 'POST',
    body: JSON.stringify({ username, role }),
  });
}

export function changeRole(docId: string, username: string, role: Role): Promise<Collaborator> {
  return request<Collaborator>(`/api/documents/${docId}/permissions`, {
    method: 'PATCH',
    body: JSON.stringify({ username, role }),
  });
}

export function revoke(docId: string, username: string): Promise<void> {
  return request<void>(`/api/documents/${docId}/permissions?username=${encodeURIComponent(username)}`, {
    method: 'DELETE',
  });
}