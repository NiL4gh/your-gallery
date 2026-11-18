import React, { useEffect, useState } from 'react';
import { MediaItem, MemoryStory } from '../types/gallery';
import { formatFullDate } from '../utils/formatters';
import { CloseFillIcon, PauseFillIcon, PlayFillIcon } from './FilledIcons';

interface MemoryPlayerModalProps {
  memory: MemoryStory;
  items: MediaItem[];
  onClose: () => void;
}

export const MemoryPlayerModal: React.FC<MemoryPlayerModalProps> = ({
  memory,
  items,
  onClose,
}) => {
  const [idx, setIdx] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (paused || items.length <= 1) return;
    const timer = window.setInterval(() => {
      setIdx((prev) => {
        if (prev >= items.length - 1) {
          onClose();
          return prev;
        }
        return prev + 1;
      });
    }, 3600);
    return () => window.clearInterval(timer);
  }, [paused, items.length, onClose]);

  const current = items[idx];
  if (!current) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={memory.title}
      className="fixed inset-0 z-50 bg-black text-white flex flex-col justify-between select-none"
    >
      <img
        src={current.src}
        alt={current.title}
        referrerPolicy="no-referrer"
        className="absolute inset-0 w-full h-full object-cover opacity-90 transition-opacity duration-500"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/70 via-transparent to-black/85" />

      <div className="relative z-20 px-4 pt-4 space-y-3">
        <div className="flex gap-1.5 max-w-2xl mx-auto">
          {items.map((item, i) => (
            <div key={item.id} className="h-1 flex-1 rounded-full bg-white/25 overflow-hidden">
              <div
                className={`h-full bg-white transition-all duration-300 ${
                  i < idx ? 'w-full' : i === idx ? 'w-full opacity-90' : 'w-0'
                }`}
              />
            </div>
          ))}
        </div>

        <div className="flex items-center justify-between max-w-2xl mx-auto">
          <div>
            <h2 className="text-base font-bold">{memory.title}</h2>
            <p className="text-xs text-zinc-300">{memory.subtitle}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPaused((p) => !p)}
              aria-label={paused ? 'Resume memory' : 'Pause memory'}
              className="min-h-[44px] min-w-[44px] rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center"
            >
              {paused ? <PlayFillIcon className="w-5 h-5" /> : <PauseFillIcon className="w-5 h-5" />}
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close memory"
              className="min-h-[44px] min-w-[44px] rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center"
            >
              <CloseFillIcon className="w-6 h-6" />
            </button>
          </div>
        </div>
      </div>

      <div className="relative z-10 flex-1 grid grid-cols-2">
        <button
          type="button"
          aria-label="Previous slide"
          onClick={() => setIdx((i) => Math.max(0, i - 1))}
          className="h-full w-full focus:outline-none"
        />
        <button
          type="button"
          aria-label="Next slide"
          onClick={() => {
            if (idx >= items.length - 1) onClose();
            else setIdx((i) => i + 1);
          }}
          className="h-full w-full focus:outline-none"
        />
      </div>

      <div className="relative z-20 px-6 pb-8 max-w-2xl mx-auto w-full">
        <p className="text-xl font-bold">{current.title}</p>
        <p className="text-xs text-zinc-300 font-mono-exif mt-1">
          {formatFullDate(current.createdAt)}
          {current.exif.locationName ? ` · ${current.exif.locationName}` : ''}
        </p>
      </div>
    </div>
  );
};
