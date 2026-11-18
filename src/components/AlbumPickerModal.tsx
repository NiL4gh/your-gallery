import React, { useState } from 'react';
import { Album } from '../types/gallery';
import { CheckCircleFillIcon, CloseFillIcon, FolderPlusFillIcon } from './FilledIcons';

interface AlbumPickerModalProps {
  albums: Album[];
  itemCount: number;
  currentAlbumIds: string[];
  onToggleAlbum: (albumId: string) => void;
  onCreateAndAdd: (name: string) => void;
  onClose: () => void;
}

export const AlbumPickerModal: React.FC<AlbumPickerModalProps> = ({
  albums,
  itemCount,
  currentAlbumIds,
  onToggleAlbum,
  onCreateAndAdd,
  onClose,
}) => {
  const [newName, setNewName] = useState('');

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newName.trim();
    if (!trimmed) return;
    onCreateAndAdd(trimmed);
    setNewName('');
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-t-3xl sm:rounded-3xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 p-5 shadow-2xl space-y-4"
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-zinc-950 dark:text-white">
              Add to Album
            </h2>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 font-mono-exif">
              {itemCount} {itemCount === 1 ? 'item' : 'items'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close album picker"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center text-zinc-500 hover:text-zinc-900 dark:hover:text-white"
          >
            <CloseFillIcon className="w-6 h-6" />
          </button>
        </div>

        <div className="max-h-60 overflow-y-auto divide-y divide-zinc-100 dark:divide-zinc-900">
          {albums.map((album) => {
            const included = currentAlbumIds.includes(album.id);
            return (
              <button
                key={album.id}
                type="button"
                onClick={() => onToggleAlbum(album.id)}
                className="w-full min-h-[48px] px-2 flex items-center justify-between text-left hover:bg-zinc-50 dark:hover:bg-zinc-900 rounded-xl transition"
              >
                <span className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
                  {album.name}
                </span>
                {included ? (
                  <CheckCircleFillIcon className="w-5 h-5 text-[var(--accent)]" />
                ) : (
                  <span className="w-5 h-5 rounded-full border-2 border-zinc-300 dark:border-zinc-700" />
                )}
              </button>
            );
          })}
        </div>

        <form onSubmit={handleCreate} className="flex gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-900">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="New album name..."
            className="flex-1 min-h-[40px] px-3 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 text-xs text-zinc-950 dark:text-white focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
          />
          <button
            type="submit"
            disabled={!newName.trim()}
            className="min-h-[40px] px-3.5 rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] disabled:opacity-40 text-white text-xs font-medium flex items-center gap-1"
          >
            <FolderPlusFillIcon className="w-4 h-4" />
            <span>Create</span>
          </button>
        </form>
      </div>
    </div>
  );
};
