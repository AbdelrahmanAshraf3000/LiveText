import { request } from './client';
import type { DocumentItem } from '../types/api';

export function listOwned(): Promise<DocumentItem[]> {
  return request<DocumentItem[]>('/api/documents/owned');
}

export function listShared(): Promise<DocumentItem[]> {
  return request<DocumentItem[]>('/api/documents/shared');
}

export function createDocument(title: string): Promise<DocumentItem> {
  return request<DocumentItem>('/api/documents', {
    method: 'POST',
    body: JSON.stringify({ title }),
  });
}

export function openDocument(id: string): Promise<DocumentItem> {
  return request<DocumentItem>(`/api/documents/${id}`);
}

export function renameDocument(id: string, title: string): Promise<DocumentItem> {
  return request<DocumentItem>(`/api/documents/${id}/rename`, {
    method: 'PATCH',
    body: JSON.stringify({ title }),
  });
}

export function deleteDocument(id: string): Promise<void> {
  return request<void>(`/api/documents/${id}`, { method: 'DELETE' });
}