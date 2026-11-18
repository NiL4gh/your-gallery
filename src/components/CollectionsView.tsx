import React, { useMemo, useState } from 'react';
import { MediaItem, MemoryStory } from '../types/gallery';
import { CameraFillIcon, PinFillIcon, PlayFillIcon } from './FilledIcons';
import { MediaThumb } from './MediaThumb';

interface CollectionsViewProps {
  media: MediaItem[];
  memories: MemoryStory[];
  onPlayMemory: (memory: MemoryStory) => void;
  onOpenItem: (item: MediaItem) => void;
}

export const CollectionsView: React.FC<CollectionsViewProps> = ({
  media,
  memories,
  onPlayMemory,
  onOpenItem,
}) => {
  const [selectedLens, setSelectedLens] = useState<string | null>(null);
  const [selectedPlace, setSelectedPlace] = useState<string | null>(null);

  const lenses = useMemo(() => {
    const map = new Map<string, MediaItem[]>();
    for (const item of media) {
      const lens = item.exif.lens || 'Camera';
      const arr = map.get(lens) || [];
      arr.push(item);
      map.set(lens, arr);
    }
    return Array.from(map.entries());
  }, [media]);

  const places = useMemo(() => {
    const map = new Map<string, MediaItem[]>();
    for (const item of media) {
      const place = item.exif.locationName;
      if (!place) continue;
      const arr = map.get(place) || [];
      arr.push(item);
      map.set(place, arr);
    }
    return Array.from(map.entries());
  }, [media]);

  const spotlightItems = useMemo(() => {
    if (selectedLens) return media.filter((m) => m.exif.lens === selectedLens);
    if (selectedPlace) return media.filter((m) => m.exif.locationName === selectedPlace);
    return [];
  }, [media, selectedLens, selectedPlace]);

  return (
    <section className="max-w-7xl mx-auto px-4 md:px-6 pt-4 pb-28 md:pb-14 space-y-10">
      {/* Memories Carousel */}
      <div>
        <div className="mb-4">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight text-zinc-950 dark:text-white">
            Memories
          </h1>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
            Curated reels from your best captures
          </p>
        </div>

        <div className="flex gap-4 overflow-x-auto no-scrollbar pb-2 snap-x snap-mandatory">
          {memories.map((mem) => {
            const storyItems = media.filter((m) => mem.mediaIds.includes(m.id));
            const cover = storyItems[0] || media[0];
            if (!cover) return null;

            return (
              <button
                key={mem.id}
                type="button"
                onClick={() => onPlayMemory(mem)}
                className="group relative snap-start shrink-0 w-[82%] sm:w-[340px] md:w-[380px] aspect-[4/5] sm:aspect-[16/11] rounded-3xl overflow-hidden bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]"
              >
                <img
                  src={cover.src}
                  alt={mem.title}
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-500"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/25 to-transparent" />
                <div className="absolute top-4 right-4 w-10 h-10 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center text-white">
                  <PlayFillIcon className="w-5 h-5 ml-0.5" />
                </div>
                <div className="absolute bottom-5 left-5 right-5">
                  <p className="text-xs text-zinc-200/90 font-mono-exif">
                    {storyItems.length} highlights
                  </p>
                  <h2 className="text-xl md:text-2xl font-bold text-white tracking-tight mt-0.5">
                    {mem.title}
                  </h2>
                  <p className="text-xs md:text-sm text-zinc-200/85 mt-0.5">{mem.subtitle}</p>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Camera Lenses */}
      <div>
        <div className="flex items-center justify-between gap-3 mb-3">
          <h2 className="text-base md:text-lg font-bold text-zinc-950 dark:text-white flex items-center gap-2">
            <CameraFillIcon className="w-4 h-4 text-[var(--accent)]" />
            <span>By Lens</span>
          </h2>
          {selectedLens && (
            <button
              type="button"
              onClick={() => setSelectedLens(null)}
              className="text-xs font-medium text-[var(--accent)] hover:underline"
            >
              Clear filter
            </button>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {lenses.map(([lens, items]) => {
            const active = selectedLens === lens;
            return (
              <button
                key={lens}
                type="button"
                onClick={() => {
                  setSelectedPlace(null);
                  setSelectedLens(active ? null : lens);
                }}
                className={`p-3.5 rounded-2xl border text-left transition ${
                  active
                    ? 'bg-[var(--accent)] border-[var(--accent)] text-white'
                    : 'bg-zinc-50 dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 hover:border-zinc-300 dark:hover:border-zinc-700'
                }`}
              >
                <p
                  className={`text-sm font-medium truncate ${
                    active ? 'text-white' : 'text-zinc-950 dark:text-zinc-100'
                  }`}
                >
                  {lens}
                </p>
                <p
                  className={`text-xs mt-1 font-mono-exif ${
                    active ? 'text-white/85' : 'text-zinc-500 dark:text-zinc-400'
                  }`}
                >
                  {items.length} {items.length === 1 ? 'shot' : 'shots'}
                </p>
              </button>
            );
          })}
        </div>
      </div>

      {/* Places */}
      {places.length > 0 && (
        <div>
          <div className="flex items-center justify-between gap-3 mb-3">
            <h2 className="text-base md:text-lg font-bold text-zinc-950 dark:text-white flex items-center gap-2">
              <PinFillIcon className="w-4 h-4 text-[var(--accent)]" />
              <span>Places</span>
            </h2>
            {selectedPlace && (
              <button
                type="button"
                onClick={() => setSelectedPlace(null)}
                className="text-xs font-medium text-[var(--accent)] hover:underline"
              >
                Clear filter
              </button>
            )}
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {places.map(([place, items]) => {
              const active = selectedPlace === place;
              const cover = items[0];
              return (
                <button
                  key={place}
                  type="button"
                  onClick={() => {
                    setSelectedLens(null);
                    setSelectedPlace(active ? null : place);
                  }}
                  className={`group rounded-2xl overflow-hidden border text-left transition ${
                    active
                      ? 'ring-2 ring-[var(--accent)] border-[var(--accent)]'
                      : 'border-zinc-200 dark:border-zinc-800'
                  }`}
                >
                  <div className="aspect-square overflow-hidden bg-zinc-100 dark:bg-zinc-900">
                    <img
                      src={cover.src}
                      alt={place}
                      referrerPolicy="no-referrer"
                      className="w-full h-full object-cover group-hover:scale-[1.04] transition-transform duration-200"
                    />
                  </div>
                  <div className="p-2.5 bg-white dark:bg-zinc-950">
                    <p className="text-xs font-medium text-zinc-950 dark:text-zinc-100 truncate">
                      {place}
                    </p>
                    <p className="text-[11px] text-zinc-500 dark:text-zinc-400 font-mono-exif">
                      {items.length} {items.length === 1 ? 'photo' : 'photos'}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {spotlightItems.length > 0 && (
        <div>
          <h3 className="text-sm font-bold text-zinc-600 dark:text-zinc-300 mb-3">
            {selectedLens || selectedPlace} · {spotlightItems.length}{' '}
            {spotlightItems.length === 1 ? 'photo' : 'photos'}
          </h3>
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-1.5 md:gap-2">
            {spotlightItems.map((item) => (
              <MediaThumb
                key={item.id}
                item={item}
                isSelectMode={false}
                isSelected={false}
                onOpen={onOpenItem}
                onToggleSelect={() => {}}
                onLongPressSelect={() => {}}
              />
            ))}
          </div>
        </div>
      )}
    </section>
  );
};
