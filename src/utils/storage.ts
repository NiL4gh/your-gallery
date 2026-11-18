import { INITIAL_ALBUMS, SAMPLE_MEDIA } from '../data/sampleMedia';
import { Album, DEFAULT_ADJUSTMENTS, MediaItem } from '../types/gallery';

const DB_NAME = 'your-gallery-db';
const DB_VERSION = 1;
const STORE_MEDIA = 'media';
const STORE_ALBUMS = 'albums';
const STORE_META = 'meta';

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_MEDIA)) {
        db.createObjectStore(STORE_MEDIA, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORE_ALBUMS)) {
        db.createObjectStore(STORE_ALBUMS, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORE_META)) {
        db.createObjectStore(STORE_META, { keyPath: 'key' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function getAllFromStore<T>(storeName: string): Promise<T[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly');
    const store = tx.objectStore(storeName);
    const req = store.getAll();
    req.onsuccess = () => resolve(req.result as T[]);
    req.onerror = () => reject(req.error);
  });
}

async function putManyInStore<T>(storeName: string, items: T[]): Promise<void> {
  if (items.length === 0) return;
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    items.forEach((item) => store.put(item));
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function putMediaItem(item: MediaItem): Promise<void> {
  await putManyInStore(STORE_MEDIA, [item]);
}

export async function putMediaItems(items: MediaItem[]): Promise<void> {
  await putManyInStore(STORE_MEDIA, items);
}

export async function deleteMediaPermanently(ids: string[]): Promise<void> {
  if (ids.length === 0) return;
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_MEDIA, 'readwrite');
    const store = tx.objectStore(STORE_MEDIA);
    ids.forEach((id) => store.delete(id));
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function putAlbum(album: Album): Promise<void> {
  await putManyInStore(STORE_ALBUMS, [album]);
}

export async function deleteAlbumRecord(id: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_ALBUMS, 'readwrite');
    tx.objectStore(STORE_ALBUMS).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getMetaValue<T>(key: string): Promise<T | null> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_META, 'readonly');
    const req = tx.objectStore(STORE_META).get(key);
    req.onsuccess = () => {
      const record = req.result as { key: string; value: T } | undefined;
      resolve(record ? record.value : null);
    };
    req.onerror = () => reject(req.error);
  });
}

export async function setMetaValue<T>(key: string, value: T): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_META, 'readwrite');
    tx.objectStore(STORE_META).put({ key, value });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function saveDirectoryHandle(handle: FileSystemDirectoryHandle): Promise<void> {
  await setMetaValue('linked_directory_handle', handle);
}

export async function getSavedDirectoryHandle(): Promise<FileSystemDirectoryHandle | null> {
  try {
    return await getMetaValue<FileSystemDirectoryHandle>('linked_directory_handle');
  } catch {
    return null;
  }
}

export async function clearSavedDirectoryHandle(): Promise<void> {
  await setMetaValue('linked_directory_handle', null);
}

export async function collectFilesFromDirectory(
  dirHandle: FileSystemDirectoryHandle,
  maxDepth = 2,
  maxFiles = 150
): Promise<File[]> {
  const files: File[] = [];

  async function walk(handle: FileSystemDirectoryHandle, depth: number) {
    if (files.length >= maxFiles) return;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    for await (const [, entry] of (handle as any).entries()) {
      if (files.length >= maxFiles) break;
      if (entry.kind === 'file') {
        const file: File = await entry.getFile();
        if (file.type.startsWith('image/') || file.type.startsWith('video/')) {
          files.push(file);
        }
      } else if (entry.kind === 'directory' && depth < maxDepth) {
        if (!entry.name.startsWith('.')) {
          await walk(entry, depth + 1);
        }
      }
    }
  }

  await walk(dirHandle, 0);
  return files;
}

