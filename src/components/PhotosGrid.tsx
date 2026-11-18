import React, { useMemo } from 'react';
import { MediaItem, SortOrder, ZoomDensity } from '../types/gallery';
import { groupMediaByDensity } from '../utils/formatters';
import {
  CheckCircleFillIcon,
  HeartFillIcon,
  PhotosFillIcon,
  SlidersFillIcon,
} from './FilledIcons';
import { MediaThumb } from './MediaThumb';

interface PhotosGridProps {
  items: MediaItem[];
  density: ZoomDensity;
  onDensityChange: (d: ZoomDensity) => void;
  gridScale: 'compact' | 'standard' | 'large';
  onGridScaleChange: (s: 'compact' | 'standard' | 'large') => void;
  sortOrder: SortOrder;
  onSortOrderChange: (s: SortOrder) => void;
  favoritesOnly: boolean;
  onToggleFavoritesOnly: () => void;
  isSelectMode: boolean;
  onToggleSelectMode: () => void;
  selectedIds: Set<string>;
  onToggleSelect: (id: string) => void;
  onSelectGroup: (ids: string[]) => void;
  onLongPressSelect: (id: string) => void;
  onOpenItem: (item: MediaItem) => void;
  onOpenImport: () => void;
}

const DENSITY_OPTIONS: { id: ZoomDensity; label: string }[] = [
  { id: 'years', label: 'Years' },
  { id: 'months', label: 'Months' },
  { id: 'days', label: 'Days' },
  { id: 'all', label: 'All' },
];

