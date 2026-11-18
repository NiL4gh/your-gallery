/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AlbumPickerModal } from './components/AlbumPickerModal';
import { AlbumsView } from './components/AlbumsView';
import { BottomNav } from './components/BottomNav';
import { CollectionsView } from './components/CollectionsView';
import { CompareModal } from './components/CompareModal';
import { DeleteConfirmModal, SnoozeDuration } from './components/DeleteConfirmModal';
import { ImportModal } from './components/ImportModal';
import { LightboxViewer } from './components/LightboxViewer';
import { MemoryPlayerModal } from './components/MemoryPlayerModal';
import { PhotoEditorModal } from './components/PhotoEditorModal';
import { PhotosGrid } from './components/PhotosGrid';
import { SearchView } from './components/SearchView';
import { SelectionActionBar } from './components/SelectionActionBar';
import { ThemeAndQoLModal } from './components/ThemeAndQoLModal';
import { TopBar } from './components/TopBar';
import { VaultPinModal } from './components/VaultPinModal';
import { INITIAL_MEMORIES } from './data/sampleMedia';
import { useOnlineStatus } from './hooks/usePWAInstall';
import {
  Album,
  DEFAULT_ADJUSTMENTS,
  MediaItem,
  MemoryStory,
  NavTab,
  PhotoAdjustments,
  SortOrder,
  ThemeId,
  ZoomDensity,
} from './types/gallery';
import {
  clearSavedDirectoryHandle,
  collectFilesFromDirectory,
  deleteAlbumRecord,
  deleteMediaPermanently,
  filesToMediaItems,
  getMetaValue,
  getSavedDirectoryHandle,
  loadInitialGalleryState,
  putAlbum,
  putMediaItem,
  putMediaItems,
  saveDirectoryHandle,
  setMetaValue,
} from './utils/storage';

declare global {
  interface Window {
    showDirectoryPicker?: () => Promise<FileSystemDirectoryHandle>;
  }
}

