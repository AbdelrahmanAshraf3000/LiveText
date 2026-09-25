import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaBolt, FaSignOutAlt, FaPlus, FaFolder, FaUsers } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import { listOwned, listShared, deleteDocument } from '../api/documents';
import { ApiError, formatApiError } from '../api/client';
import type { DocumentItem } from '../types/api';
import DocumentCard from '../components/documents/DocumentCard';
import CreateDocumentModal from '../components/documents/CreateDocumentModal';
import RenameModal from '../components/documents/RenameModal';
import DeleteConfirmModal from '../components/documents/DeleteConfirmModal';
import ShareModal from '../components/documents/ShareModal';

type Tab = 'owned' | 'shared';

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [tab, setTab] = useState<Tab>('owned');
  const [owned, setOwned] = useState<DocumentItem[]>([]);
  const [shared, setShared] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showCreate, setShowCreate] = useState(false);
  const [renameTarget, setRenameTarget] = useState<DocumentItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DocumentItem | null>(null);
  const [shareTarget, setShareTarget] = useState<DocumentItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [own, shr] = await Promise.all([listOwned(), listShared()]);
      setOwned(own);
      setShared(shr);
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const onConfirmDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteDocument(deleteTarget.id);
      setDeleteTarget(null);
      await refresh();
    } catch (err) {
      setError(err instanceof ApiError ? formatApiError(err) : 'Failed to delete document');
    } finally {
      setDeleting(false);
    }
  };

  const docs = tab === 'owned' ? owned : shared;

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-blue-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
      <nav className="flex justify-between items-center px-6 py-4 md:px-12 bg-white/70 dark:bg-gray-800/70 backdrop-blur border-b border-gray-100 dark:border-gray-700">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center">
            <FaBolt className="text-white text-sm" />
          </div>
          <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-600 to-purple-600">
            LiveText
          </span>
        </div>
        <div className="flex items-center space-x-4">
          <div className="w-9 h-9 rounded-full bg-indigo-500 flex items-center justify-center text-white font-medium" title={user?.email}>
            {user?.username.charAt(0).toUpperCase()}
          </div>
          <button
            onClick={onLogout}
            className="inline-flex items-center space-x-2 px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
          >
            <FaSignOutAlt />
            <span>Logout</span>
          </button>
        </div>
      </nav>

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-800 dark:text-gray-100">
              Welcome, <span className="text-indigo-600 dark:text-indigo-400">{user?.username}</span>
            </h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">Manage your documents and collaborations.</p>
          </div>
          <button
            onClick={() => setShowCreate(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700 transition-colors text-sm"
          >
            <FaPlus /> New
          </button>
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm px-4 py-3 border border-red-200 dark:border-red-800">
            {error}
          </div>
        )}

        <div className="flex gap-2 mb-6 border-b border-gray-200 dark:border-gray-700">
          <button
            onClick={() => setTab('owned')}
            className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              tab === 'owned'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <FaFolder /> Your documents
            <span className="ml-1 text-xs px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-700">{owned.length}</span>
          </button>
          <button
            onClick={() => setTab('shared')}
            className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              tab === 'shared'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <FaUsers /> Shared with you
            <span className="ml-1 text-xs px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-700">{shared.length}</span>
          </button>
        </div>

        {loading ? (
          <p className="text-gray-400 text-sm">Loading documents...</p>
        ) : docs.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <p className="text-sm">
              {tab === 'owned' ? 'You have no documents yet. Create one to get started.' : 'No documents have been shared with you.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {docs.map((doc) => (
              <DocumentCard
                key={doc.id}
                doc={doc}
                onRename={setRenameTarget}
                onDelete={setDeleteTarget}
                onShare={setShareTarget}
              />
            ))}
          </div>
        )}
      </main>

      <CreateDocumentModal open={showCreate} onClose={() => setShowCreate(false)} onCreated={refresh} />
      {renameTarget && (
        <RenameModal
          docId={renameTarget.id}
          currentTitle={renameTarget.title}
          open={!!renameTarget}
          onClose={() => setRenameTarget(null)}
          onRenamed={refresh}
        />
      )}
      <DeleteConfirmModal
        open={!!deleteTarget}
        title={deleteTarget?.title ?? ''}
        onClose={() => setDeleteTarget(null)}
        onConfirm={onConfirmDelete}
        submitting={deleting}
      />
      {shareTarget && (
        <ShareModal docId={shareTarget.id} open={!!shareTarget} onClose={() => setShareTarget(null)} />
      )}
    </div>
  );
}