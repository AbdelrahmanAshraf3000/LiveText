import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FaBolt, FaArrowLeft, FaUsers, FaTable, FaImage } from 'react-icons/fa';
import * as Y from 'yjs';
import { WebsocketProvider } from 'y-websocket';
import { QuillBinding } from 'y-quill';
import Quill from 'quill';
import QuillCursors from 'quill-cursors';
import 'quill/dist/quill.snow.css';

import { openDocument } from '../api/documents';
import { getAccessToken } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ApiError, formatApiError } from '../api/client';
import { uploadImage, setAccessTokenGetter } from '../api/assets';
import { uploadCollabSnapshot } from '../api/collab';
import type { DocumentItem } from '../types/api';
import VersionPanel from '../components/documents/VersionPanel';

// Wire the token getter for the assets API module
setAccessTokenGetter(getAccessToken);

// Helper: encode a byte array as base64 (for snapshot uploads)
function bytesToBase64(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

// Register the cursors module for collaborative cursor display
Quill.register('modules/cursors', QuillCursors);

interface PresenceUser {
 clientId: number;
 name: string;
 color: string;
}

function createUniqueCursorColor(existingColors: Iterable<string>): string {
 const usedColors = new Set(Array.from(existingColors, (color) => color.toLowerCase()));
 const randomValues = new Uint32Array(1);

 for (let attempt = 0; attempt < 100; attempt++) {
   crypto.getRandomValues(randomValues);
   const color = `#${(randomValues[0] & 0xffffff).toString(16).padStart(6, '0')}`;
   if (!usedColors.has(color)) return color;
 }

 // A collision after 100 attempts is exceptionally unlikely, but this keeps
 // the uniqueness guarantee if many users are already connected.
 for (let value = 0; value <= 0xffffff; value++) {
   const color = `#${value.toString(16).padStart(6, '0')}`;
   if (!usedColors.has(color)) return color;
 }

 return '#000000';
}

export default function Editor() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [connectedUsers, setConnectedUsers] = useState<PresenceUser[]>([]);
  const [connStatus, setConnStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');

  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<Quill | null>(null);
  const providerRef = useRef<WebsocketProvider | null>(null);
  const ydocRef = useRef<Y.Doc | null>(null);
  const bindingRef = useRef<QuillBinding | null>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const isViewer = doc?.viewerRole === 'VIEWER';


  // Produce a base64-encoded Yjs snapshot from the live Y.Doc — used by the
  // VersionPanel "Save snapshot" button (the client computes the full state since
  // the server has no Yjs engine in Architecture B).
  const produceSnapshot = (): { yjsState: string; stateVector: string } | null => {
    if (!ydocRef.current) return null;
    try {
      const stateBytes = Y.encodeStateAsUpdate(ydocRef.current);
      const vectorBytes = Y.encodeStateVector(ydocRef.current);
      return { yjsState: bytesToBase64(stateBytes), stateVector: bytesToBase64(vectorBytes) };
    } catch {
      return null;
    }
  };

  const handleImageUpload = async (file: File) => {
    if (!id || !quillRef.current) return;

    const range = quillRef.current.getSelection(true);
    if (!range) return;

    try {
      const result = await uploadImage(id, file);
      quillRef.current.insertEmbed(range.index, 'image', result.url, 'user');
      quillRef.current.setSelection(range.index + 1, 0, 'user');
    } catch (err) {
      console.error('Image upload failed:', err);
      setError(err instanceof Error ? err.message : 'Image upload failed');
    } finally {
      if (imageInputRef.current) {
        imageInputRef.current.value = '';
      }
    }
  };

  const insertTable = () => {
    const quill = quillRef.current;
    if (!quill || isViewer) return;

    const rows = Number.parseInt(window.prompt('Number of rows', '3') ?? '', 10);
    const columns = Number.parseInt(window.prompt('Number of columns', '3') ?? '', 10);
    if (
      !Number.isInteger(rows) ||
      !Number.isInteger(columns) ||
      rows < 1 ||
      columns < 1 ||
      rows > 20 ||
      columns > 20
    ) {
      window.alert('Tables must have between 1 and 20 rows and columns.');
      return;
    }

    const table = quill.getModule('table') as { insertTable: (rows: number, columns: number) => void };
    table.insertTable(rows, columns);
  };

  // Load document metadata
  useEffect(() => {
    if (!id) return;
    openDocument(id)
      .then(setDoc)
      .catch((err) => setError(err instanceof ApiError ? formatApiError(err) : 'Failed to open document'))
      .finally(() => setLoading(false));
  }, [id]);

  // Set up collaborative editor
  useEffect(() => {
    if (!id || !doc || !editorRef.current) return;

    const isViewer = doc.viewerRole === 'VIEWER';

    // Create Yjs document
    const ydoc = new Y.Doc();
    ydocRef.current = ydoc;
    const ytext = ydoc.getText('quill');

    // Create Quill editor
    const quill = new Quill(editorRef.current, {
      theme: 'snow',
      readOnly: isViewer,
      modules: {
        cursors: true,
        table: true,
        toolbar: isViewer ? false : {
          container: [
            [{ header: [1, 2, 3, false] }],
            ['bold', 'italic', 'underline', 'strike'],
            [{ list: 'ordered' }, { list: 'bullet' }],
            [{ script: 'sub' }, { script: 'super' }],
            ['blockquote', 'code-block'],
            ['link', 'image'],
            ['clean'],
          ],
          handlers: {
            image: () => imageInputRef.current?.click(),
          },
        },
      },
      placeholder: isViewer ? 'Document is read-only' : 'Start writing...',
    });
    quillRef.current = quill;

    // Connect to collaboration server
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/collab`;

    const provider = new WebsocketProvider(wsUrl, id, ydoc, {
      params: { token: getAccessToken() || '' },
    });
    providerRef.current = provider;

    setConnStatus('connecting');
    provider.on('status', (event: { status: string }) => {
      setConnStatus(event.status === 'connected' ? 'connected' : 'disconnected');
    });

    // Set awareness user info (name + color for cursor)
    const existingCursorColors = Array.from(provider.awareness.getStates().values())
      .map((state) => state.user?.color)
      .filter((color): color is string => typeof color === 'string');
    const color = createUniqueCursorColor(existingCursorColors);
    provider.awareness.setLocalStateField('user', {
      name: user?.username || 'Anonymous',
      color: color,
    });

    // Track connected users via awareness
    const updatePresence = () => {
      const states = provider.awareness.getStates();
      const users: PresenceUser[] = [];
      states.forEach((state, clientId) => {
        if (state.user) {
          users.push({
            clientId,
            name: state.user.name || `User ${clientId}`,
            color: state.user.color || '#888',
          });
        }
      });
      setConnectedUsers(users);
    };
    provider.awareness.on('change', updatePresence);
    updatePresence();

    // Bind Quill to Yjs
    const binding = new QuillBinding(ytext, quill, provider.awareness);
    bindingRef.current = binding;

    // Leader snapshot timer: periodically upload the full Yjs doc state to the server so
    // it can prune the update log and serve fresh snapshots to reconnecting clients.
    // Only editors/owners do this (viewers can't write anyway). The timer fires every 30s
    // after a 30s initial delay. Any connected editor is a valid leader — last write wins.
    let snapshotTimer: ReturnType<typeof setInterval> | null = null;
    if (!isViewer) {
      snapshotTimer = setInterval(() => {
        if (!ydocRef.current) return;
        try {
          const stateBytes = Y.encodeStateAsUpdate(ydocRef.current);
          const vectorBytes = Y.encodeStateVector(ydocRef.current);
          uploadCollabSnapshot(id!, bytesToBase64(stateBytes), bytesToBase64(vectorBytes))
            .catch((e) => console.warn('Snapshot upload failed:', e));
        } catch (e) {
          console.warn('Snapshot computation failed:', e);
        }
      }, 30000);
    }

    // Cleanup on unmount
    return () => {
      updatePresence();
      if (snapshotTimer) clearInterval(snapshotTimer);
      binding.destroy();
      provider.destroy();
      ydoc.destroy();
      quillRef.current = null;
      providerRef.current = null;
      ydocRef.current = null;
      bindingRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, doc]);

  const onLogout = async () => {
    // Method intentionally minimal – proxy through auth context not needed here
    navigate('/dashboard');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-500">
        Loading document...
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <nav className="flex items-center px-6 py-3 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700">
          <button
            onClick={() => navigate('/dashboard')}
            className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-indigo-600"
          >
            <FaArrowLeft /> Back to dashboard
          </button>
        </nav>
        <div className="max-w-3xl mx-auto py-16 px-6">
          <div className="rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 px-4 py-3 border border-red-200 dark:border-red-800">
            {error}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col">
      {/* Top navbar */}
      <nav className="flex items-center justify-between px-6 py-3 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700 flex-shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            title="Back to dashboard"
          >
            <FaArrowLeft />
          </button>
          <div className="flex items-center space-x-2">
            <div className="w-7 h-7 rounded-full bg-indigo-600 flex items-center justify-center">
              <FaBolt className="text-white text-xs" />
            </div>
            <span className="font-semibold text-gray-800 dark:text-gray-100">
              {doc?.title ?? 'Untitled'}
            </span>
            {isViewer && (
              <span className="text-xs px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-500">
                Viewer
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-4">
          {/* Presence bar */}
          <div className="flex items-center gap-2">
            <FaUsers className="text-gray-400 text-sm" />
            <div className="flex -space-x-2">
              {connectedUsers.slice(0, 6).map((u) => (
                <div
                  key={u.clientId}
                  className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-medium border-2 border-white dark:border-gray-800"
                  style={{ backgroundColor: u.color }}
                  title={u.name}
                >
                  {u.name.charAt(0).toUpperCase()}
                </div>
              ))}
            </div>
            {connectedUsers.length > 6 && (
              <span className="text-xs text-gray-400">
                +{connectedUsers.length - 6}
              </span>
            )}
          </div>

          {/* Connection status */}
          <span className={`text-xs px-2 py-1 rounded-full ${
            connStatus === 'connected'
              ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
              : connStatus === 'connecting'
              ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
              : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
          }`}>
            {connStatus}
          </span>

          {/* Version history */}
          <VersionPanel
            docId={id!}
            canEdit={!isViewer}
            onRollback={() => {
              // Force reconnect by reloading the page after rollback
              setTimeout(() => window.location.reload(), 500);
            }}
            produceSnapshot={produceSnapshot}
          />

          <button
            onClick={onLogout}
            className="text-sm text-gray-500 hover:text-indigo-600"
          >
            {user?.username}
          </button>
        </div>
      </nav>

      {/* Quill editor container */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-4xl mx-auto py-8 px-6">
          {!isViewer && (
            <div className="mb-3 flex items-center gap-2">
              <input
                ref={imageInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) void handleImageUpload(file);
                }}
              />
              <button
                type="button"
                onClick={() => imageInputRef.current?.click()}
                className="inline-flex items-center gap-2 rounded-md bg-white px-3 py-2 text-sm text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 dark:bg-gray-800 dark:text-gray-200 dark:ring-gray-600 dark:hover:bg-gray-700"
              >
                <FaImage aria-hidden="true" />
                Insert image
              </button>
              <button
                type="button"
                onClick={insertTable}
                className="inline-flex items-center gap-2 rounded-md bg-white px-3 py-2 text-sm text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 dark:bg-gray-800 dark:text-gray-200 dark:ring-gray-600 dark:hover:bg-gray-700"
              >
                <FaTable aria-hidden="true" />
                Insert table
              </button>
            </div>
          )}
          <div
            ref={editorRef}
            className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-100 dark:border-gray-700 min-h-[600px]"
          />
        </div>
      </div>
    </div>
  );
}