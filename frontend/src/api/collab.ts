import { request } from './client';

export function uploadCollabSnapshot(docId: string, yjsState: string, stateVector?: string): Promise<{ byteSize: number }> {
  return request<{ byteSize: number }>(`/api/documents/${docId}/collab/snapshot`, {
    method: 'POST',
    body: JSON.stringify({ yjsState, stateVector }),
  });
}