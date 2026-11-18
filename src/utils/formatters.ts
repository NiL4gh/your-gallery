import { MediaItem, ZoomDensity } from '../types/gallery';

export function formatBytes(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 KB';
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatDuration(seconds?: number): string {
  if (!seconds || seconds <= 0) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function formatFullDate(ts: number): string {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(ts));
}

export function formatTime(ts: number): string {
  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(ts));
}

export interface MediaGroup {
  key: string;
  label: string;
  sublabel: string;
  items: MediaItem[];
}

export function groupMediaByDensity(items: MediaItem[], density: ZoomDensity): MediaGroup[] {
  if (density === 'all') {
    return items.length
      ? [
          {
            key: 'all',
            label: 'All Photos',
            sublabel: `${items.length} ${items.length === 1 ? 'item' : 'items'}`,
            items,
          },
        ]
      : [];
  }

  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const yesterdayStart = todayStart - 86400000;

  const groups = new Map<string, MediaGroup>();

  for (const item of items) {
    const d = new Date(item.createdAt);
    let key = '';
    let label = '';
    let sublabel = '';

    if (density === 'years') {
      key = `${d.getFullYear()}`;
      label = `${d.getFullYear()}`;
    } else if (density === 'months') {
      key = `${d.getFullYear()}-${d.getMonth()}`;
      label = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(d);
    } else {
      key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
      if (item.createdAt >= todayStart) {
        label = 'Today';
      } else if (item.createdAt >= yesterdayStart) {
        label = 'Yesterday';
      } else {
        label = new Intl.DateTimeFormat('en-US', {
          weekday: 'short',
          month: 'short',
          day: 'numeric',
        }).format(d);
      }
      sublabel = item.exif.locationName || '';
    }

    const existing = groups.get(key);
    if (existing) {
      existing.items.push(item);
      if (!existing.sublabel && sublabel) existing.sublabel = sublabel;
    } else {
      groups.set(key, {
        key,
        label,
        sublabel,
        items: [item],
      });
    }
  }

  return Array.from(groups.values());
}