export default function App() {
  const isOnline = useOnlineStatus();
  const [loading, setLoading] = useState(true);
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [activeTab, setActiveTab] = useState<NavTab>('photos');
  const [density, setDensity] = useState<ZoomDensity>('days');
  const [gridScale, setGridScale] = useState<'compact' | 'standard' | 'large'>('standard');
  const [sortOrder, setSortOrder] = useState<SortOrder>('newest');
  const [favoritesOnly, setFavoritesOnly] = useState(false);

  const [galleryTitle, setGalleryTitle] = useState<string>(() => {
    return localStorage.getItem('your-gallery-title') || 'Your Gallery';
  });

  const [isDark, setIsDark] = useState<boolean>(() => {
    const saved = localStorage.getItem('your-gallery-dark');
    return saved ? saved === 'dark' : true;
  });

  const [themeId, setThemeId] = useState<ThemeId>(() => {
    const saved = localStorage.getItem('your-gallery-theme') as ThemeId | null;
    return saved || 'rose-gold';
  });

  // Delete confirmation snooze timestamp (never permanent)
  const [deleteSnoozeUntil, setDeleteSnoozeUntil] = useState<number | null>(() => {
    const raw = localStorage.getItem('your-gallery-delete-snooze');
    if (!raw) return null;
    const ts = Number(raw);
    return ts > Date.now() ? ts : null;
  });
  const [pendingDeleteIds, setPendingDeleteIds] = useState<string[] | null>(null);
  const [pendingDeletePermanent, setPendingDeletePermanent] = useState(false);

  // Auto-populate phone folder handle state
  const [linkedFolderName, setLinkedFolderName] = useState<string | null>(null);
  const [needsFolderPermissionResume, setNeedsFolderPermissionResume] = useState(false);

  // Selection state
  const [isSelectMode, setIsSelectMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isTrashContext, setIsTrashContext] = useState(false);
  const [isLockedContext, setIsLockedContext] = useState(false);

  // Modals & Overlays
  const [lightboxList, setLightboxList] = useState<MediaItem[] | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState(0);
  const [editingItem, setEditingItem] = useState<MediaItem | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [themesModalOpen, setThemesModalOpen] = useState(false);
  const [albumPickerTargetIds, setAlbumPickerTargetIds] = useState<string[] | null>(null);
  const [activeMemory, setActiveMemory] = useState<MemoryStory | null>(null);
  const [compareIds, setCompareIds] = useState<string[] | null>(null);

  // Vault PIN state
  const [vaultPin, setVaultPin] = useState<string | null>(null);
  const [isVaultUnlocked, setIsVaultUnlocked] = useState(false);
  const [vaultPromptOpen, setVaultPromptOpen] = useState(false);
  const [pendingVaultAction, setPendingVaultAction] = useState<(() => void) | null>(null);

  const [toast, setToast] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast((curr) => (curr === msg ? null : curr)), 2800);
  }, []);

  // Sync Tailwind dark class & color theme attribute
  useEffect(() => {
    const root = document.documentElement;
    if (isDark) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    root.setAttribute('data-theme', themeId);
    localStorage.setItem('your-gallery-dark', isDark ? 'dark' : 'light');
    localStorage.setItem('your-gallery-theme', themeId);

    const themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) {
      themeMeta.setAttribute('content', isDark ? '#000000' : '#ffffff');
    }
  }, [isDark, themeId]);

  // Privacy QoL: auto-lock Vault when app goes to background
  useEffect(() => {
    const onVisibility = () => {
      if (document.hidden) {
        setIsVaultUnlocked(false);
      }
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  const syncFromHandle = useCallback(
    async (
      handle: FileSystemDirectoryHandle,
      currentMedia: MediaItem[],
      targetAlbumId = 'camera',
      silentIfEmpty = false
    ) => {
      const files = await collectFilesFromDirectory(handle);
      if (files.length === 0) {
        if (!silentIfEmpty) showToast('No photos or videos found in folder.');
        return;
      }
      const existingKeys = new Set(
        currentMedia.map((m) => m.sourceKey).filter((k): k is string => !!k)
      );
      const newItems = await filesToMediaItems(files, targetAlbumId, existingKeys);
      if (newItems.length > 0) {
        await putMediaItems(newItems);
        setMedia((prev) => [...newItems, ...prev].sort((a, b) => b.createdAt - a.createdAt));
        showToast(
          `Auto-synced ${newItems.length} new ${newItems.length === 1 ? 'photo' : 'photos'}`
        );
      } else if (!silentIfEmpty) {
        showToast('Gallery is already up to date');
      }
    },
    [showToast]
  );

  // Initial load + automatic folder rescan if permission already granted
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [{ media: loadedMedia, albums: loadedAlbums }, savedPin, savedHandle] =
        await Promise.all([
          loadInitialGalleryState(),
          getMetaValue<string>('vault_pin'),
          getSavedDirectoryHandle(),
        ]);
      if (cancelled) return;
      setMedia(loadedMedia);
      setAlbums(loadedAlbums);
      setVaultPin(savedPin);
      setLoading(false);

      if (savedHandle) {
        setLinkedFolderName(savedHandle.name);
        try {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const perm = await (savedHandle as any).queryPermission?.({ mode: 'read' });
          if (perm === 'granted') {
            await syncFromHandle(savedHandle, loadedMedia, 'camera', true);
          } else {
            setNeedsFolderPermissionResume(true);
          }
        } catch {
          setNeedsFolderPermissionResume(true);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [syncFromHandle]);

  const visibleMedia = useMemo(
    () => media.filter((m) => !m.isTrashed && !m.isLocked),
    [media]
  );
  const trashedMedia = useMemo(() => media.filter((m) => m.isTrashed), [media]);
  const lockedMedia = useMemo(
    () => media.filter((m) => !m.isTrashed && m.isLocked),
    [media]
  );

  const supportsFolderSync = typeof window !== 'undefined' && 'showDirectoryPicker' in window;

  const handleImportFiles = async (files: File[], targetAlbumId = 'camera') => {
    const existingKeys = new Set(
      media.map((m) => m.sourceKey).filter((k): k is string => !!k)
    );
    const newItems = await filesToMediaItems(files, targetAlbumId, existingKeys);
    if (newItems.length === 0) {
      showToast('All selected files are already in your gallery.');
      return;
    }
    await putMediaItems(newItems);
    setMedia((prev) => [...newItems, ...prev].sort((a, b) => b.createdAt - a.createdAt));
    showToast(`Added ${newItems.length} ${newItems.length === 1 ? 'item' : 'items'}`);
  };

  const handleSyncDeviceFolder = async (targetAlbumId = 'camera') => {
    if (!window.showDirectoryPicker) return;
    try {
      const dirHandle = await window.showDirectoryPicker();
      await saveDirectoryHandle(dirHandle);
      setLinkedFolderName(dirHandle.name);
      setNeedsFolderPermissionResume(false);
      await syncFromHandle(dirHandle, media, targetAlbumId, false);
    } catch (err) {
      if ((err as Error).name !== 'AbortError') {
        showToast('Could not read selected folder.');
      }
    }
  };

  const handleResumeFolderAutoSync = async () => {
    const handle = await getSavedDirectoryHandle();
    if (!handle) return;
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const perm = await (handle as any).requestPermission?.({ mode: 'read' });
      if (perm === 'granted') {
        setNeedsFolderPermissionResume(false);
        await syncFromHandle(handle, media, 'camera', false);
      }
    } catch {
      await handleSyncDeviceFolder('camera');
    }
  };

  const handleUnlinkFolder = async () => {
    await clearSavedDirectoryHandle();
    setLinkedFolderName(null);
    setNeedsFolderPermissionResume(false);
    showToast('Folder unlinked');
  };

  const openItemInLightbox = (item: MediaItem, list?: MediaItem[]) => {
    const sourceList =
      list ||
      (item.isTrashed ? trashedMedia : item.isLocked ? lockedMedia : visibleMedia);
    const idx = sourceList.findIndex((i) => i.id === item.id);
    setLightboxList(sourceList);
    setLightboxIndex(idx >= 0 ? idx : 0);
  };

  const toggleFavorite = async (id: string) => {
    let updated: MediaItem | undefined;
    setMedia((prev) =>
      prev.map((m) => {
        if (m.id === id) {
          updated = { ...m, isFavorite: !m.isFavorite, updatedAt: Date.now() };
          return updated;
        }
        return m;
      })
    );
    setLightboxList((prev) =>
      prev ? prev.map((m) => (m.id === id && updated ? updated : m)) : null
    );
    if (updated) await putMediaItem(updated);
  };

  const updateMediaDetails = async (
    id: string,
    patch: { caption?: string; tags?: string[]; albumIds?: string[] }
  ) => {
    let updated: MediaItem | undefined;
    setMedia((prev) =>
      prev.map((m) => {
        if (m.id === id) {
          updated = { ...m, ...patch, updatedAt: Date.now() };
          return updated;
        }
        return m;
      })
    );
    setLightboxList((prev) =>
      prev ? prev.map((m) => (m.id === id && updated ? updated : m)) : null
    );
    if (updated) await putMediaItem(updated);
  };

  const handleSaveEdit = async (
    itemId: string,
    newSrc: string,
    width: number,
    height: number,
    adjustments: PhotoAdjustments,
    asCopy: boolean
  ) => {
    const target = media.find((m) => m.id === itemId);
    if (!target) return;

    if (asCopy) {
      const copy: MediaItem = {
        ...target,
        id: `copy-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        title: `${target.title} (Edit)`,
        src: newSrc,
        originalSrc: target.originalSrc || target.src,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        adjustments,
        isEdited: true,
        exif: { ...target.exif, width, height },
        isSample: false,
      };
      await putMediaItem(copy);
      setMedia((prev) => [copy, ...prev]);
      showToast('Saved edited copy');
    } else {
      const updated: MediaItem = {
        ...target,
        src: newSrc,
        originalSrc: target.originalSrc || target.src,
        updatedAt: Date.now(),
        adjustments,
        isEdited: true,
        exif: { ...target.exif, width, height },
      };
      await putMediaItem(updated);
      setMedia((prev) => prev.map((m) => (m.id === itemId ? updated : m)));
      setLightboxList((prev) =>
        prev ? prev.map((m) => (m.id === itemId ? updated : m)) : null
      );
      showToast('Photo saved');
    }
  };

  const handleRevertOriginal = async (itemId: string) => {
    const target = media.find((m) => m.id === itemId);
    if (!target || !target.originalSrc) return;
    const reverted: MediaItem = {
      ...target,
      src: target.originalSrc,
      adjustments: { ...DEFAULT_ADJUSTMENTS },
      isEdited: false,
      updatedAt: Date.now(),
    };
    await putMediaItem(reverted);
    setMedia((prev) => prev.map((m) => (m.id === itemId ? reverted : m)));
    setLightboxList((prev) =>
      prev ? prev.map((m) => (m.id === itemId ? reverted : m)) : null
    );
    showToast('Reverted to original');
  };

  const executeTrashOrDeleteIds = async (ids: string[]) => {
    if (ids.length === 0) return;
    const idSet = new Set(ids);
    const itemsToProcess = media.filter((m) => idSet.has(m.id));
    const allAlreadyTrashed = itemsToProcess.every((m) => m.isTrashed);

    if (allAlreadyTrashed) {
      await deleteMediaPermanently(ids);
      setMedia((prev) => prev.filter((m) => !idSet.has(m.id)));
      setLightboxList((prev) => {
        if (!prev) return null;
        const remaining = prev.filter((m) => !idSet.has(m.id));
        return remaining.length ? remaining : null;
      });
      showToast(`Deleted ${ids.length} ${ids.length === 1 ? 'item' : 'items'} permanently`);
    } else {
      const updatedItems: MediaItem[] = [];
      setMedia((prev) =>
        prev.map((m) => {
          if (idSet.has(m.id)) {
            const u = { ...m, isTrashed: true, trashedAt: Date.now() };
            updatedItems.push(u);
            return u;
          }
          return m;
        })
      );
      await putMediaItems(updatedItems);
      setLightboxList((prev) => {
        if (!prev) return null;
        const remaining = prev.filter((m) => !idSet.has(m.id));
        return remaining.length ? remaining : null;
      });
      showToast(`Moved ${ids.length} ${ids.length === 1 ? 'item' : 'items'} to Trash`);
    }
    setSelectedIds(new Set());
    setIsSelectMode(false);
  };

  // Enforce Delete Confirmation with optional Timed Snooze (never permanent)
  const requestDeleteIds = (ids: string[]) => {
    if (ids.length === 0) return;
    const idSet = new Set(ids);
    const itemsToProcess = media.filter((m) => idSet.has(m.id));
    const allAlreadyTrashed = itemsToProcess.every((m) => m.isTrashed);
    const isSnoozed = !allAlreadyTrashed && deleteSnoozeUntil !== null && Date.now() < deleteSnoozeUntil;

    if (isSnoozed) {
      executeTrashOrDeleteIds(ids);
      return;
    }

    setPendingDeleteIds(ids);
    setPendingDeletePermanent(allAlreadyTrashed);
  };

  const handleConfirmDelete = async (snooze: SnoozeDuration) => {
    if (!pendingDeleteIds) return;
    const ids = pendingDeleteIds;
    setPendingDeleteIds(null);

    if (snooze !== 'none') {
      const now = Date.now();
      let until = now;
      if (snooze === '15m') until = now + 15 * 60 * 1000;
      else if (snooze === '1h') until = now + 60 * 60 * 1000;
      else if (snooze === 'today') {
        const end = new Date();
        end.setHours(23, 59, 59, 999);
        until = end.getTime();
      }
      setDeleteSnoozeUntil(until);
      localStorage.setItem('your-gallery-delete-snooze', String(until));
    }

    await executeTrashOrDeleteIds(ids);
  };

  const handleRestoreIds = async (ids: string[]) => {
    const idSet = new Set(ids);
    const updatedItems: MediaItem[] = [];
    setMedia((prev) =>
      prev.map((m) => {
        if (idSet.has(m.id)) {
          const u = { ...m, isTrashed: false, trashedAt: undefined };
          updatedItems.push(u);
          return u;
        }
        return m;
      })
    );
    await putMediaItems(updatedItems);
    setLightboxList((prev) => {
      if (!prev) return null;
      const remaining = prev.filter((m) => !idSet.has(m.id));
      return remaining.length ? remaining : null;
    });
    setSelectedIds(new Set());
    setIsSelectMode(false);
    showToast(`Restored ${ids.length} ${ids.length === 1 ? 'item' : 'items'}`);
  };

  const handleToggleLockIds = async (ids: string[]) => {
    const runLock = async () => {
      const idSet = new Set(ids);
      const updatedItems: MediaItem[] = [];
      setMedia((prev) =>
        prev.map((m) => {
          if (idSet.has(m.id)) {
            const u = { ...m, isLocked: !m.isLocked, updatedAt: Date.now() };
            updatedItems.push(u);
            return u;
          }
          return m;
        })
      );
      await putMediaItems(updatedItems);
      setLightboxList((prev) => {
        if (!prev) return null;
        const remaining = prev.filter((m) => !idSet.has(m.id));
        return remaining.length ? remaining : null;
      });
      setSelectedIds(new Set());
      setIsSelectMode(false);
      const lockedNow = updatedItems[0]?.isLocked;
      showToast(
        lockedNow
          ? `Moved ${ids.length} ${ids.length === 1 ? 'item' : 'items'} to Locked Vault`
          : `Removed ${ids.length} ${ids.length === 1 ? 'item' : 'items'} from Vault`
      );
    };

    if (!vaultPin) {
      setPendingVaultAction(() => runLock);
      setVaultPromptOpen(true);
    } else {
      await runLock();
    }
  };

  const handleCreateAlbum = async (name: string): Promise<Album> => {
    const album: Album = {
      id: `album-${Date.now()}`,
      name,
      createdAt: Date.now(),
    };
    await putAlbum(album);
    setAlbums((prev) => [...prev, album]);
    showToast(`Created album "${name}"`);
    return album;
  };

  const handleDeleteAlbum = async (albumId: string) => {
    await deleteAlbumRecord(albumId);
    setAlbums((prev) => prev.filter((a) => a.id !== albumId));
    const updated: MediaItem[] = [];
    setMedia((prev) =>
      prev.map((m) => {
        if (m.albumIds.includes(albumId)) {
          const u = { ...m, albumIds: m.albumIds.filter((id) => id !== albumId) };
          updated.push(u);
          return u;
        }
        return m;
      })
    );
    await putMediaItems(updated);
    showToast('Album removed');
  };

  const toggleSelectId = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleLongPressSelect = (id: string) => {
    setIsSelectMode(true);
    setSelectedIds(new Set([id]));
  };

  const handleSelectGroup = (ids: string[]) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      const allIn = ids.every((id) => next.has(id));
      if (allIn) ids.forEach((id) => next.delete(id));
      else ids.forEach((id) => next.add(id));
      return next;
    });
  };

  const handleDownloadSelected = () => {
    const targets = media.filter((m) => selectedIds.has(m.id));
    targets.forEach((item) => {
      const a = document.createElement('a');
      a.href = item.src;
      a.download = `${item.title.replace(/\s+/g, '_').toLowerCase()}.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    });
  };

  const albumPickerCurrentIds = useMemo(() => {
    if (!albumPickerTargetIds || albumPickerTargetIds.length === 0) return [];
    const targetItems = media.filter((m) => albumPickerTargetIds.includes(m.id));
    if (targetItems.length === 0) return [];
    return albums
      .filter((a) => targetItems.every((m) => m.albumIds.includes(a.id)))
      .map((a) => a.id);
  }, [albumPickerTargetIds, media, albums]);

  return (
    <div
      className="min-h-screen bg-white text-zinc-950 dark:bg-black dark:text-zinc-50 transition-colors"
      onDragOver={(e) => e.preventDefault()}
      onDrop={(e) => {
        if (e.dataTransfer.files?.length) {
          e.preventDefault();
          handleImportFiles(Array.from(e.dataTransfer.files));
        }
      }}
    >
      <TopBar
        title={galleryTitle}
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setIsSelectMode(false);
          setSelectedIds(new Set());
          setIsTrashContext(false);
          setIsLockedContext(false);
        }}
        onOpenImport={() => setImportOpen(true)}
        onOpenThemesAndQoL={() => setThemesModalOpen(true)}
        needsFolderPermissionResume={needsFolderPermissionResume}
        onResumeFolderAutoSync={handleResumeFolderAutoSync}
      />

      {loading ? (
        <div className="max-w-7xl mx-auto px-4 md:px-6 py-8 grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="aspect-square rounded-lg bg-zinc-100 dark:bg-zinc-900 animate-pulse"
            />
          ))}
        </div>
      ) : (
        <main>
          {activeTab === 'photos' && (
            <PhotosGrid
              items={visibleMedia}
              density={density}
              onDensityChange={setDensity}
              gridScale={gridScale}
              onGridScaleChange={setGridScale}
              sortOrder={sortOrder}
              onSortOrderChange={setSortOrder}
              favoritesOnly={favoritesOnly}
              onToggleFavoritesOnly={() => setFavoritesOnly((f) => !f)}
              isSelectMode={isSelectMode}
              onToggleSelectMode={() => {
                setIsSelectMode((s) => !s);
                setSelectedIds(new Set());
              }}
              selectedIds={selectedIds}
              onToggleSelect={toggleSelectId}
              onSelectGroup={handleSelectGroup}
              onLongPressSelect={handleLongPressSelect}
              onOpenItem={(item) =>
                openItemInLightbox(
                  item,
                  favoritesOnly ? visibleMedia.filter((m) => m.isFavorite) : visibleMedia
                )
              }
              onOpenImport={() => setImportOpen(true)}
            />
          )}

          {activeTab === 'albums' && (
            <AlbumsView
              media={visibleMedia}
              trashedMedia={trashedMedia}
              lockedMedia={lockedMedia}
              albums={albums}
              isVaultUnlocked={isVaultUnlocked}
              onRequestVaultUnlock={(onSuccess) => {
                setPendingVaultAction(() => onSuccess);
                setVaultPromptOpen(true);
              }}
              onCreateAlbum={handleCreateAlbum}
              onDeleteAlbum={handleDeleteAlbum}
              onOpenItem={(item) => openItemInLightbox(item)}
              isSelectMode={isSelectMode}
              onToggleSelectMode={() => {
                setIsSelectMode((s) => !s);
                setSelectedIds(new Set());
              }}
              selectedIds={selectedIds}
              onToggleSelect={toggleSelectId}
              onLongPressSelect={handleLongPressSelect}
              onEmptyTrash={() => requestDeleteIds(trashedMedia.map((m) => m.id))}
              onOpenThemesAndQoL={() => setThemesModalOpen(true)}
              onContextTrashChange={(trash, locked) => {
                setIsTrashContext(trash);
                setIsLockedContext(locked);
              }}
            />
          )}

          {activeTab === 'collections' && (
            <CollectionsView
              media={visibleMedia}
              memories={INITIAL_MEMORIES}
              onPlayMemory={(mem) => setActiveMemory(mem)}
              onOpenItem={(item) => openItemInLightbox(item, visibleMedia)}
            />
          )}

          {activeTab === 'search' && (
            <SearchView
              media={visibleMedia}
              onOpenItem={(item) => openItemInLightbox(item, visibleMedia)}
            />
          )}
        </main>
      )}

      <BottomNav
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setIsSelectMode(false);
          setSelectedIds(new Set());
          setIsTrashContext(false);
          setIsLockedContext(false);
        }}
      />

      <SelectionActionBar
        selectedCount={selectedIds.size}
        isTrashContext={isTrashContext}
        isLockedContext={isLockedContext}
        onClear={() => {
          setSelectedIds(new Set());
          setIsSelectMode(false);
        }}
        onFavoriteAll={async () => {
          const ids = Array.from(selectedIds);
          const updated: MediaItem[] = [];
          setMedia((prev) =>
            prev.map((m) => {
              if (ids.includes(m.id)) {
                const u = { ...m, isFavorite: true };
                updated.push(u);
                return u;
              }
              return m;
            })
          );
          await putMediaItems(updated);
          setSelectedIds(new Set());
          setIsSelectMode(false);
          showToast(`Favorited ${ids.length} ${ids.length === 1 ? 'item' : 'items'}`);
        }}
        onAddToAlbum={() => setAlbumPickerTargetIds(Array.from(selectedIds))}
        onDownloadSelected={handleDownloadSelected}
        onToggleLock={() => handleToggleLockIds(Array.from(selectedIds))}
        onTrashOrDelete={() => requestDeleteIds(Array.from(selectedIds))}
        onRestoreSelected={() => handleRestoreIds(Array.from(selectedIds))}
        onCompareSelected={() => setCompareIds(Array.from(selectedIds))}
      />

      {compareIds && (
        <CompareModal
          items={visibleMedia.filter((m) => compareIds.includes(m.id) && m.mediaType === 'image')}
          onToggleFavorite={toggleFavorite}
          onTrashItem={(id) => {
            requestDeleteIds([id]);
            setCompareIds((prev) => {
              if (!prev) return null;
              const next = prev.filter((x) => x !== id);
              return next.length >= 2 ? next : null;
            });
          }}
          onClose={() => setCompareIds(null)}
        />
      )}

      {lightboxList && (
        <LightboxViewer
          items={lightboxList}
          currentIndex={lightboxIndex}
          albums={albums}
          onClose={() => setLightboxList(null)}
          onChangeIndex={setLightboxIndex}
          onToggleFavorite={toggleFavorite}
          onEdit={(item) => setEditingItem(item)}
          onTrash={(id) => requestDeleteIds([id])}
          onRestore={(id) => handleRestoreIds([id])}
          onToggleLock={(id) => handleToggleLockIds([id])}
          onUpdateDetails={updateMediaDetails}
          onOpenAlbumPicker={(ids) => setAlbumPickerTargetIds(ids)}
        />
      )}

      {editingItem && (
        <PhotoEditorModal
          item={editingItem}
          onClose={() => setEditingItem(null)}
          onSaveEdit={handleSaveEdit}
          onRevertOriginal={handleRevertOriginal}
        />
      )}

      {importOpen && (
        <ImportModal
          albums={albums}
          supportsFolderSync={supportsFolderSync}
          linkedFolderName={linkedFolderName}
          onClose={() => setImportOpen(false)}
          onImportFiles={handleImportFiles}
          onSyncFolder={handleSyncDeviceFolder}
        />
      )}

      {themesModalOpen && (
        <ThemeAndQoLModal
          galleryTitle={galleryTitle}
          onChangeGalleryTitle={(title) => {
            setGalleryTitle(title);
            localStorage.setItem('your-gallery-title', title);
          }}
          themeId={themeId}
          onChangeTheme={setThemeId}
          isDark={isDark}
          onToggleDark={() => setIsDark((d) => !d)}
          linkedFolderName={linkedFolderName}
          supportsFolderSync={supportsFolderSync}
          onLinkOrRescanFolder={
            linkedFolderName ? handleResumeFolderAutoSync : () => handleSyncDeviceFolder('camera')
          }
          onUnlinkFolder={handleUnlinkFolder}
          deleteSnoozeUntil={deleteSnoozeUntil}
          onResetDeleteSnooze={() => {
            setDeleteSnoozeUntil(null);
            localStorage.removeItem('your-gallery-delete-snooze');
            showToast('Delete confirmation enabled');
          }}
          media={visibleMedia}
          onQuickCleanScreenshots={(ids) => requestDeleteIds(ids)}
          onOpenItem={(item) => openItemInLightbox(item, visibleMedia)}
          onClose={() => setThemesModalOpen(false)}
        />
      )}

      {pendingDeleteIds && (
        <DeleteConfirmModal
          count={pendingDeleteIds.length}
          isPermanent={pendingDeletePermanent}
          onConfirm={handleConfirmDelete}
          onCancel={() => setPendingDeleteIds(null)}
        />
      )}

      {albumPickerTargetIds && (
        <AlbumPickerModal
          albums={albums}
          itemCount={albumPickerTargetIds.length}
          currentAlbumIds={albumPickerCurrentIds}
          onToggleAlbum={async (albumId) => {
            const targetSet = new Set(albumPickerTargetIds);
            const allHave = albumPickerCurrentIds.includes(albumId);
            const updated: MediaItem[] = [];
            setMedia((prev) =>
              prev.map((m) => {
                if (!targetSet.has(m.id)) return m;
                const nextAlbums = allHave
                  ? m.albumIds.filter((id) => id !== albumId)
                  : Array.from(new Set([...m.albumIds, albumId]));
                const u = { ...m, albumIds: nextAlbums };
                updated.push(u);
                return u;
              })
            );
            await putMediaItems(updated);
          }}
          onCreateAndAdd={async (name) => {
            const created = await handleCreateAlbum(name);
            const targetSet = new Set(albumPickerTargetIds);
            const updated: MediaItem[] = [];
            setMedia((prev) =>
              prev.map((m) => {
                if (!targetSet.has(m.id)) return m;
                const u = { ...m, albumIds: [...m.albumIds, created.id] };
                updated.push(u);
                return u;
              })
            );
            await putMediaItems(updated);
          }}
          onClose={() => setAlbumPickerTargetIds(null)}
        />
      )}

      {activeMemory && (
        <MemoryPlayerModal
          memory={activeMemory}
          items={visibleMedia.filter((m) => activeMemory.mediaIds.includes(m.id))}
          onClose={() => setActiveMemory(null)}
        />
      )}

      {vaultPromptOpen && (
        <VaultPinModal
          hasExistingPin={!!vaultPin}
          onVerifyPin={(entered) => {
            if (entered === vaultPin) {
              setIsVaultUnlocked(true);
              setVaultPromptOpen(false);
              pendingVaultAction?.();
              setPendingVaultAction(null);
              return true;
            }
            return false;
          }}
          onSetPin={async (newPin) => {
            await setMetaValue('vault_pin', newPin);
            setVaultPin(newPin);
            setIsVaultUnlocked(true);
            setVaultPromptOpen(false);
            pendingVaultAction?.();
            setPendingVaultAction(null);
            showToast('Vault PIN set');
          }}
          onClose={() => {
            setVaultPromptOpen(false);
            setPendingVaultAction(null);
          }}
        />
      )}

      {!isOnline && (
        <div className="fixed bottom-[70px] md:bottom-5 left-4 z-40 rounded-xl bg-amber-500 px-3 py-1.5 text-xs font-medium text-white shadow-lg">
          Offline Mode · Local Gallery Ready
        </div>
      )}

      {toast && (
        <div className="fixed bottom-[72px] md:bottom-6 right-4 z-50 rounded-xl bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-950 px-4 py-2.5 text-xs font-medium shadow-xl">
          {toast}
        </div>
      )}
    </div>
  );
}
