import React, { useMemo, useState } from 'react';
import { Album, MediaItem } from '../types/gallery';
import {
  ArrowLeftFillIcon,
  ChevronRightFillIcon,
  FilmFillIcon,
  FolderPlusFillIcon,
  HeartFillIcon,
  LockFillIcon,
  PaletteFillIcon,
  PhotosFillIcon,
  SlidersFillIcon,
  TrashFillIcon,
} from './FilledIcons';
import { MediaThumb } from './MediaThumb';

type ActiveCollectionFilter =
  | { type: 'album'; album: Album }
  | { type: 'favorites' }
  | { type: 'videos' }
  | { type: 'edited' }
  | { type: 'locked' }
  | { type: 'trash' };

interface AlbumsViewProps {
  media: MediaItem[];
  trashedMedia: MediaItem[];
  lockedMedia: MediaItem[];
  albums: Album[];
  isVaultUnlocked: boolean;
  onRequestVaultUnlock: (onSuccess: () => void) => void;
  onCreateAlbum: (name: string) => void;
  onDeleteAlbum: (albumId: string) => void;
  onOpenItem: (item: MediaItem) => void;
  isSelectMode: boolean;
  onToggleSelectMode: () => void;
  selectedIds: Set<string>;
  onToggleSelect: (id: string) => void;
  onLongPressSelect: (id: string) => void;
  onEmptyTrash: () => void;
  onOpenThemesAndQoL: () => void;
  onContextTrashChange?: (isTrash: boolean, isLocked: boolean) => void;
}

