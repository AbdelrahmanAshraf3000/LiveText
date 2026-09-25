import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { User } from '../types/api';
import { setAccessToken, setOnAuthFailed } from '../api/client';
import * as authApi from '../api/auth';
import { me as fetchMe } from '../api/users';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setOnAuthFailed(() => setUser(null));

    (async () => {
      try {
        const res = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
        });
        if (res.ok) {
          const body = await res.json();
          setAccessToken(body.data.accessToken);
          const u = await fetchMe();
          setUser(u);
        }
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const login = async (identifier: string, password: string) => {
    const result = await authApi.login(identifier, password);
    setAccessToken(result.accessToken);
    setUser(result.user);
  };

  const register = async (username: string, email: string, password: string) => {
    const result = await authApi.register(username, email, password);
    setAccessToken(result.accessToken);
    setUser(result.user);
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore network errors on logout
    }
    setAccessToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}