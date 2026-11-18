export type MediaType = 'image' | 'video';

export type ZoomDensity = 'years' | 'months' | 'days' | 'all';

export type NavTab = 'photos' | 'albums' | 'collections' | 'search';

export type ThemeId = 'rose-gold' | 'lavender' | 'champagne' | 'cobalt';

export type SortOrder = 'newest' | 'oldest' | 'largest';

export type FilterPresetId =
  | 'original'
  | 'rose-film'
  | 'soft-glow'
  | 'vivid'
  | 'warm'
  | 'dramatic'
  | 'mono'
  | 'noir';

export type CropAspect = 'original' | '1:1' | '4:3' | '3:4' | '16:9' | '9:16' | '9:20';

export interface PhotoAdjustments {
  exposure: number;
  contrast: number;
  saturation: number;
  warmth: number;
  vignette: number;
  rotation: number;
  flipH: boolean;
  flipV: boolean;
  filterId: FilterPresetId;
  cropAspect: CropAspect;
}

export interface ExifInfo {
  camera: string;
  lens: string;
  focalLength: string;
  aperture: string;
  shutter: string;
  iso: number;
  width: number;
  height: number;
  fileSizeBytes: number;
  locationName?: string;
}

export interface MediaItem {
  id: string;
  title: string;
  src: string;
  originalSrc?: string;
  mediaType: MediaType;
  durationSec?: number;
  createdAt: number;
  updatedAt: number;
  albumIds: string[];
  isFavorite: boolean;
  isLocked: boolean;
  isTrashed: boolean;
  trashedAt?: number;
  caption: string;
  tags: string[];
  exif: ExifInfo;
  adjustments: PhotoAdjustments;
  isEdited?: boolean;
  isSample?: boolean;
  sourceKey?: string;
}

export interface Album {
  id: string;
  name: string;
  createdAt: number;
  coverMediaId?: string;
  isSystem?: boolean;
}

export interface MemoryStory {
  id: string;
  title: string;
  subtitle: string;
  mediaIds: string[];
}

export const DEFAULT_ADJUSTMENTS: PhotoAdjustments = {
  exposure: 0,
  contrast: 0,
  saturation: 0,
  warmth: 0,
  vignette: 0,
  rotation: 0,
  flipH: false,
  flipV: false,
  filterId: 'original',
  cropAspect: 'original',
};

export const THEME_OPTIONS: { id: ThemeId; label: string; swatch: string }[] = [
  { id: 'rose-gold', label: 'Rose Gold', swatch: '#C87A7E' },
  { id: 'lavender', label: 'Lavender Silk', swatch: '#9676CC' },
  { id: 'champagne', label: 'Champagne', swatch: '#C29660' },
  { id: 'cobalt', label: 'Classic Blue', swatch: '#2563EB' },
];
