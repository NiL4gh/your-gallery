import React, { useMemo, useState } from 'react';
import { MediaItem } from '../types/gallery';
import { formatFullDate } from '../utils/formatters';
import { CloseFillIcon, SearchFillIcon } from './FilledIcons';
import { MediaThumb } from './MediaThumb';

interface SearchViewProps {
  media: MediaItem[];
  onOpenItem: (item: MediaItem) => void;
}

const SUGGESTIONS = ['Night', 'Coastal', '50MP', 'Macro', 'Architecture', 'Favorites', 'Edited'];

export const SearchView: React.FC<SearchViewProps> = ({ media, onOpenItem }) => {
  const [query, setQuery] = useState('');

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return media;

    return media.filter((item) => {
      if (q === 'favorites' || q === 'favorite') return item.isFavorite;
      if (q === 'edited') return !!item.isEdited;
      if (q === 'video' || q === 'videos') return item.mediaType === 'video';

      const haystack = [
        item.title,
        item.caption,
        item.exif.camera,
        item.exif.lens,
        item.exif.locationName || '',
        formatFullDate(item.createdAt),
        ...item.tags,
      ]
        .join(' ')
        .toLowerCase();

      return haystack.includes(q);
    });
  }, [media, query]);

  return (
    <section className="max-w-7xl mx-auto px-4 md:px-6 pt-4 pb-28 md:pb-14 space-y-6">
      <div>
        <h1 className="text-xl md:text-2xl font-bold tracking-tight text-zinc-950 dark:text-white mb-3">
          Search
        </h1>

        <div className="relative max-w-2xl">
          <SearchFillIcon className="w-4 h-4 text-zinc-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search photos, locations, lenses, or tags..."
            className="w-full min-h-[48px] pl-11 pr-11 rounded-2xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800 text-sm text-zinc-950 dark:text-white placeholder:text-zinc-500 focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery('')}
              aria-label="Clear search"
              className="min-h-[40px] min-w-[40px] flex items-center justify-center absolute right-1.5 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-900 dark:hover:text-white"
            >
              <CloseFillIcon className="w-5 h-5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pt-3">
          {SUGGESTIONS.map((s) => {
            const active = query.toLowerCase() === s.toLowerCase();
            return (
              <button
                key={s}
                type="button"
                onClick={() => setQuery(active ? '' : s)}
                className={`min-h-[36px] px-3.5 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 border transition ${
                  active
                    ? 'bg-[var(--accent)] border-[var(--accent)] text-white'
                    : 'bg-zinc-50 dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-900'
                }`}
              >
                {s}
              </button>
            );
          })}
        </div>
      </div>

      <div>
        <p className="text-xs text-zinc-500 dark:text-zinc-400 mb-3 font-mono-exif">
          {results.length} {results.length === 1 ? 'result' : 'results'}
          {query.trim() ? ` for "${query.trim()}"` : ''}
        </p>

        {results.length === 0 ? (
          <div className="rounded-2xl border border-zinc-200 dark:border-zinc-800 p-10 text-center max-w-md">
            <p className="text-sm font-medium text-zinc-800 dark:text-zinc-200">
              No matching photos or videos
            </p>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              Try searching by camera lens (50MP), place, or tag.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-1 md:gap-2">
            {results.map((item) => (
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
        )}
      </div>
    </section>
  );
};
