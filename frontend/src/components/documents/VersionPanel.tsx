import { useEffect, useState } from 'react';
import { FaHistory, FaUndo, FaSave, FaTimes, FaClock } from 'react-icons/fa';
import { listVersions, createSnapshot, rollback } from '../../api/versions';
import type { VersionItem } from '../../types/api';
import { ApiError, formatApiError } from '../../api/client';

interface Props {
  docId: string;
  canEdit: boolean;
  onRollback: () => void;
  /** Produces the current Yjs doc state as base64 for snapshot upload (client computes it). */
  produceSnapshot: () => { yjsState: string; stateVector: string } | null;
}

export default function VersionPanel({ docId, canEdit, onRollback, produceSnapshot }: Props) {
  const [open, setOpen] = useState(false);
  const [versions, setVersions] = useState<VersionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [committing, setCommitting] = useState(false);
  const [confirmVersion, setConfirmVersion] = useState<VersionItem | null>(null);

  const refresh = () => {
    setLoading(true);
    setError(null);
    listVersions(docId)
      .then(setVersions)
      .catch((err) => setError(err instanceof ApiError ? formatApiError(err) : 'Failed to load versions'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (open) refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, docId]);

  const onCreateSnapshot = async () => {
    setError(null);
    setCommitting(true);
    try {
      const snapshot = produceSnapshot();
      if (!snapshot) {
        setError('Document not ready yet');
        return;
      }
      await createSnapshot(docId, snapshot.yjsState, snapshot.stateVector);
      refresh();
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to save snapshot');
    } finally {
      setCommitting(false);
    }
  };

  const onConfirmRollback = async () => {
    if (!confirmVersion) return;
    setError(null);
    setCommitting(true);
    try {
      await rollback(docId, confirmVersion.versionNo);
      setConfirmVersion(null);
      onRollback();
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to rollback');
    } finally {
      setCommitting(false);
    }
  };

  const formatDate = (iso: string) => {
    return new Date(iso).toLocaleString(undefined, {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  };

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-indigo-600 transition-colors"
        title="Version history"
      >
        <FaHistory />
      </button>

      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 w-full max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
              <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                <FaHistory className="text-indigo-500" /> Version history
              </h2>
              <button
                onClick={() => setOpen(false)}
                className="p-2 rounded-lg text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              >
                <FaTimes />
              </button>
            </div>

            <div className="px-6 py-3 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
              <p className="text-sm text-gray-500">{versions.length} version(s) saved</p>
              {canEdit && (
                <button
                  onClick={onCreateSnapshot}
                  disabled={committing}
                  className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 disabled:opacity-60 transition-colors"
                >
                  <FaSave /> {committing ? 'Saving...' : 'Save snapshot'}
                </button>
              )}
            </div>

            <div className="overflow-y-auto flex-1 px-6 py-4">
              {error && (
                <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm px-4 py-3 border border-red-200 dark:border-red-800">
                  {error}
                </div>
              )}
              {loading ? (
                <p className="text-sm text-gray-400">Loading...</p>
              ) : versions.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">
                  No versions saved yet. Click "Save snapshot" to create one.
                </p>
              ) : (
                <ul className="space-y-2">
                  {versions.map((v, idx) => (
                    <li
                      key={v.id}
                      className="flex items-center justify-between gap-3 py-3 px-4 rounded-lg bg-gray-50 dark:bg-gray-700/50"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <FaClock className="text-gray-400 flex-shrink-0" />
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-800 dark:text-gray-100">
                            Version {v.versionNo}
                            {idx === 0 && (
                              <span className="ml-2 text-xs px-1.5 py-0.5 rounded bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                                Latest
                              </span>
                            )}
                          </p>
                          <p className="text-xs text-gray-400">
                            {formatDate(v.createdAt)} &middot; by {v.createdByUsername}
                          </p>
                        </div>
                      </div>
                      {canEdit && idx !== 0 && (
                        <button
                          onClick={() => setConfirmVersion(v)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 hover:text-indigo-600 transition-colors text-sm"
                        >
                          <FaUndo /> Rollback
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {confirmVersion && (
              <div className="px-6 py-4 border-t border-gray-100 dark:border-gray-700 bg-yellow-50 dark:bg-yellow-900/20">
                <p className="text-sm text-gray-700 dark:text-gray-200 mb-3">
                  Rollback to version <strong>{confirmVersion.versionNo}</strong>?
                  All collaborators will be disconnected and re-synced. The rollback will be saved as a new version.
                </p>
                <div className="flex justify-end gap-3">
                  <button
                    onClick={() => setConfirmVersion(null)}
                    className="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 text-sm"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={onConfirmRollback}
                    disabled={committing}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700 disabled:opacity-60 text-sm"
                  >
                    <FaUndo /> {committing ? 'Rolling back...' : 'Confirm rollback'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}