export const AlbumsView: React.FC<AlbumsViewProps> = ({
  media,
  trashedMedia,
  lockedMedia,
  albums,
  isVaultUnlocked,
  onRequestVaultUnlock,
  onCreateAlbum,
  onDeleteAlbum,
  onOpenItem,
  isSelectMode,
  onToggleSelectMode,
  selectedIds,
  onToggleSelect,
  onLongPressSelect,
  onEmptyTrash,
  onOpenThemesAndQoL,
  onContextTrashChange,
}) => {
  const [activeFilter, setActiveFilter] = useState<ActiveCollectionFilter | null>(null);
  const [newAlbumOpen, setNewAlbumOpen] = useState(false);
  const [albumName, setAlbumName] = useState('');

  const openFilter = (filter: ActiveCollectionFilter) => {
    setActiveFilter(filter);
    onContextTrashChange?.(filter.type === 'trash', filter.type === 'locked');
  };

  const closeFilter = () => {
    setActiveFilter(null);
    onContextTrashChange?.(false, false);
  };

  const filteredItems = useMemo(() => {
    if (!activeFilter) return [];
    switch (activeFilter.type) {
      case 'album':
        return media.filter((m) => m.albumIds.includes(activeFilter.album.id));
      case 'favorites':
        return media.filter((m) => m.isFavorite);
      case 'videos':
        return media.filter((m) => m.mediaType === 'video');
      case 'edited':
        return media.filter((m) => m.isEdited);
      case 'locked':
        return lockedMedia;
      case 'trash':
        return trashedMedia;
    }
  }, [activeFilter, media, lockedMedia, trashedMedia]);

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = albumName.trim();
    if (!trimmed) return;
    onCreateAlbum(trimmed);
    setAlbumName('');
    setNewAlbumOpen(false);
  };

  if (activeFilter) {
    const title =
      activeFilter.type === 'album'
        ? activeFilter.album.name
        : activeFilter.type === 'favorites'
        ? 'Favorites'
        : activeFilter.type === 'videos'
        ? 'Videos'
        : activeFilter.type === 'edited'
        ? 'Edited Photos'
        : activeFilter.type === 'locked'
        ? 'Locked Vault'
        : 'Recently Deleted';

    return (
      <section className="max-w-7xl mx-auto px-3 sm:px-4 md:px-6 pt-3 pb-28 md:pb-14">
        <div className="flex items-center justify-between gap-3 pb-4">
          <div className="flex items-center gap-2 min-w-0">
            <button
              type="button"
              onClick={closeFilter}
              aria-label="Back to Albums"
              className="min-h-[44px] min-w-[44px] -ml-2 flex items-center justify-center rounded-xl text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition"
            >
              <ArrowLeftFillIcon className="w-6 h-6" />
            </button>
            <div className="min-w-0">
              <h1 className="text-lg md:text-xl font-bold text-zinc-950 dark:text-white truncate">
                {title}
              </h1>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 font-mono-exif">
                {filteredItems.length} {filteredItems.length === 1 ? 'item' : 'items'}
                {activeFilter.type === 'trash' ? ' · Kept for 30 days' : ''}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {activeFilter.type === 'trash' && filteredItems.length > 0 && (
              <button
                type="button"
                onClick={onEmptyTrash}
                className="min-h-[40px] px-3 rounded-xl border border-red-200 dark:border-red-900/60 text-red-600 dark:text-red-400 text-xs font-medium hover:bg-red-50 dark:hover:bg-red-950/40 transition"
              >
                Empty Trash
              </button>
            )}
            {activeFilter.type === 'album' && !activeFilter.album.isSystem && (
              <button
                type="button"
                onClick={() => {
                  onDeleteAlbum(activeFilter.album.id);
                  closeFilter();
                }}
                className="min-h-[40px] px-3 rounded-xl border border-zinc-200 dark:border-zinc-800 text-zinc-600 dark:text-zinc-300 text-xs font-medium hover:bg-zinc-100 dark:hover:bg-zinc-900 transition"
              >
                Delete Album
              </button>
            )}
            {filteredItems.length > 0 && (
              <button
                type="button"
                onClick={onToggleSelectMode}
                className={`min-h-[40px] px-3 rounded-xl border text-xs font-medium transition ${
                  isSelectMode
                    ? 'bg-[var(--accent)] border-[var(--accent)] text-white'
                    : 'border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900'
                }`}
              >
                {isSelectMode ? 'Done' : 'Select'}
              </button>
            )}
          </div>
        </div>

        {filteredItems.length === 0 ? (
          <div className="my-10 rounded-2xl border border-zinc-200 dark:border-zinc-800 p-10 text-center max-w-md mx-auto">
            <PhotosFillIcon className="w-8 h-8 mx-auto text-zinc-400 mb-2" />
            <p className="text-sm font-medium text-zinc-800 dark:text-zinc-200">
              No items in {title}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-1 md:gap-2">
            {filteredItems.map((item) => (
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
        )}
      </section>
    );
  }

  const favoritesCount = media.filter((m) => m.isFavorite).length;
  const videosCount = media.filter((m) => m.mediaType === 'video').length;
  const editedCount = media.filter((m) => m.isEdited).length;

  return (
    <section className="max-w-7xl mx-auto px-4 md:px-6 pt-4 pb-28 md:pb-14 space-y-8">
      <div>
        <div className="flex items-center justify-between gap-4 mb-4">
          <div>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight text-zinc-950 dark:text-white">
              Albums
            </h1>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5 font-mono-exif">
              {albums.length} albums · {media.length} total items
            </p>
          </div>

          <button
            type="button"
            onClick={() => setNewAlbumOpen(true)}
            className="min-h-[40px] px-3.5 py-2 rounded-xl border border-zinc-200 dark:border-zinc-800 text-xs font-medium text-zinc-800 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900 flex items-center gap-1.5 whitespace-nowrap shrink-0 transition"
          >
            <FolderPlusFillIcon className="w-4 h-4 text-[var(--accent)]" />
            <span>New Album</span>
          </button>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {albums.map((album) => {
            const albumItems = media.filter((m) => m.albumIds.includes(album.id));
            const cover =
              (album.coverMediaId && albumItems.find((m) => m.id === album.coverMediaId)) ||
              albumItems[0];

            return (
              <button
                key={album.id}
                type="button"
                onClick={() => openFilter({ type: 'album', album })}
                className="group text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] rounded-2xl"
              >
                <div className="aspect-square rounded-2xl overflow-hidden bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800">
                  {cover ? (
                    cover.mediaType === 'video' ? (
                      <video
                        src={cover.src}
                        muted
                        playsInline
                        className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-200"
                      />
                    ) : (
                      <img
                        src={cover.src}
                        alt={album.name}
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-200"
                      />
                    )
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-400">
                      <PhotosFillIcon className="w-8 h-8 opacity-40" />
                    </div>
                  )}
                </div>
                <div className="mt-2 px-0.5">
                  <p className="text-sm font-medium text-zinc-950 dark:text-zinc-100 truncate">
                    {album.name}
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400 font-mono-exif">
                    {albumItems.length} {albumItems.length === 1 ? 'item' : 'items'}
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Smart Categories & Utilities */}
      <div className="max-w-3xl">
        <h2 className="text-sm font-bold text-zinc-500 dark:text-zinc-400 mb-2">
          Media Types & Utilities
        </h2>
        <div className="rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/70 dark:bg-zinc-950 divide-y divide-zinc-200/80 dark:divide-zinc-800/80 overflow-hidden">
          {[
            {
              key: 'favorites',
              label: 'Favorites',
              count: favoritesCount,
              icon: HeartFillIcon,
              iconClass: 'text-[var(--accent)]',
              onClick: () => openFilter({ type: 'favorites' }),
            },
            {
              key: 'videos',
              label: 'Videos',
              count: videosCount,
              icon: FilmFillIcon,
              iconClass: 'text-[var(--accent)]',
              onClick: () => openFilter({ type: 'videos' }),
            },
            {
              key: 'edited',
              label: 'Edited',
              count: editedCount,
              icon: SlidersFillIcon,
              iconClass: 'text-amber-500',
              onClick: () => openFilter({ type: 'edited' }),
            },
            {
              key: 'locked',
              label: 'Locked Vault',
              count: isVaultUnlocked ? lockedMedia.length : undefined,
              sub: isVaultUnlocked ? 'Unlocked' : 'PIN Protected',
              icon: LockFillIcon,
              iconClass: 'text-zinc-600 dark:text-zinc-300',
              onClick: () => {
                if (isVaultUnlocked) {
                  openFilter({ type: 'locked' });
                } else {
                  onRequestVaultUnlock(() => openFilter({ type: 'locked' }));
                }
              },
            },
            {
              key: 'trash',
              label: 'Recently Deleted',
              count: trashedMedia.length,
              icon: TrashFillIcon,
              iconClass: 'text-zinc-500 dark:text-zinc-400',
              onClick: () => openFilter({ type: 'trash' }),
            },
            {
              key: 'themes',
              label: 'Themes & Smart Tools',
              sub: 'Rose Gold, cleanup & folder sync',
              icon: PaletteFillIcon,
              iconClass: 'text-[var(--accent)]',
              onClick: onOpenThemesAndQoL,
            },
          ].map((row) => {
            const Icon = row.icon;
            return (
              <button
                key={row.key}
                type="button"
                onClick={row.onClick}
                className="w-full min-h-[58px] px-4 flex items-center justify-between gap-4 hover:bg-zinc-100/80 dark:hover:bg-zinc-900/70 transition text-left"
              >
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="w-9 h-9 rounded-full bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 flex items-center justify-center shrink-0">
                    <Icon className={`w-4 h-4 ${row.iconClass}`} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-zinc-950 dark:text-zinc-100 truncate">
                      {row.label}
                    </p>
                    {row.sub && (
                      <p className="text-xs text-zinc-500 dark:text-zinc-400">{row.sub}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0 text-zinc-500 dark:text-zinc-400">
                  {typeof row.count === 'number' && (
                    <span className="text-xs font-mono-exif">{row.count}</span>
                  )}
                  <ChevronRightFillIcon className="w-4 h-4 opacity-60" />
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Create Album Modal */}
      {newAlbumOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <form
            onSubmit={handleCreateSubmit}
            className="w-full max-w-sm rounded-2xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 p-6 shadow-2xl"
          >
            <h3 className="text-lg font-bold text-zinc-950 dark:text-white">
              Create New Album
            </h3>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              Organize favorite memories into custom albums.
            </p>
            <input
              type="text"
              autoFocus
              value={albumName}
              onChange={(e) => setAlbumName(e.target.value)}
              placeholder="Album title (e.g. Date Nights)"
              className="mt-4 w-full min-h-[44px] px-3.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-950 text-sm text-zinc-950 dark:text-white focus:outline-none focus:ring-2 focus:ring-[var(--accent)]"
            />
            <div className="mt-5 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setNewAlbumOpen(false);
                  setAlbumName('');
                }}
                className="min-h-[40px] px-4 rounded-xl text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!albumName.trim()}
                className="min-h-[40px] px-4 rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] disabled:opacity-40 text-white text-xs font-medium transition"
              >
                Create
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
};
