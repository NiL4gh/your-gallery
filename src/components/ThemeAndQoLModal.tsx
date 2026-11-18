import React, { useMemo } from 'react';
import { MediaItem, THEME_OPTIONS, ThemeId } from '../types/gallery';
import { formatBytes } from '../utils/formatters';
import {
  CheckCircleFillIcon,
  CloseFillIcon,
  FolderSyncFillIcon,
  MoonFillIcon,
  PaletteFillIcon,
  SunFillIcon,
  TrashFillIcon,
} from './FilledIcons';

interface ThemeAndQoLModalProps {
  galleryTitle?: string;
  onChangeGalleryTitle?: (title: string) => void;
  themeId: ThemeId;
  onChangeTheme: (id: ThemeId) => void;
  isDark: boolean;
  onToggleDark: () => void;
  linkedFolderName: string | null;
  supportsFolderSync: boolean;
  onLinkOrRescanFolder: () => void;
  onUnlinkFolder: () => void;
  deleteSnoozeUntil: number | null;
  onResetDeleteSnooze: () => void;
  media: MediaItem[];
  onQuickCleanScreenshots: (ids: string[]) => void;
  onOpenItem: (item: MediaItem) => void;
  onClose: () => void;
}

export const ThemeAndQoLModal: React.FC<ThemeAndQoLModalProps> = ({
  galleryTitle,
  onChangeGalleryTitle,
  themeId,
  onChangeTheme,
  isDark,
  onToggleDark,
  linkedFolderName,
  supportsFolderSync,
  onLinkOrRescanFolder,
  onUnlinkFolder,
  deleteSnoozeUntil,
  onResetDeleteSnooze,
  media,
  onQuickCleanScreenshots,
  onOpenItem,
  onClose,
}) => {
  const now = Date.now();
  const snoozeMinutesLeft =
    deleteSnoozeUntil && deleteSnoozeUntil > now
      ? Math.max(1, Math.ceil((deleteSnoozeUntil - now) / 60000))
      : 0;

  const screenshots = useMemo(
    () =>
      media.filter(
        (m) =>
          m.tags.includes('screenshot') ||
          m.albumIds.includes('screenshots') ||
          /screenshot|screen/i.test(m.title)
      ),
    [media]
  );

  const duplicateGroups = useMemo(() => {
    const map = new Map<string, MediaItem[]>();
    for (const item of media) {
      const sig = `${item.exif.width}x${item.exif.height}-${item.exif.fileSizeBytes}`;
      const list = map.get(sig) || [];
      list.push(item);
      map.set(sig, list);
    }
    return Array.from(map.values()).filter((g) => g.length > 1);
  }, [media]);

  const totalBytes = useMemo(
    () => media.reduce((acc, m) => acc + (m.exif.fileSizeBytes || 0), 0),
    [media]
  );

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Themes and Smart Tools"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md max-h-[86vh] overflow-y-auto rounded-t-3xl sm:rounded-3xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 p-6 shadow-2xl space-y-6"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <PaletteFillIcon className="w-5 h-5 text-[var(--accent)]" />
            <h2 className="text-lg font-bold text-zinc-950 dark:text-white">
              Themes & Smart Tools
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center text-zinc-500 hover:text-zinc-900 dark:hover:text-white"
          >
            <CloseFillIcon className="w-6 h-6" />
          </button>
        </div>

        {/* Gallery Personalization */}
        {onChangeGalleryTitle && (
          <div className="space-y-1.5">
            <label htmlFor="gallery-title-input" className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
              Gallery Name
            </label>
            <input
              id="gallery-title-input"
              type="text"
              value={galleryTitle || ''}
              onChange={(e) => onChangeGalleryTitle(e.target.value)}
              placeholder="Your Gallery"
              className="w-full px-3.5 py-2.5 text-sm rounded-xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
            />
          </div>
        )}

        {/* Appearance & Palette Picker */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
              Color Theme
            </span>
            <button
              type="button"
              onClick={onToggleDark}
              className="min-h-[36px] px-3 rounded-xl bg-zinc-100 dark:bg-zinc-900 text-xs font-medium text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5"
            >
              {isDark ? <MoonFillIcon className="w-4 h-4 text-[var(--accent)]" /> : <SunFillIcon className="w-4 h-4 text-[var(--accent)]" />}
              <span>{isDark ? 'OLED Dark' : 'Crisp Light'}</span>
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2.5">
            {THEME_OPTIONS.map((t) => {
              const active = themeId === t.id;
              return (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => onChangeTheme(t.id)}
                  className={`min-h-[46px] px-3.5 rounded-2xl border flex items-center justify-between transition ${
                    active
                      ? 'border-[var(--accent)] bg-[var(--accent-soft)] text-zinc-950 dark:text-white'
                      : 'border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-900'
                  }`}
                >
                  <span className="flex items-center gap-2.5 text-xs font-medium">
                    <span
                      className="w-4 h-4 rounded-full shrink-0"
                      style={{ backgroundColor: t.swatch }}
                    />
                    {t.label}
                  </span>
                  {active && <CheckCircleFillIcon className="w-4 h-4 text-[var(--accent)]" />}
                </button>
              );
            })}
          </div>
        </div>

        {/* Auto-Populate Phone Folder Sync */}
        <div className="rounded-2xl border border-zinc-200 dark:border-zinc-800 p-4 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <FolderSyncFillIcon className="w-5 h-5 text-[var(--accent)]" />
              <span className="text-sm font-semibold text-zinc-950 dark:text-white">
                Auto-Populate Phone Photos
              </span>
            </div>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 leading-relaxed">
            {linkedFolderName
              ? `Linked to "${linkedFolderName}". New photos in this folder auto-populate whenever you open ${galleryTitle || 'Your Gallery'}.`
              : supportsFolderSync
              ? 'Link your phone DCIM or Pictures folder once so new camera shots auto-populate.'
              : 'Your browser supports instant multi-photo import via Add Photos.'}
          </p>
          {supportsFolderSync && (
            <div className="flex items-center gap-2 pt-1">
              <button
                type="button"
                onClick={onLinkOrRescanFolder}
                className="min-h-[40px] px-3.5 rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white text-xs font-medium transition"
              >
                {linkedFolderName ? 'Rescan Folder Now' : 'Link Phone Folder'}
              </button>
              {linkedFolderName && (
                <button
                  type="button"
                  onClick={onUnlinkFolder}
                  className="min-h-[40px] px-3 rounded-xl bg-zinc-100 dark:bg-zinc-900 text-xs font-medium text-zinc-600 dark:text-zinc-300"
                >
                  Unlink
                </button>
              )}
            </div>
          )}
        </div>

        {/* Delete Confirmation Shield */}
        <div className="rounded-2xl border border-zinc-200 dark:border-zinc-800 p-4 flex items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-zinc-950 dark:text-white">
              Delete Confirmation
            </p>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              {snoozeMinutesLeft > 0
                ? `Snoozed for ${snoozeMinutesLeft} min (auto-resets)`
                : 'Always asks before deleting photos'}
            </p>
          </div>
          {snoozeMinutesLeft > 0 && (
            <button
              type="button"
              onClick={onResetDeleteSnooze}
              className="min-h-[36px] px-3 rounded-xl bg-zinc-100 dark:bg-zinc-900 text-xs font-medium text-[var(--accent)] shrink-0"
            >
              Turn On
            </button>
          )}
        </div>

        {/* Smart QoL Cleanup */}
        <div className="rounded-2xl border border-zinc-200 dark:border-zinc-800 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-zinc-950 dark:text-white">
              Smart Space Helper
            </span>
            <span className="text-xs text-zinc-500 dark:text-zinc-400 font-mono-exif">
              {formatBytes(totalBytes)} total
            </span>
          </div>

          <div className="flex items-center justify-between text-xs py-1">
            <span className="text-zinc-600 dark:text-zinc-300">
              Screenshots ({screenshots.length})
            </span>
            {screenshots.length > 0 ? (
              <button
                type="button"
                onClick={() => {
                  onQuickCleanScreenshots(screenshots.map((s) => s.id));
                  onClose();
                }}
                className="text-red-500 font-medium flex items-center gap-1 hover:underline"
              >
                <TrashFillIcon className="w-3.5 h-3.5" />
                <span>Clean All</span>
              </button>
            ) : (
              <span className="text-zinc-400">Clean</span>
            )}
          </div>

          <div className="flex items-center justify-between text-xs py-1 border-t border-zinc-100 dark:border-zinc-900">
            <span className="text-zinc-600 dark:text-zinc-300">
              Possible Duplicates ({duplicateGroups.length} sets)
            </span>
            {duplicateGroups.length > 0 ? (
              <button
                type="button"
                onClick={() => {
                  onOpenItem(duplicateGroups[0][0]);
                  onClose();
                }}
                className="text-[var(--accent)] font-medium hover:underline"
              >
                Review
              </button>
            ) : (
              <span className="text-zinc-400">None found</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
