import { CropAspect, FilterPresetId, PhotoAdjustments } from '../types/gallery';

export interface FilterPreset {
  id: FilterPresetId;
  label: string;
  cssFilter: string;
}

export const FILTER_PRESETS: FilterPreset[] = [
  { id: 'original', label: 'Original', cssFilter: 'none' },
  { id: 'rose-film', label: 'Rose Film', cssFilter: 'sepia(0.14) saturate(1.18) brightness(1.04) contrast(1.04)' },
  { id: 'soft-glow', label: 'Soft Glow', cssFilter: 'brightness(1.07) contrast(0.95) saturate(1.12)' },
  { id: 'vivid', label: 'Vivid', cssFilter: 'saturate(1.32) contrast(1.08)' },
  { id: 'warm', label: 'Golden', cssFilter: 'sepia(0.18) saturate(1.18) brightness(1.03)' },
  { id: 'dramatic', label: 'Dramatic', cssFilter: 'contrast(1.26) saturate(0.86) brightness(0.96)' },
  { id: 'mono', label: 'Mono', cssFilter: 'grayscale(1) contrast(1.12)' },
  { id: 'noir', label: 'Noir', cssFilter: 'grayscale(1) contrast(1.38) brightness(0.92)' },
];

export function buildCssFilter(adj: PhotoAdjustments): string {
  const preset = FILTER_PRESETS.find((p) => p.id === adj.filterId);
  const brightness = 1 + adj.exposure / 100;
  const contrast = 1 + adj.contrast / 100;
  const saturate = 1 + adj.saturation / 100;
  const sepia = adj.warmth > 0 ? (adj.warmth / 100) * 0.45 : 0;
  const hueRotate = adj.warmth < 0 ? (adj.warmth / 50) * 14 : 0;

  const parts = [
    `brightness(${brightness.toFixed(2)})`,
    `contrast(${contrast.toFixed(2)})`,
    `saturate(${saturate.toFixed(2)})`,
  ];
  if (sepia > 0) parts.push(`sepia(${sepia.toFixed(2)})`);
  if (hueRotate !== 0) parts.push(`hue-rotate(${hueRotate.toFixed(1)}deg)`);
  if (preset && preset.cssFilter !== 'none') parts.push(preset.cssFilter);

  return parts.join(' ');
}

function getAspectRatioValue(aspect: CropAspect, imgW: number, imgH: number): number {
  switch (aspect) {
    case '1:1':
      return 1;
    case '4:3':
      return 4 / 3;
    case '3:4':
      return 3 / 4;
    case '16:9':
      return 16 / 9;
    case '9:16':
      return 9 / 16;
    case '9:20':
      return 9 / 20;
    default:
      return imgW / imgH;
  }
}

export async function renderEditedDataUrl(
  src: string,
  adj: PhotoAdjustments
): Promise<{ dataUrl: string; width: number; height: number }> {
  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('Failed to load image for editing'));
    image.src = src;
  });

  const rotated90 = adj.rotation === 90 || adj.rotation === 270;
  const orientedW = rotated90 ? img.naturalHeight : img.naturalWidth;
  const orientedH = rotated90 ? img.naturalWidth : img.naturalHeight;

  const targetRatio = getAspectRatioValue(adj.cropAspect, orientedW, orientedH);
  const currentRatio = orientedW / orientedH;

  let outW = orientedW;
  let outH = orientedH;
  if (adj.cropAspect !== 'original') {
    if (currentRatio > targetRatio) {
      outW = Math.round(orientedH * targetRatio);
    } else {
      outH = Math.round(orientedW / targetRatio);
    }
  }

  const maxEdge = 2400;
  const scale = Math.min(1, maxEdge / Math.max(outW, outH));
  const finalW = Math.max(1, Math.round(outW * scale));
  const finalH = Math.max(1, Math.round(outH * scale));

  const canvas = document.createElement('canvas');
  canvas.width = finalW;
  canvas.height = finalH;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Canvas 2D context unavailable');
  }

  ctx.save();
  ctx.filter = buildCssFilter(adj);
  ctx.translate(finalW / 2, finalH / 2);
  ctx.rotate((adj.rotation * Math.PI) / 180);
  ctx.scale(adj.flipH ? -1 : 1, adj.flipV ? -1 : 1);

  const drawW = img.naturalWidth * scale;
  const drawH = img.naturalHeight * scale;
  ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
  ctx.restore();

  if (adj.vignette > 0) {
    const strength = (adj.vignette / 100) * 0.72;
    const rad = Math.max(finalW, finalH) * 0.72;
    const grad = ctx.createRadialGradient(
      finalW / 2,
      finalH / 2,
      rad * 0.35,
      finalW / 2,
      finalH / 2,
      rad
    );
    grad.addColorStop(0, 'rgba(0,0,0,0)');
    grad.addColorStop(1, `rgba(0,0,0,${strength.toFixed(2)})`);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, finalW, finalH);
  }

  return {
    dataUrl: canvas.toDataURL('image/jpeg', 0.92),
    width: finalW,
    height: finalH,
  };
}

export async function extractDominantPalette(src: string): Promise<string[]> {
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('load failed'));
      image.src = src;
    });

    const canvas = document.createElement('canvas');
    canvas.width = 24;
    canvas.height = 24;
    const ctx = canvas.getContext('2d');
    if (!ctx) return [];
    ctx.drawImage(img, 0, 0, 24, 24);
    const data = ctx.getImageData(0, 0, 24, 24).data;

    const buckets = new Map<string, { r: number; g: number; b: number; count: number }>();
    for (let i = 0; i < data.length; i += 4) {
      const r = Math.round(data[i] / 48) * 48;
      const g = Math.round(data[i + 1] / 48) * 48;
      const b = Math.round(data[i + 2] / 48) * 48;
      const key = `${r},${g},${b}`;
      const existing = buckets.get(key);
      if (existing) {
        existing.count += 1;
      } else {
        buckets.set(key, { r: data[i], g: data[i + 1], b: data[i + 2], count: 1 });
      }
    }

    return Array.from(buckets.values())
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
      .map((c) => `#${[c.r, c.g, c.b].map((v) => v.toString(16).padStart(2, '0')).join('')}`);
  } catch {
    return [];
  }
}