export const PhotosGrid: React.FC<PhotosGridProps> = ({
  items,
  density,
  onDensityChange,
  gridScale,
  onGridScaleChange,
  sortOrder,
  onSortOrderChange,
  favoritesOnly,
  onToggleFavoritesOnly,
  isSelectMode,
  onToggleSelectMode,
  selectedIds,
  onToggleSelect,
  onSelectGroup,
  onLongPressSelect,
  onOpenItem,
  onOpenImport,
}) => {
  const filteredItems = useMemo(() => {
    const base = favoritesOnly ? items.filter((i) => i.isFavorite) : [...items];
    if (sortOrder === 'oldest') {
      base.sort((a, b) => a.createdAt - b.createdAt);
    } else if (sortOrder === 'largest') {
      base.sort((a, b) => (b.exif.fileSizeBytes || 0) - (a.exif.fileSizeBytes || 0));
    } else {
      base.sort((a, b) => b.createdAt - a.createdAt);
    }
    return base;
  }, [items, favoritesOnly, sortOrder]);

  const groups = useMemo(
    () =>
      sortOrder === 'largest'
        ? [
            {
              key: 'largest',
              label: 'Largest Files',
              sublabel: `${filteredItems.length} items`,
              items: filteredItems,
            },
          ]
        : groupMediaByDensity(filteredItems, density),
    [filteredItems, density, sortOrder]
  );

  const gridColsClass = useMemo(() => {
    if (gridScale === 'compact') {
      return 'grid-cols-4 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-8 gap-0.5 md:gap-1.5';
    }
    if (gridScale === 'large') {
      return 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-1.5 md:gap-3';
    }
    return 'grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-1 md:gap-2';
  }, [gridScale]);

  const cycleGridScale = () => {
    const order: ('compact' | 'standard' | 'large')[] = ['standard', 'compact', 'large'];
    const next = order[(order.indexOf(gridScale) + 1) % order.length];
    onGridScaleChange(next);
  };

  const cycleSort = () => {
    const order: SortOrder[] = ['newest', 'oldest', 'largest'];
    onSortOrderChange(order[(order.indexOf(sortOrder) + 1) % order.length]);
  };

  return (
    <section className="max-w-7xl mx-auto px-3 sm:px-4 md:px-6 pt-3 pb-28 md:pb-14">
      {/* Minimal Photo-First Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-4">
        <div className="flex items-center gap-3 min-w-0">
          <div
            role="tablist"
            aria-label="Timeline zoom density"
            className="inline-flex items-center p-1 rounded-xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/70 dark:border-zinc-800"
          >
            {DENSITY_OPTIONS.map((opt) => {
              const active = density === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => onDensityChange(opt.id)}
                  className={`min-h-[34px] px-3 py-1 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 transition ${
                    active
                      ? 'bg-white dark:bg-zinc-800 text-zinc-950 dark:text-white shadow-sm'
                      : 'text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-200'
                  }`}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>

          <span className="hidden sm:inline text-xs text-zinc-500 dark:text-zinc-400 font-mono-exif whitespace-nowrap">
            {filteredItems.length} {filteredItems.length === 1 ? 'item' : 'items'}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={onToggleFavoritesOnly}
            aria-pressed={favoritesOnly}
            title="Favorites filter"
            className={`min-h-[40px] min-w-[40px] px-2.5 rounded-xl border text-xs font-medium flex items-center justify-center gap-1.5 whitespace-nowrap shrink-0 transition ${
              favoritesOnly
                ? 'bg-[var(--accent-soft)] border-[var(--accent)] text-[var(--accent)]'
                : 'border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-900'
            }`}
          >
            <HeartFillIcon className="w-4 h-4" />
            <span className="hidden lg:inline">Favorites</span>
          </button>

          {(density === 'days' || density === 'all') && (
            <button
              type="button"
              onClick={cycleGridScale}
              title={`Grid size: ${gridScale}`}
              aria-label="Cycle grid density"
              className="hidden sm:flex min-h-[40px] px-3 rounded-xl border border-zinc-200 dark:border-zinc-800 text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-900 items-center gap-1.5 transition"
            >
              <SlidersFillIcon className="w-4 h-4" />
              <span className="capitalize">{gridScale}</span>
            </button>
          )}

          <button
            type="button"
            onClick={cycleSort}
            title="Change sort order"
            className="hidden md:flex min-h-[40px] px-3 rounded-xl border border-zinc-200 dark:border-zinc-800 text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-900 items-center whitespace-nowrap transition"
          >
            {sortOrder === 'newest' ? 'Newest' : sortOrder === 'oldest' ? 'Oldest' : 'Largest'}
          </button>

          <button
            type="button"
            onClick={onToggleSelectMode}
            aria-pressed={isSelectMode}
            className={`min-h-[40px] px-3 rounded-xl border text-xs font-medium flex items-center gap-1.5 whitespace-nowrap shrink-0 transition ${
              isSelectMode
                ? 'bg-[var(--accent)] border-[var(--accent)] text-white'
                : 'border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900'
            }`}
          >
            <CheckCircleFillIcon className="w-4 h-4" />
            <span>{isSelectMode ? 'Done' : 'Select'}</span>
          </button>
        </div>
      </div>

      {/* Empty State */}
      {groups.length === 0 && (
        <div className="my-12 rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 p-10 text-center max-w-lg mx-auto">
          <PhotosFillIcon className="w-10 h-10 mx-auto text-[var(--accent)] mb-3" />
          <h2 className="text-lg font-bold text-zinc-950 dark:text-white">
            {favoritesOnly ? 'No favorite photos yet' : 'Your gallery is empty'}
          </h2>
          <p className="mt-1.5 text-sm text-zinc-500 dark:text-zinc-400">
            {favoritesOnly
              ? 'Tap the heart on any photo or video to see it here.'
              : 'Link your Motorola folder once or add photos from device storage.'}
          </p>
          {!favoritesOnly && (
            <button
              type="button"
              onClick={onOpenImport}
              className="mt-5 min-h-[44px] px-5 py-2.5 rounded-full bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white text-sm font-medium transition"
            >
              Add Photos
            </button>
          )}
        </div>
      )}

      {/* Years / Months Editorial View */}
      {groups.length > 0 && (density === 'years' || density === 'months') && sortOrder !== 'largest' && (
        <div
          className={
            density === 'years'
              ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4'
              : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
          }
        >
          {groups.map((group) => {
            const cover = group.items[0];
            return (
              <button
                key={group.key}
                type="button"
                onClick={() => onDensityChange(density === 'years' ? 'months' : 'days')}
                className="group relative aspect-[4/3] rounded-2xl overflow-hidden bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]"
              >
                {cover.mediaType === 'video' ? (
                  <video
                    src={cover.src}
                    muted
                    playsInline
                    className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-300"
                  />
                ) : (
                  <img
                    src={cover.src}
                    alt={group.label}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-300"
                  />
                )}
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/25 to-transparent" />
                <div className="absolute bottom-4 left-4 right-4 flex items-end justify-between gap-3">
                  <div>
                    <h3 className="text-xl md:text-2xl font-bold text-white tracking-tight">
                      {group.label}
                    </h3>
                    <p className="text-xs text-zinc-200/90 mt-0.5 font-mono-exif">
                      {group.items.length} {group.items.length === 1 ? 'photo' : 'photos'}
                      {group.sublabel ? ` · ${group.sublabel}` : ''}
                    </p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}

      {/* Days / All Photos Crisp Matrix Grid */}
      {groups.length > 0 && (density === 'days' || density === 'all' || sortOrder === 'largest') && (
        <div className="space-y-6">
          {groups.map((group) => {
            const groupIds = group.items.map((i) => i.id);
            const allSelected =
              groupIds.length > 0 && groupIds.every((id) => selectedIds.has(id));

            return (
              <div key={group.key}>
                <div className="sticky top-[52px] md:top-16 z-20 -mx-3 sm:-mx-4 md:-mx-6 px-3 sm:px-4 md:px-6 py-2 bg-white/85 dark:bg-black/85 backdrop-blur-md flex items-center justify-between gap-3">
                  <div className="flex items-baseline gap-2 min-w-0">
                    <h2 className="text-sm md:text-base font-bold text-zinc-950 dark:text-white truncate">
                      {group.label}
                    </h2>
                    {group.sublabel && (
                      <span className="text-xs text-zinc-500 dark:text-zinc-400 truncate">
                        · {group.sublabel}
                      </span>
                    )}
                  </div>

                  {isSelectMode && (
                    <button
                      type="button"
                      onClick={() => onSelectGroup(groupIds)}
                      className="min-h-[32px] px-2.5 rounded-lg text-xs font-medium text-[var(--accent)] hover:bg-[var(--accent-soft)] whitespace-nowrap shrink-0 transition"
                    >
                      {allSelected ? 'Deselect all' : 'Select all'}
                    </button>
                  )}
                </div>

                <div className={`grid ${gridColsClass} mt-1`}>
                  {group.items.map((item) => (
                    <MediaThumb
                      key={item.id}
                      item={item}
                      isSelectMode={isSelectMode}
                      isSelected={selectedIds.has(item.id)}
                      onOpen={onOpenItem}
                      onToggleSelect={onToggleSelect}
                      onLongPressSelect={onLongPressSelect}
                    />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
};
