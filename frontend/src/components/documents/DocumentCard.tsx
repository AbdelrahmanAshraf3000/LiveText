import { useNavigate } from 'react-router-dom';
import { FaFileAlt, FaPencilAlt, FaTrashAlt, FaShareAlt, FaLock } from 'react-icons/fa';
import type { DocumentItem } from '../../types/api';

interface Props {
  doc: DocumentItem;
  onRename: (doc: DocumentItem) => void;
  onDelete: (doc: DocumentItem) => void;
  onShare: (doc: DocumentItem) => void;
}

export default function DocumentCard({ doc, onRename, onDelete, onShare }: Props) {
  const navigate = useNavigate();
  const canEdit = doc.viewerRole === 'OWNER' || doc.viewerRole === 'EDITOR';
  const canShare = doc.viewerRole === 'OWNER' || doc.viewerRole === 'EDITOR';
  const canDelete = doc.viewerRole === 'OWNER';

  return (
    <div className="group bg-white dark:bg-gray-800 rounded-xl border border-gray-100 dark:border-gray-700 p-5 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <button
          onClick={() => navigate(`/editor/${doc.id}`)}
          className="flex items-center gap-3 min-w-0 text-left"
        >
          <div className="p-2.5 rounded-lg bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex-shrink-0">
            <FaFileAlt />
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
              {doc.title}
            </h3>
            <p className="text-xs text-gray-400 mt-0.5">
              {doc.viewerRole === 'OWNER' ? 'Owned by you' : `Shared by ${doc.ownerUsername}`}
            </p>
          </div>
        </button>
        {!canEdit && (
          <span className="inline-flex items-center gap-1 text-xs text-gray-400 px-2 py-1 rounded-md bg-gray-100 dark:bg-gray-700" title="View only">
            <FaLock /> Viewer
          </span>
        )}
      </div>

      <div className="flex items-center gap-1 mt-4 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={() => navigate(`/editor/${doc.id}`)}
          className="px-3 py-1.5 text-sm rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
        >
          Open
        </button>
        {canEdit && (
          <button
            onClick={() => onRename(doc)}
            className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-indigo-600 transition-colors"
            title="Rename"
          >
            <FaPencilAlt />
          </button>
        )}
        {canShare && (
          <button
            onClick={() => onShare(doc)}
            className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-indigo-600 transition-colors"
            title="Share"
          >
            <FaShareAlt />
          </button>
        )}
        {canDelete && (
          <button
            onClick={() => onDelete(doc)}
            className="p-2 rounded-lg text-gray-500 hover:bg-red-50 dark:hover:bg-red-900/30 hover:text-red-600 transition-colors"
            title="Delete"
          >
            <FaTrashAlt />
          </button>
        )}
      </div>
    </div>
  );
}