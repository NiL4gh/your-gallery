import React, { useRef, useState } from 'react';
import { Album } from '../types/gallery';
import { CloseFillIcon, FolderSyncFillIcon, PhotosFillIcon } from './FilledIcons';

interface ImportModalProps {
  albums: Album[];
  supportsFolderSync: boolean;
  linkedFolderName: string | null;
  onClose: () => void;
  onImportFiles: (files: File[], targetAlbumId: string) => Promise<void>;
  onSyncFolder: (targetAlbumId: string) => Promise<void>;
}

export const ImportModal: React.FC<ImportModalProps> = ({
  albums,
  supportsFolderSync,
  linkedFolderName,
  onClose,
  onImportFiles,
  onSyncFolder,
}) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [targetAlbumId, setTargetAlbumId] = useState(albums[0]?.id || 'camera');
  const [busy, setBusy] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  const handleFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    setBusy(true);
    try {
      await onImportFiles(Array.from(fileList), targetAlbumId);
      onClose();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 p-6 shadow-2xl space-y-5"
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-zinc-950 dark:text-white">
              Sync Phone Photos
            </h2>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              Auto-populates from your Motorola Edge 70 storage
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close import modal"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center text-zinc-500 hover:text-zinc-900 dark:hover:text-white"
          >
            <CloseFillIcon className="w-6 h-6" />
          </button>
        </div>

        {supportsFolderSync && (
          <div className="rounded-2xl bg-[var(--accent-soft)] border border-[var(--accent)]/30 p-4 space-y-2.5">
            <div className="flex items-center gap-2 text-zinc-950 dark:text-white">
              <FolderSyncFillIcon className="w-5 h-5 text-[var(--accent)]" />
              <span className="text-sm font-bold">
                {linkedFolderName ? `Auto-Sync Linked (${linkedFolderName})` : 'Auto-Populate Folder'}
              </span>
            </div>
            <p className="text-xs text-zinc-600 dark:text-zinc-300 leading-relaxed">
              Select your phone&apos;s <strong>DCIM</strong> or <strong>Pictures</strong> folder once.
              Your Gallery remembers it and auto-populates new shots automatically.
            </p>
            <button
              type="button"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  await onSyncFolder(targetAlbumId);
                  onClose();
                } finally {
                  setBusy(false);
                }
              }}
              className="w-full min-h-[44px] rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white text-sm font-medium flex items-center justify-center gap-2 transition"
            >
              <FolderSyncFillIcon className="w-4 h-4" />
              <span>{linkedFolderName ? 'Change or Rescan Folder' : 'Link Phone Folder Once'}</span>
            </button>
          </div>
        )}

        <div>
          <label className="block text-xs font-medium text-zinc-600 dark:text-zinc-400 mb-1.5">
            Save to Album
          </label>
          <select
            value={targetAlbumId}
            onChange={(e) => setTargetAlbumId(e.target.value)}
            className="w-full min-h-[44px] px-3.5 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 text-sm text-zinc-950 dark:text-white focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
          >
            {albums.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
        </div>

        <div
          onDragOver={(e) => {
            e.preventDefault();
            setDragActive(true);
          }}
          onDragLeave={() => setDragActive(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragActive(false);
            handleFiles(e.dataTransfer.files);
          }}
          className={`rounded-2xl border-2 border-dashed p-5 text-center transition ${
            dragActive
              ? 'border-[var(--accent)] bg-[var(--accent-soft)]'
              : 'border-zinc-200 dark:border-zinc-800 bg-zinc-50/60 dark:bg-zinc-900/40'
          }`}
        >
          <PhotosFillIcon className="w-7 h-7 mx-auto text-[var(--accent)] mb-2" />
          <p className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
            Or pick individual photos & videos manually
          </p>

          <input
            ref={inputRef}
            type="file"
            multiple
            accept="image/*,video/*"
            onChange={(e) => handleFiles(e.target.files)}
            className="hidden"
          />

          <button
            type="button"
            disabled={busy}
            onClick={() => inputRef.current?.click()}
            className="mt-3 w-full min-h-[42px] rounded-xl border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-100 dark:hover:bg-zinc-800 disabled:opacity-50 text-zinc-900 dark:text-white text-xs font-medium transition"
          >
            {busy ? 'Importing...' : 'Browse Files'}
          </button>
        </div>
      </div>
    </div>
  );
};
