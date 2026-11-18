import React from 'react';
import {
  CloseFillIcon,
  CompareFillIcon,
  DownloadFillIcon,
  FolderPlusFillIcon,
  HeartFillIcon,
  LockFillIcon,
  RestoreFillIcon,
  TrashFillIcon,
} from './FilledIcons';

interface SelectionActionBarProps {
  selectedCount: number;
  isTrashContext?: boolean;
  isLockedContext?: boolean;
  onClear: () => void;
  onFavoriteAll: () => void;
  onAddToAlbum: () => void;
  onDownloadSelected: () => void;
  onToggleLock: () => void;
  onTrashOrDelete: () => void;
  onRestoreSelected?: () => void;
  onCompareSelected?: () => void;
}

export const SelectionActionBar: React.FC<SelectionActionBarProps> = ({
  selectedCount,
  isTrashContext,
  isLockedContext,
  onClear,
  onFavoriteAll,
  onAddToAlbum,
  onDownloadSelected,
  onToggleLock,
  onTrashOrDelete,
  onRestoreSelected,
  onCompareSelected,
}) => {
  if (selectedCount === 0) return null;

  return (
    <div className="fixed bottom-[64px] md:bottom-6 left-1/2 -translate-x-1/2 z-40 w-[calc(100%-24px)] max-w-xl">
      <div className="rounded-2xl bg-zinc-900/95 text-white backdrop-blur-xl border border-white/10 shadow-2xl px-3 py-2 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 pl-1">
          <button
            type="button"
            onClick={onClear}
            aria-label="Cancel selection"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 transition"
          >
            <CloseFillIcon className="w-5 h-5" />
          </button>
          <span className="text-xs md:text-sm font-medium whitespace-nowrap font-mono-exif">
            {selectedCount} selected
          </span>
        </div>

        <div className="flex items-center gap-1">
          {isTrashContext ? (
            <>
              {onRestoreSelected && (
                <button
                  type="button"
                  onClick={onRestoreSelected}
                  className="min-h-[40px] px-3 rounded-xl hover:bg-white/10 text-xs font-medium flex items-center gap-1.5 transition"
                >
                  <RestoreFillIcon className="w-4 h-4" />
                  <span>Restore</span>
                </button>
              )}
              <button
                type="button"
                onClick={onTrashOrDelete}
                className="min-h-[40px] px-3 rounded-xl bg-red-600 hover:bg-red-700 text-xs font-medium flex items-center gap-1.5 transition"
              >
                <TrashFillIcon className="w-4 h-4" />
                <span>Delete</span>
              </button>
            </>
          ) : (
            <>
              {selectedCount >= 2 && selectedCount <= 4 && onCompareSelected && (
                <button
                  type="button"
                  onClick={onCompareSelected}
                  title="Compare side by side"
                  aria-label="Compare selected photos"
                  className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 text-[var(--accent)] transition"
                >
                  <CompareFillIcon className="w-4 h-4" />
                </button>
              )}
              <button
                type="button"
                onClick={onFavoriteAll}
                title="Favorite"
                aria-label="Favorite selected"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 transition"
              >
                <HeartFillIcon className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={onAddToAlbum}
                title="Add to album"
                aria-label="Add selected to album"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 transition"
              >
                <FolderPlusFillIcon className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={onDownloadSelected}
                title="Download"
                aria-label="Download selected"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 transition"
              >
                <DownloadFillIcon className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={onToggleLock}
                title={isLockedContext ? 'Unlock from Vault' : 'Move to Locked Vault'}
                aria-label={isLockedContext ? 'Unlock from Vault' : 'Move to Locked Vault'}
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-white/10 transition"
              >
                <LockFillIcon className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={onTrashOrDelete}
                title="Move to Trash"
                aria-label="Move selected to Trash"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl hover:bg-red-500/20 text-red-400 transition"
              >
                <TrashFillIcon className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
