import React from 'react';
import { MediaItem } from '../types/gallery';
import { CloseFillIcon, HeartFillIcon, TrashFillIcon } from './FilledIcons';

interface CompareModalProps {
  items: MediaItem[];
  onToggleFavorite: (id: string) => void;
  onTrashItem: (id: string) => void;
  onClose: () => void;
}

export const CompareModal: React.FC<CompareModalProps> = ({
  items,
  onToggleFavorite,
  onTrashItem,
  onClose,
}) => {
  if (items.length < 2) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Compare photos side by side"
      className="fixed inset-0 z-50 bg-black text-white flex flex-col"
    >
      <div className="h-14 px-4 flex items-center justify-between border-b border-zinc-900">
        <div>
          <h2 className="text-sm font-bold">Compare Shots</h2>
          <p className="text-[11px] text-zinc-400">Keep your favorite and trash the rest</p>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close comparison"
          className="min-h-[40px] min-w-[40px] flex items-center justify-center text-zinc-300 hover:text-white"
        >
          <CloseFillIcon className="w-6 h-6" />
        </button>
      </div>

      <div
        className={`flex-1 grid gap-2 p-2 md:p-4 overflow-hidden ${
          items.length === 2 ? 'grid-cols-2' : 'grid-cols-2 md:grid-cols-3'
        }`}
      >
        {items.slice(0, 4).map((item) => (
          <div
            key={item.id}
            className="relative rounded-2xl overflow-hidden bg-zinc-950 border border-zinc-800 flex flex-col"
          >
            <div className="flex-1 overflow-hidden flex items-center justify-center">
              <img
                src={item.src}
                alt={item.title}
                referrerPolicy="no-referrer"
                className="w-full h-full object-contain"
              />
            </div>

            <div className="p-2.5 bg-zinc-900/90 flex items-center justify-between gap-2">
              <span className="text-xs truncate text-zinc-300">{item.title}</span>
              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  onClick={() => onToggleFavorite(item.id)}
                  aria-label="Favorite this shot"
                  className="min-h-[36px] min-w-[36px] rounded-xl flex items-center justify-center hover:bg-white/10"
                >
                  <HeartFillIcon
                    className={`w-4 h-4 ${item.isFavorite ? 'text-[var(--accent)]' : 'text-zinc-400'}`}
                  />
                </button>
                <button
                  type="button"
                  onClick={() => onTrashItem(item.id)}
                  aria-label="Trash this shot"
                  className="min-h-[36px] min-w-[36px] rounded-xl flex items-center justify-center text-red-400 hover:bg-red-500/20"
                >
                  <TrashFillIcon className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
