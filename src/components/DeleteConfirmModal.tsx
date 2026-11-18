import React, { useState } from 'react';
import { TrashFillIcon } from './FilledIcons';

export type SnoozeDuration = 'none' | '15m' | '1h' | 'today';

interface DeleteConfirmModalProps {
  count: number;
  isPermanent: boolean;
  onConfirm: (snooze: SnoozeDuration) => void;
  onCancel: () => void;
}

const SNOOZE_OPTIONS: { id: SnoozeDuration; label: string }[] = [
  { id: 'none', label: 'Keep asking' },
  { id: '15m', label: '15 mins' },
  { id: '1h', label: '1 hour' },
  { id: 'today', label: 'Until midnight' },
];

export const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  count,
  isPermanent,
  onConfirm,
  onCancel,
}) => {
  const [snooze, setSnooze] = useState<SnoozeDuration>('none');

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Confirm deletion"
      className="fixed inset-0 z-50 bg-black/65 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      onClick={onCancel}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-t-3xl sm:rounded-3xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 p-6 shadow-2xl space-y-4"
      >
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-2xl bg-red-500/15 text-red-500 flex items-center justify-center shrink-0">
            <TrashFillIcon className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-zinc-950 dark:text-white">
              {isPermanent
                ? `Permanently delete ${count} ${count === 1 ? 'item' : 'items'}?`
                : `Move ${count} ${count === 1 ? 'item' : 'items'} to Trash?`}
            </h2>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              {isPermanent
                ? 'This cannot be undone.'
                : 'You can restore items from Recently Deleted for 30 days.'}
            </p>
          </div>
        </div>

        {!isPermanent && (
          <div className="pt-1">
            <p className="text-xs font-medium text-zinc-600 dark:text-zinc-400 mb-2">
              Snooze this prompt temporarily (auto-resets):
            </p>
            <div className="grid grid-cols-2 gap-2">
              {SNOOZE_OPTIONS.map((opt) => {
                const active = snooze === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => setSnooze(opt.id)}
                    className={`min-h-[38px] px-3 rounded-xl text-xs font-medium border transition ${
                      active
                        ? 'bg-[var(--accent)] border-[var(--accent)] text-white'
                        : 'border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-900'
                    }`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <div className="flex items-center gap-2 pt-2">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 min-h-[46px] rounded-2xl bg-zinc-100 dark:bg-zinc-900 hover:bg-zinc-200 dark:hover:bg-zinc-800 text-sm font-medium text-zinc-800 dark:text-zinc-200 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => onConfirm(snooze)}
            className="flex-1 min-h-[46px] rounded-2xl bg-red-600 hover:bg-red-700 text-sm font-medium text-white transition"
          >
            {isPermanent ? 'Delete Forever' : 'Move to Trash'}
          </button>
        </div>
      </div>
    </div>
  );
};
