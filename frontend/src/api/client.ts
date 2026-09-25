import type { ApiResponse, AuthResult } from '../types/api';

let accessToken: string | null = null;
let refreshInFlight: Promise<string | null> | null = null;
let onAuthFailed: (() => void) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function setOnAuthFailed(cb: (() => void) | null): void {
  onAuthFailed = cb;
}

export class ApiError extends Error {
  status: number;
  data: unknown;
  constructor(message: string, status: number, data: unknown = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function doRefresh(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight;
  refreshInFlight = (async () => {
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!res.ok) return null;
      const body = (await res.json()) as ApiResponse<AuthResult>;
      accessToken = body.data.accessToken;
      return accessToken;
    } catch {
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

function buildHeaders(init: HeadersInit | undefined, token: string | null, hasBody: boolean): Headers {
  const headers = new Headers(init);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (hasBody && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  return headers;
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const hasBody = !!options.body;
  const send = (token: string | null) =>
    fetch(path, { ...options, headers: buildHeaders(options.headers, token, hasBody), credentials: 'include' });

  let res = await send(accessToken);

  // If access token expired, try a single silent refresh + retry (skip for auth endpoints themselves)
  if (res.status === 401 && !path.startsWith('/api/auth/')) {
    const newToken = await doRefresh();
    if (newToken) {
      res = await send(newToken);
    }
    if (!res.ok) {
      accessToken = null;
      onAuthFailed?.();
    }
  }

  const text = await res.text();
  const body = text ? (JSON.parse(text) as ApiResponse<T>) : ({ success: res.ok, message: '', data: null as T });
  if (!res.ok) {
    throw new ApiError(body.message || 'Request failed', res.status, body.data);
  }
  return body.data;
}

export function formatApiError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.data && typeof err.data === 'object') {
      const values = Object.values(err.data).filter(Boolean);
      if (values.length) return values.join('; ');
    }
    return err.message;
  }
  return 'Unexpected error';
}