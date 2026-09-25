import Modal from '../common/Modal';
import { FaTrashAlt } from 'react-icons/fa';

interface Props {
  open: boolean;
  title: string;
  onClose: () => void;
  onConfirm: () => void;
  submitting?: boolean;
}

export default function DeleteConfirmModal({ open, title, onClose, onConfirm, submitting }: Props) {
  return (
    <Modal open={open} title="Delete document" onClose={onClose}>
      <div className="space-y-5">
        <div className="flex items-start gap-3">
          <div className="p-3 rounded-full bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400">
            <FaTrashAlt />
          </div>
          <p className="text-gray-600 dark:text-gray-300 text-sm pt-2">
            Are you sure you want to delete <span className="font-semibold">{title}</span>? This action cannot be undone.
          </p>
        </div>
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={submitting}
            className="px-4 py-2 rounded-lg bg-red-600 text-white font-medium hover:bg-red-700 disabled:opacity-60 transition-colors text-sm"
          >
            {submitting ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </Modal>
  );
}