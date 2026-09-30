import type { ApiResponse,AssetDto,UploadResult } from '../types/api';



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
    type UploadResponse = ApiResponse<UploadResult>;
    let body: UploadResponse | null = null;
    if (text) {
      try {
        body = JSON.parse(text) as UploadResponse;
      } catch {
        const message = res.status === 413
          ? 'Image upload is too large (maximum 5MB)'
          : `Image upload failed (HTTP ${res.status})`;
        throw new Error(message);
      }
    }
    if (!res.ok || !body?.success || !body.data?.id || !body.data.url) {
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