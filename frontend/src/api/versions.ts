import { request } from './client';
import type { VersionItem } from '../types/api';

export function listVersions(docId: string): Promise<VersionItem[]> {
  return request<VersionItem[]>(`/api/documents/${docId}/versions`);
}

export function createSnapshot(docId: string, yjsState: string, stateVector?: string): Promise<VersionItem> {
  return request<VersionItem>(`/api/documents/${docId}/versions`, {
    method: 'POST',
    body: JSON.stringify({ yjsState, stateVector }),
  });
}

export function rollback(docId: string, versionNo: number): Promise<VersionItem> {
  return request<VersionItem>(`/api/documents/${docId}/versions/${versionNo}/rollback`, {
    method: 'POST',
  });
}