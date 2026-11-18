import React, { useEffect, useRef, useState } from 'react';
import { Album, MediaItem } from '../types/gallery';
import { formatBytes, formatFullDate, formatTime } from '../utils/formatters';
import { extractDominantPalette } from '../utils/imageEditor';
import {
  ArrowLeftFillIcon,
  ChevronRightFillIcon,
  CloseFillIcon,
  DownloadFillIcon,
  EditFillIcon,
  FolderPlusFillIcon,
  HeartFillIcon,
  InfoFillIcon,
  LockFillIcon,
  RestoreFillIcon,
  ShareFillIcon,
  TrashFillIcon,
} from './FilledIcons';

interface LightboxViewerProps {
  items: MediaItem[];
  currentIndex: number;
  albums: Album[];
  onClose: () => void;
  onChangeIndex: (idx: number) => void;
  onToggleFavorite: (id: string) => void;
  onEdit: (item: MediaItem) => void;
  onTrash: (id: string) => void;
  onRestore?: (id: string) => void;
  onToggleLock: (id: string) => void;
  onUpdateDetails: (id: string, patch: { caption?: string; tags?: string[]; albumIds?: string[] }) => void;
  onOpenAlbumPicker: (ids: string[]) => void;
}

export const LightboxViewer: React.FC<LightboxViewerProps> = ({
  items,
  currentIndex,
  albums,
  onClose,
  onChangeIndex,
  onToggleFavorite,
  onEdit,
  onTrash,
  onRestore,
  onToggleLock,
  onUpdateDetails,
  onOpenAlbumPicker,
}) => {
  const item = items[currentIndex];
  const [chromeVisible, setChromeVisible] = useState(true);
  const [showInfo, setShowInfo] = useState(false);
  const [zoomed, setZoomed] = useState(false);
  const [palette, setPalette] = useState<string[]>([]);
  const [captionDraft, setCaptionDraft] = useState('');
  const [tagDraft, setTagDraft] = useState('');
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);

  useEffect(() => {
    if (!item) return;
    setZoomed(false);
    setCaptionDraft(item.caption || '');
    if (item.mediaType === 'image') {
      extractDominantPalette(item.src).then(setPalette);
    } else {
      setPalette([]);
    }
  }, [item?.id, item?.src]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (showInfo && (e.target as HTMLElement)?.tagName === 'INPUT') return;
      if (showInfo && (e.target as HTMLElement)?.tagName === 'TEXTAREA') return;

      if (e.key === 'Escape') {
        if (showInfo) setShowInfo(false);
        else onClose();
      } else if (e.key === 'ArrowLeft' && currentIndex > 0) {
        onChangeIndex(currentIndex - 1);
      } else if (e.key === 'ArrowRight' && currentIndex < items.length - 1) {
        onChangeIndex(currentIndex + 1);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [currentIndex, items.length, onChangeIndex, onClose, showInfo]);

  if (!item) return null;

  const handleShareOrDownload = async () => {
    try {
      if (navigator.share && item.src.startsWith('data:')) {
        const res = await fetch(item.src);
        const blob = await res.blob();
        const file = new File([blob], `${item.title || 'photo'}.jpg`, { type: blob.type || 'image/jpeg' });
        if (navigator.canShare && navigator.canShare({ files: [file] })) {
          await navigator.share({ files: [file], title: item.title });
          return;
        }
      }
    } catch {
      // Fallback to download
    }

    const a = document.createElement('a');
    a.href = item.src;
    a.download = `${item.title.replace(/\s+/g, '_').toLowerCase()}.${
      item.mediaType === 'video' ? 'mp4' : 'jpg'
    }`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (zoomed || showInfo) return;
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null || zoomed || showInfo) return;
    const dx = e.changedTouches[0].clientX - touchStartX.current;
    const dy = e.changedTouches[0].clientY - touchStartY.current;
    touchStartX.current = null;
    touchStartY.current = null;

    if (Math.abs(dx) > 48 && Math.abs(dx) > Math.abs(dy)) {
      if (dx < 0 && currentIndex < items.length - 1) onChangeIndex(currentIndex + 1);
      if (dx > 0 && currentIndex > 0) onChangeIndex(currentIndex - 1);
    } else if (dy < -70 && Math.abs(dy) > Math.abs(dx)) {
      setShowInfo(true);
    } else if (dy > 90 && Math.abs(dy) > Math.abs(dx)) {
      onClose();
    }
  };

  const addTag = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = tagDraft.trim().toLowerCase();
    if (!clean || item.tags.includes(clean)) {
      setTagDraft('');
      return;
    }
    onUpdateDetails(item.id, { tags: [...item.tags, clean] });
    setTagDraft('');
  };

  const removeTag = (tag: string) => {
    onUpdateDetails(item.id, { tags: item.tags.filter((t) => t !== tag) });
  };

  const itemAlbums = albums.filter((a) => item.albumIds.includes(a.id));

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={item.title}
      className="fixed inset-0 z-50 bg-black text-white flex flex-col select-none"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {/* Top Overlay Bar */}
      {chromeVisible && (
        <div className="absolute top-0 inset-x-0 z-20 px-3 md:px-6 py-3 bg-gradient-to-b from-black/80 via-black/40 to-transparent flex items-center justify-between gap-4">
          <div className="flex items-center gap-2 min-w-0">
            <button
              type="button"
              onClick={onClose}
              aria-label="Close viewer"
              className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-md transition"
            >
              <CloseFillIcon className="w-6 h-6" />
            </button>
            <div className="min-w-0 pl-1">
              <p className="text-sm font-medium text-white truncate">{item.title}</p>
              <p className="text-xs text-zinc-300 font-mono-exif truncate">
                {formatFullDate(item.createdAt)} · {formatTime(item.createdAt)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={() => setShowInfo((v) => !v)}
              aria-label="Photo details and EXIF"
              className={`min-h-[44px] min-w-[44px] flex items-center justify-center rounded-full backdrop-blur-md transition ${
                showInfo
                  ? 'bg-[var(--accent)] text-white'
                  : 'bg-white/10 hover:bg-white/20 text-white'
              }`}
            >
              <InfoFillIcon className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}

      {/* Main Media Stage */}
      <div
        className="flex-1 relative flex items-center justify-center overflow-hidden"
        onClick={() => {
          if (!showInfo) setChromeVisible((c) => !c);
        }}
      >
        {item.mediaType === 'video' ? (
          <video
            src={item.src}
            controls
            autoPlay
            playsInline
            onClick={(e) => e.stopPropagation()}
            className="max-w-full max-h-full object-contain"
          />
        ) : (
          <img
            src={item.src}
            alt={item.title}
            referrerPolicy="no-referrer"
            onDoubleClick={(e) => {
              e.stopPropagation();
              setZoomed((z) => !z);
            }}
            className={`max-w-full max-h-full object-contain transition-transform duration-300 ${
              zoomed ? 'scale-[1.85] cursor-zoom-out' : 'scale-100 cursor-zoom-in'
            }`}
          />
        )}

        {/* Desktop Prev / Next Arrows */}
        {chromeVisible && currentIndex > 0 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onChangeIndex(currentIndex - 1);
            }}
            aria-label="Previous photo"
            className="hidden md:flex absolute left-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/50 hover:bg-black/75 border border-white/15 items-center justify-center text-white backdrop-blur-md transition"
          >
            <ArrowLeftFillIcon className="w-6 h-6" />
          </button>
        )}
        {chromeVisible && currentIndex < items.length - 1 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onChangeIndex(currentIndex + 1);
            }}
            aria-label="Next photo"
            className="hidden md:flex absolute right-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/50 hover:bg-black/75 border border-white/15 items-center justify-center text-white backdrop-blur-md transition"
          >
            <ChevronRightFillIcon className="w-6 h-6" />
          </button>
        )}
      </div>

      {/* Bottom Filmstrip Scrubber + Action Dock */}
      {chromeVisible && (
        <div className="relative z-20 bg-gradient-to-t from-black via-black/90 to-transparent pt-4 pb-[max(12px,env(safe-area-inset-bottom))]">
          {items.length > 1 && (
            <div className="flex items-center justify-center gap-1 px-4 pb-3 overflow-x-auto no-scrollbar">
              {items.slice(Math.max(0, currentIndex - 10), Math.min(items.length, currentIndex + 11)).map((thumb) => {
                const realIdx = items.findIndex((i) => i.id === thumb.id);
                const isCurrent = realIdx === currentIndex;
                return (
                  <button
                    key={thumb.id}
                    type="button"
                    onClick={() => onChangeIndex(realIdx)}
                    aria-label={`Jump to ${thumb.title}`}
                    className={`shrink-0 h-10 rounded overflow-hidden transition-all ${
                      isCurrent
                        ? 'w-10 ring-2 ring-[var(--accent)] mx-1.5 scale-105'
                        : 'w-6 opacity-55 hover:opacity-90'
                    }`}
                  >
                    {thumb.mediaType === 'video' ? (
                      <video src={thumb.src} muted className="w-full h-full object-cover" />
                    ) : (
                      <img
                        src={thumb.src}
                        alt=""
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover"
                      />
                    )}
                  </button>
                );
              })}
            </div>
          )}

          <div className="max-w-lg mx-auto px-3 flex items-center justify-around">
            <button
              type="button"
              onClick={handleShareOrDownload}
              className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-white transition"
            >
              {typeof navigator !== 'undefined' && 'share' in navigator ? (
                <ShareFillIcon className="w-5 h-5" />
              ) : (
                <DownloadFillIcon className="w-5 h-5" />
              )}
              <span className="text-[10px]">Share</span>
            </button>

            <button
              type="button"
              onClick={() => onToggleFavorite(item.id)}
              className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-white transition"
            >
              <HeartFillIcon
                className={`w-5 h-5 ${item.isFavorite ? 'text-[var(--accent)]' : ''}`}
              />
              <span className="text-[10px]">Favorite</span>
            </button>

            {item.mediaType === 'image' && !item.isTrashed && (
              <button
                type="button"
                onClick={() => onEdit(item)}
                className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-white transition"
              >
                <EditFillIcon className="w-5 h-5" />
                <span className="text-[10px]">Edit</span>
              </button>
            )}

            {!item.isTrashed && (
              <button
                type="button"
                onClick={() => onOpenAlbumPicker([item.id])}
                className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-white transition"
              >
                <FolderPlusFillIcon className="w-5 h-5" />
                <span className="text-[10px]">Album</span>
              </button>
            )}

            {!item.isTrashed && (
              <button
                type="button"
                onClick={() => {
                  onToggleLock(item.id);
                  if (items.length <= 1) onClose();
                }}
                className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-white transition"
              >
                <LockFillIcon className="w-5 h-5" />
                <span className="text-[10px]">{item.isLocked ? 'Unlock' : 'Lock'}</span>
              </button>
            )}

            {item.isTrashed && onRestore ? (
              <button
                type="button"
                onClick={() => onRestore(item.id)}
                className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-[var(--accent)] transition"
              >
                <RestoreFillIcon className="w-5 h-5" />
                <span className="text-[10px]">Restore</span>
              </button>
            ) : null}

            <button
              type="button"
              onClick={() => onTrash(item.id)}
              className="min-h-[44px] min-w-[44px] flex flex-col items-center justify-center gap-1 text-zinc-200 hover:text-red-400 transition"
            >
              <TrashFillIcon className="w-5 h-5" />
              <span className="text-[10px]">{item.isTrashed ? 'Delete' : 'Trash'}</span>
            </button>
          </div>
        </div>
      )}

      {/* Slide-Up Details & EXIF Sheet */}
      {showInfo && (
        <div
          className="fixed inset-0 z-30 bg-black/50 backdrop-blur-sm flex items-end md:items-center justify-center"
          onClick={() => setShowInfo(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-lg max-h-[82vh] overflow-y-auto rounded-t-3xl md:rounded-3xl bg-zinc-950 border border-zinc-800 p-5 space-y-5 text-zinc-100"
          >
            <div className="w-10 h-1 bg-zinc-700 rounded-full mx-auto md:hidden" />

            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-bold text-white">{item.title}</h3>
                <p className="text-xs text-zinc-400 font-mono-exif mt-0.5">
                  {formatFullDate(item.createdAt)} · {formatTime(item.createdAt)}
                  {item.exif.locationName ? ` · ${item.exif.locationName}` : ''}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowInfo(false)}
                aria-label="Close details"
                className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-full text-zinc-400 hover:text-white"
              >
                <CloseFillIcon className="w-6 h-6" />
              </button>
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-400 mb-1.5">Caption</label>
              <textarea
                rows={2}
                value={captionDraft}
                onChange={(e) => setCaptionDraft(e.target.value)}
                onBlur={() => onUpdateDetails(item.id, { caption: captionDraft })}
                placeholder="Add a note or memory..."
                className="w-full rounded-xl bg-zinc-900 border border-zinc-800 px-3 py-2 text-sm text-white placeholder:text-zinc-500 focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
              />
            </div>

            <div className="rounded-2xl bg-zinc-900/90 border border-zinc-800 p-4 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-bold text-white">{item.exif.camera}</span>
                <span className="text-xs text-zinc-400 font-mono-exif">{item.exif.lens}</span>
              </div>
              <div className="pt-2 border-t border-zinc-800 grid grid-cols-4 gap-2 text-center font-mono-exif">
                <div>
                  <p className="text-[11px] text-zinc-500">Focal</p>
                  <p className="text-xs font-medium text-zinc-200">{item.exif.focalLength}</p>
                </div>
                <div>
                  <p className="text-[11px] text-zinc-500">Aperture</p>
                  <p className="text-xs font-medium text-zinc-200">{item.exif.aperture}</p>
                </div>
                <div>
                  <p className="text-[11px] text-zinc-500">Shutter</p>
                  <p className="text-xs font-medium text-zinc-200">{item.exif.shutter}</p>
                </div>
                <div>
                  <p className="text-[11px] text-zinc-500">ISO</p>
                  <p className="text-xs font-medium text-zinc-200">{item.exif.iso}</p>
                </div>
              </div>
              <div className="pt-2 border-t border-zinc-800 flex items-center justify-between text-xs text-zinc-400 font-mono-exif">
                <span>
                  {item.exif.width} × {item.exif.height} px
                </span>
                <span>{formatBytes(item.exif.fileSizeBytes)}</span>
              </div>
            </div>

            {palette.length > 0 && (
              <div>
                <p className="text-xs font-medium text-zinc-400 mb-2">Color Palette</p>
                <div className="flex items-center gap-2">
                  {palette.map((hex) => (
                    <span
                      key={hex}
                      className="w-6 h-6 rounded-full border border-white/15"
                      style={{ backgroundColor: hex }}
                      title={hex}
                    />
                  ))}
                </div>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <p className="text-xs font-medium text-zinc-400">Albums</p>
                <button
                  type="button"
                  onClick={() => onOpenAlbumPicker([item.id])}
                  className="text-xs text-[var(--accent)] hover:underline"
                >
                  Manage
                </button>
              </div>
              <p className="text-xs text-zinc-300">
                {itemAlbums.length ? itemAlbums.map((a) => a.name).join(' · ') : 'Not in any album'}
              </p>
            </div>

            <div>
              <p className="text-xs font-medium text-zinc-400 mb-1.5">Search Tags</p>
              <div className="flex flex-wrap items-center gap-2 text-xs text-zinc-300 mb-2">
                {item.tags.map((tag) => (
                  <button
                    key={tag}
                    type="button"
                    onClick={() => removeTag(tag)}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-zinc-900 border border-zinc-800 hover:border-zinc-700"
                    title="Remove tag"
                  >
                    <span>{tag}</span>
                    <CloseFillIcon className="w-3.5 h-3.5 text-zinc-500" />
                  </button>
                ))}
              </div>
              <form onSubmit={addTag} className="flex gap-2">
                <input
                  type="text"
                  value={tagDraft}
                  onChange={(e) => setTagDraft(e.target.value)}
                  placeholder="Add keyword tag..."
                  className="flex-1 min-h-[38px] px-3 rounded-xl bg-zinc-900 border border-zinc-800 text-xs text-white focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
                />
                <button
                  type="submit"
                  className="min-h-[38px] px-3.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-xs font-medium"
                >
                  Add
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
