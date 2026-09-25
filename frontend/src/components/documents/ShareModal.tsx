import { useEffect, useState, type FormEvent } from 'react';
import { FaTimes, FaUserPlus } from 'react-icons/fa';
import Modal from '../common/Modal';
import { listCollaborators, share, changeRole, revoke } from '../../api/permissions';
import { searchByUsername } from '../../api/users';
import { ApiError, formatApiError } from '../../api/client';
import type { Collaborator, Role, User } from '../../types/api';

interface Props {
  docId: string;
  open: boolean;
  onClose: () => void;
}

const ROLES: Role[] = ['VIEWER', 'EDITOR'];

export default function ShareModal({ docId, open, onClose }: Props) {
  const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<User[]>([]);
  const [selected, setSelected] = useState<User | null>(null);
  const [role, setRole] = useState<Role>('VIEWER');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [loadingCollab, setLoadingCollab] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setQuery('');
    setSelected(null);
    setResults([]);
    setLoadingCollab(true);
    listCollaborators(docId)
      .then(setCollaborators)
      .catch((err) => setError(err instanceof ApiError ? formatApiError(err) : 'Failed to load collaborators'))
      .finally(() => setLoadingCollab(false));
  }, [open, docId]);

  useEffect(() => {
    if (!query.trim() || selected) {
      setResults([]);
      return;
    }
    const timer = setTimeout(() => {
      searchByUsername(query.trim())
        .then(setResults)
        .catch(() => setResults([]));
    }, 250);
    return () => clearTimeout(timer);
  }, [query, selected]);

  const onShare = async (e: FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setError(null);
    setSubmitting(true);
    try {
      const added = await share(docId, selected.username, role);
      setCollaborators((prev) => [...prev, added]);
      setSelected(null);
      setQuery('');
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to share');
    } finally {
      setSubmitting(false);
    }
  };

  const onChangeRole = async (username: string, newRole: Role) => {
    setError(null);
    try {
      const updated = await changeRole(docId, username, newRole);
      setCollaborators((prev) => prev.map((c) => (c.username === username ? updated : c)));
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to update role');
    }
  };

  const onRevoke = async (username: string) => {
    setError(null);
    try {
      await revoke(docId, username);
      setCollaborators((prev) => prev.filter((c) => c.username !== username));
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to revoke access');
    }
  };

  return (
    <Modal open={open} title="Share document" onClose={onClose}>
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm px-4 py-3 border border-red-200 dark:border-red-800">
          {error}
        </div>
      )}

      <form onSubmit={onShare} className="space-y-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Add people</label>
        <div className="relative">
          <input
            type="text"
            value={selected ? selected.username : query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelected(null);
            }}
            placeholder="Search by username..."
            className="w-full rounded-lg border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          {results.length > 0 && !selected && (
            <div className="absolute z-10 mt-1 w-full bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg max-h-48 overflow-y-auto">
              {results.map((u) => (
                <button
                  key={u.id}
                  type="button"
                  onClick={() => {
                    setSelected(u);
                    setResults([]);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-indigo-50 dark:hover:bg-gray-600 text-sm text-gray-700 dark:text-gray-200"
                >
                  {u.username}
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="flex gap-3">
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
            className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r === 'VIEWER' ? 'Viewer (read only)' : 'Editor (can edit)'}
              </option>
            ))}
          </select>
          <button
            type="submit"
            disabled={!selected || submitting}
            className="px-4 py-2.5 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700 disabled:opacity-60 transition-colors text-sm inline-flex items-center gap-2"
          >
            <FaUserPlus />
            Share
          </button>
        </div>
      </form>

      <div className="mt-6">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
          People with access
        </h3>
        {loadingCollab ? (
          <p className="text-sm text-gray-400">Loading...</p>
        ) : collaborators.length === 0 ? (
          <p className="text-sm text-gray-400">No collaborators yet. Only the owner has access.</p>
        ) : (
          <ul className="space-y-2">
            {collaborators.map((c) => (
              <li
                key={c.userId}
                className="flex items-center justify-between gap-2 py-2 px-3 rounded-lg bg-gray-50 dark:bg-gray-700/50"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-8 h-8 rounded-full bg-indigo-400 flex items-center justify-center text-white text-sm flex-shrink-0">
                    {c.username.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-100 truncate">{c.username}</p>
                    <p className="text-xs text-gray-400 truncate">by {c.grantedByUsername}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <select
                    value={c.role}
                    onChange={(e) => onChangeRole(c.username, e.target.value as Role)}
                    className="rounded-md border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r.toLowerCase()}</option>
                    ))}
                  </select>
                  <button
                    onClick={() => onRevoke(c.username)}
                    className="p-1.5 rounded text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 transition-colors"
                    aria-label={`Remove ${c.username}`}
                  >
                    <FaTimes />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
}