import type { AssetDto } from '../types/api';

export interface UploadResult {
  id: string;
  url: string;
}

export function uploadImage(documentId: string, file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('documentId', documentId);
  return fetch('/api/assets', {
    method: 'POST',
    body: formData,
    credentials: 'include',
    headers: {
      Authorization: `Bearer ${getAccessToken()}`,
    },
  }).then(async (res) => {
    const text = await res.text();
    const body = text ? JSON.parse(text) : null;
    if (!res.ok || !body?.success) {
      throw new Error(body?.message || 'Upload failed');
    }
    return { id: body.data.id, url: body.data.url };
  });
}

// avoid circular dep — import the token getter lazily
let getAccessToken: () => string | null = () => null;
export function setAccessTokenGetter(fn: () => string | null) {
  getAccessToken = fn;
}

export type { AssetDto };