export async function loadInitialGalleryState(): Promise<{
  media: MediaItem[];
  albums: Album[];
}> {
  try {
    const seeded = await getMetaValue<boolean>('seeded_v1');
    let media = await getAllFromStore<MediaItem>(STORE_MEDIA);
    let albums = await getAllFromStore<Album>(STORE_ALBUMS);

    if (!seeded && media.length === 0) {
      await putManyInStore(STORE_MEDIA, SAMPLE_MEDIA);
      await putManyInStore(STORE_ALBUMS, INITIAL_ALBUMS);
      await setMetaValue('seeded_v1', true);
      media = SAMPLE_MEDIA;
      albums = INITIAL_ALBUMS;
    }

    media.sort((a, b) => b.createdAt - a.createdAt);
    albums.sort((a, b) => (a.isSystem ? -1 : b.isSystem ? 1 : b.createdAt - a.createdAt));

    return { media, albums };
  } catch {
    return { media: SAMPLE_MEDIA, albums: INITIAL_ALBUMS };
  }
}

export function readFileAsDataURL(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

export function extractImageDimensions(src: string): Promise<{ width: number; height: number }> {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve({ width: img.naturalWidth || 1200, height: img.naturalHeight || 900 });
    img.onerror = () => resolve({ width: 1200, height: 900 });
    img.src = src;
  });
}

export function extractVideoMetadata(src: string): Promise<{ width: number; height: number; durationSec: number }> {
  return new Promise((resolve) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      resolve({
        width: video.videoWidth || 1920,
        height: video.videoHeight || 1080,
        durationSec: Math.round(video.duration || 0),
      });
    };
    video.onerror = () => resolve({ width: 1920, height: 1080, durationSec: 0 });
    video.src = src;
  });
}

export function buildSourceKey(file: File): string {
  return `${file.name}::${file.size}::${file.lastModified}`;
}

export async function filesToMediaItems(
  files: File[],
  targetAlbumId = 'camera',
  existingSourceKeys?: Set<string>
): Promise<MediaItem[]> {
  const results: MediaItem[] = [];

  for (const file of files) {
    const isVideo = file.type.startsWith('video/');
    const isImage = file.type.startsWith('image/');
    if (!isImage && !isVideo) continue;

    const sourceKey = buildSourceKey(file);
    if (existingSourceKeys && existingSourceKeys.has(sourceKey)) continue;

    const dataUrl = await readFileAsDataURL(file);
    let width = 1200;
    let height = 900;
    let durationSec: number | undefined;

    if (isVideo) {
      const meta = await extractVideoMetadata(dataUrl);
      width = meta.width;
      height = meta.height;
      durationSec = meta.durationSec;
    } else {
      const dims = await extractImageDimensions(dataUrl);
      width = dims.width;
      height = dims.height;
    }

    const baseName = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]+/g, ' ');
    const createdAt = file.lastModified || Date.now();
    const id = `local-${createdAt}-${Math.random().toString(36).slice(2, 8)}`;

    const isScreenshot = /screenshot|screen/i.test(file.name);
    const albumList = isScreenshot && targetAlbumId === 'camera' ? ['camera', 'screenshots'] : [targetAlbumId];

    results.push({
      id,
      title: baseName || 'Photo',
      src: dataUrl,
      originalSrc: dataUrl,
      mediaType: isVideo ? 'video' : 'image',
      durationSec,
      createdAt,
      updatedAt: Date.now(),
      albumIds: albumList,
      isFavorite: false,
      isLocked: false,
      isTrashed: false,
      caption: '',
      tags: isVideo ? ['video', 'phone'] : isScreenshot ? ['screenshot'] : ['phone', 'camera'],
      exif: {
        camera: 'Motorola Edge 70',
        lens: isVideo ? '4K Video Capture' : isScreenshot ? 'Screen Capture' : '50MP OIS Main',
        focalLength: '24mm',
        aperture: 'f/1.8',
        shutter: isVideo ? '60fps' : 'Auto',
        iso: 100,
        width,
        height,
        fileSizeBytes: file.size,
        locationName: 'Phone Storage',
      },
      adjustments: { ...DEFAULT_ADJUSTMENTS },
      isSample: false,
      sourceKey,
    });
  }

  return results;
}
