import React, { useRef, useState } from 'react';
import { MediaItem } from '../types/gallery';
import { formatDuration } from '../utils/formatters';
import {
  CheckCircleFillIcon,
  HeartFillIcon,
  PhotosFillIcon,
  PlayFillIcon,
} from './FilledIcons';

interface MediaThumbProps {
  item: MediaItem;
  isSelectMode: boolean;
  isSelected: boolean;
  aspectClass?: string;
  roundedClass?: string;
  onOpen: (item: MediaItem) => void;
  onToggleSelect: (id: string) => void;
  onLongPressSelect: (id: string) => void;
}

export const MediaThumb: React.FC<MediaThumbProps> = ({
  item,
  isSelectMode,
  isSelected,
  aspectClass = 'aspect-square',
  roundedClass = 'rounded-[4px] md:rounded-lg',
  onOpen,
  onToggleSelect,
  onLongPressSelect,
}) => {
  const [hasError, setHasError] = useState(false);
  const timerRef = useRef<number | null>(null);
  const longPressedRef = useRef(false);

  const handlePointerDown = () => {
    longPressedRef.current = false;
    timerRef.current = window.setTimeout(() => {
      longPressedRef.current = true;
      onLongPressSelect(item.id);
    }, 450);
  };

  const clearTimer = () => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  const handleClick = () => {
    if (longPressedRef.current) {
      longPressedRef.current = false;
      return;
    }
    if (isSelectMode) {
      onToggleSelect(item.id);
    } else {
      onOpen(item);
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      onPointerDown={handlePointerDown}
      onPointerUp={clearTimer}
      onPointerLeave={clearTimer}
      onPointerCancel={clearTimer}
      aria-label={item.title}
      className={`group relative w-full ${aspectClass} ${roundedClass} overflow-hidden bg-zinc-100 dark:bg-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] select-none`}
    >
      {hasError ? (
        <div className="w-full h-full flex flex-col items-center justify-center p-2 bg-gradient-to-br from-zinc-200 to-zinc-300 dark:from-zinc-800 dark:to-zinc-900 text-zinc-500 dark:text-zinc-400">
          <PhotosFillIcon className="w-5 h-5 mb-1 opacity-70" />
          <span className="text-[11px] font-medium truncate max-w-full">{item.title}</span>
        </div>
      ) : item.mediaType === 'video' ? (
        <video
          src={item.src}
          muted
          playsInline
          preload="metadata"
          onError={() => setHasError(true)}
          className={`w-full h-full object-cover transition-transform duration-200 ${
            isSelected ? 'scale-90 rounded-lg' : 'group-hover:scale-[1.03]'
          }`}
        />
      ) : (
        <img
          src={item.src}
          alt={item.title}
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setHasError(true)}
          className={`w-full h-full object-cover transition-transform duration-200 ${
            isSelected ? 'scale-90 rounded-lg' : 'group-hover:scale-[1.03]'
          }`}
        />
      )}

      {(item.isFavorite || item.mediaType === 'video') && !isSelectMode && (
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-8 bg-gradient-to-t from-black/60 to-transparent" />
      )}

      {item.isFavorite && (
        <HeartFillIcon className="absolute bottom-1.5 left-1.5 w-3.5 h-3.5 text-white drop-shadow" />
      )}

      {item.mediaType === 'video' && (
        <div className="absolute bottom-1.5 right-1.5 flex items-center gap-1 text-[11px] font-mono-exif font-medium text-white drop-shadow">
          <PlayFillIcon className="w-3 h-3" />
          <span>{formatDuration(item.durationSec)}</span>
        </div>
      )}

      {isSelectMode && (
        <div className="absolute top-1.5 right-1.5">
          {isSelected ? (
            <CheckCircleFillIcon className="w-6 h-6 text-[var(--accent)] bg-white rounded-full shadow" />
          ) : (
            <span className="block w-5 h-5 rounded-full bg-black/35 border-2 border-white/85" />
          )}
        </div>
      )}
    </button>
  );
};
