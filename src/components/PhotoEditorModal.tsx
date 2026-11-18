import React, { useState } from 'react';
import { CropAspect, DEFAULT_ADJUSTMENTS, MediaItem, PhotoAdjustments } from '../types/gallery';
import { buildCssFilter, FILTER_PRESETS, renderEditedDataUrl } from '../utils/imageEditor';
import {
  CheckCircleFillIcon,
  CloseFillIcon,
  MemoriesFillIcon,
  PhotosFillIcon,
  RestoreFillIcon,
  SlidersFillIcon,
} from './FilledIcons';

interface PhotoEditorModalProps {
  item: MediaItem;
  onClose: () => void;
  onSaveEdit: (
    itemId: string,
    newSrc: string,
    width: number,
    height: number,
    adjustments: PhotoAdjustments,
    asCopy: boolean
  ) => void;
  onRevertOriginal: (itemId: string) => void;
}

type EditorTab = 'adjust' | 'filters' | 'crop';

const ASPECT_OPTIONS: { id: CropAspect; label: string }[] = [
  { id: 'original', label: 'Original' },
  { id: '9:20', label: 'Edge 70 Wallpaper' },
  { id: '1:1', label: '1:1' },
  { id: '4:3', label: '4:3' },
  { id: '3:4', label: '3:4' },
  { id: '16:9', label: '16:9' },
  { id: '9:16', label: '9:16' },
];

export const PhotoEditorModal: React.FC<PhotoEditorModalProps> = ({
  item,
  onClose,
  onSaveEdit,
  onRevertOriginal,
}) => {
  const baseSrc = item.originalSrc || item.src;
  const [adj, setAdj] = useState<PhotoAdjustments>({ ...item.adjustments });
  const [activeTab, setActiveTab] = useState<EditorTab>('adjust');
  const [comparingOriginal, setComparingOriginal] = useState(false);
  const [showLockPreview, setShowLockPreview] = useState(false);
  const [saving, setSaving] = useState(false);

  const previewFilter = comparingOriginal ? 'none' : buildCssFilter(adj);

  const previewTransform = comparingOriginal
    ? 'none'
    : `rotate(${adj.rotation}deg) scaleX(${adj.flipH ? -1 : 1}) scaleY(${adj.flipV ? -1 : 1})`;

  const aspectClass =
    adj.cropAspect === '1:1'
      ? 'aspect-square'
      : adj.cropAspect === '4:3'
      ? 'aspect-[4/3]'
      : adj.cropAspect === '3:4'
      ? 'aspect-[3/4]'
      : adj.cropAspect === '16:9'
      ? 'aspect-video'
      : adj.cropAspect === '9:16'
      ? 'aspect-[9/16]'
      : adj.cropAspect === '9:20'
      ? 'aspect-[9/20]'
      : '';

  const handleSave = async (asCopy: boolean) => {
    setSaving(true);
    try {
      const { dataUrl, width, height } = await renderEditedDataUrl(baseSrc, adj);
      onSaveEdit(item.id, dataUrl, width, height, adj, asCopy);
      onClose();
    } finally {
      setSaving(false);
    }
  };

  const updateField = <K extends keyof PhotoAdjustments>(key: K, val: PhotoAdjustments[K]) => {
    setAdj((prev) => ({ ...prev, [key]: val }));
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Edit photo"
      className="fixed inset-0 z-50 bg-black text-white flex flex-col select-none"
    >
      {/* Top Bar */}
      <div className="h-14 px-4 flex items-center justify-between border-b border-zinc-900">
        <button
          type="button"
          onClick={onClose}
          className="min-h-[40px] px-3 rounded-xl text-sm text-zinc-300 hover:text-white hover:bg-zinc-900 flex items-center gap-1.5 transition"
        >
          <CloseFillIcon className="w-4 h-4" />
          <span>Cancel</span>
        </button>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onPointerDown={() => setComparingOriginal(true)}
            onPointerUp={() => setComparingOriginal(false)}
            onPointerLeave={() => setComparingOriginal(false)}
            onPointerCancel={() => setComparingOriginal(false)}
            className="min-h-[38px] px-3 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-xs font-medium text-zinc-200 transition"
          >
            {comparingOriginal ? 'Original' : 'Hold to Compare'}
          </button>

          {item.isEdited && (
            <button
              type="button"
              onClick={() => {
                onRevertOriginal(item.id);
                onClose();
              }}
              className="min-h-[38px] px-3 rounded-xl text-xs font-medium text-amber-400 hover:bg-zinc-900 flex items-center gap-1 transition"
            >
              <RestoreFillIcon className="w-4 h-4" />
              <span className="hidden sm:inline">Revert</span>
            </button>
          )}
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            disabled={saving}
            onClick={() => handleSave(true)}
            className="min-h-[38px] px-3 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-xs font-medium text-zinc-200 disabled:opacity-50 transition"
          >
            Save Copy
          </button>
          <button
            type="button"
            disabled={saving}
            onClick={() => handleSave(false)}
            className="min-h-[38px] px-4 rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-xs font-semibold text-white flex items-center gap-1.5 disabled:opacity-50 transition"
          >
            <CheckCircleFillIcon className="w-4 h-4" />
            <span>{saving ? 'Saving...' : 'Done'}</span>
          </button>
        </div>
      </div>

      {/* Live Canvas Preview */}
      <div className="flex-1 flex items-center justify-center p-4 md:p-8 overflow-hidden relative">
        <div
          className={`relative max-w-full max-h-full overflow-hidden flex items-center justify-center ${aspectClass} ${
            adj.cropAspect === '9:20' && showLockPreview ? 'rounded-[28px] ring-2 ring-white/25' : ''
          }`}
        >
          <img
            src={baseSrc}
            alt={item.title}
            referrerPolicy="no-referrer"
            style={{
              filter: previewFilter,
              transform: previewTransform,
            }}
            className={`max-w-full max-h-[54vh] md:max-h-[60vh] transition-[filter,transform] duration-150 ${
              aspectClass ? 'w-full h-full object-cover' : 'object-contain'
            }`}
          />
          {!comparingOriginal && adj.vignette > 0 && (
            <div
              className="pointer-events-none absolute inset-0"
              style={{
                background: `radial-gradient(circle, transparent 40%, rgba(0,0,0,${
                  (adj.vignette / 100) * 0.72
                }) 100%)`,
              }}
            />
          )}
          {adj.cropAspect === '9:20' && showLockPreview && (
            <div className="pointer-events-none absolute inset-0 flex flex-col items-center pt-8 bg-gradient-to-b from-black/45 via-transparent to-black/30 text-white">
              <p className="text-[11px] font-medium tracking-wide opacity-90">Mon, Nov 16</p>
              <p className="text-4xl font-bold tracking-tight mt-0.5 drop-shadow">9:41</p>
            </div>
          )}
        </div>
      </div>

      {/* Editor Controls */}
      <div className="bg-zinc-950 border-t border-zinc-900 px-4 pt-4 pb-[max(16px,env(safe-area-inset-bottom))]">
        <div className="max-w-2xl mx-auto space-y-4">
          {activeTab === 'adjust' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3">
              {[
                { key: 'exposure' as const, label: 'Exposure', min: -50, max: 50 },
                { key: 'contrast' as const, label: 'Contrast', min: -50, max: 50 },
                { key: 'saturation' as const, label: 'Saturation', min: -50, max: 50 },
                { key: 'warmth' as const, label: 'Warmth', min: -50, max: 50 },
                { key: 'vignette' as const, label: 'Vignette', min: 0, max: 100 },
              ].map((ctrl) => (
                <label key={ctrl.key} className="block">
                  <div className="flex items-center justify-between text-xs mb-1">
                    <span className="text-zinc-300 font-medium">{ctrl.label}</span>
                    <span className="font-mono-exif text-zinc-400">{adj[ctrl.key]}</span>
                  </div>
                  <input
                    type="range"
                    min={ctrl.min}
                    max={ctrl.max}
                    value={adj[ctrl.key]}
                    onChange={(e) => updateField(ctrl.key, Number(e.target.value))}
                    className="w-full accent-[var(--accent)] h-1.5 bg-zinc-800 rounded-lg cursor-pointer"
                  />
                </label>
              ))}
              <div className="flex items-end">
                <button
                  type="button"
                  onClick={() => setAdj({ ...DEFAULT_ADJUSTMENTS })}
                  className="min-h-[36px] px-3 rounded-lg border border-zinc-800 text-xs text-zinc-400 hover:text-white hover:bg-zinc-900 transition"
                >
                  Reset All Sliders
                </button>
              </div>
            </div>
          )}

          {activeTab === 'filters' && (
            <div className="flex items-center gap-3 overflow-x-auto no-scrollbar pb-1">
              {FILTER_PRESETS.map((preset) => {
                const active = adj.filterId === preset.id;
                return (
                  <button
                    key={preset.id}
                    type="button"
                    onClick={() => updateField('filterId', preset.id)}
                    className="shrink-0 flex flex-col items-center gap-1.5"
                  >
                    <div
                      className={`w-16 h-16 rounded-xl overflow-hidden border-2 transition ${
                        active ? 'border-[var(--accent)] scale-105' : 'border-zinc-800 opacity-80'
                      }`}
                    >
                      <img
                        src={baseSrc}
                        alt={preset.label}
                        referrerPolicy="no-referrer"
                        style={{ filter: preset.cssFilter }}
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <span
                      className={`text-[11px] font-medium ${
                        active ? 'text-[var(--accent)]' : 'text-zinc-400'
                      }`}
                    >
                      {preset.label}
                    </span>
                  </button>
                );
              })}
            </div>
          )}

          {activeTab === 'crop' && (
            <div className="space-y-3">
              <div className="flex items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={() => updateField('rotation', (adj.rotation + 90) % 360)}
                  className="min-h-[40px] px-3.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-xs font-medium"
                >
                  Rotate 90°
                </button>
                <button
                  type="button"
                  onClick={() => updateField('flipH', !adj.flipH)}
                  className={`min-h-[40px] px-3.5 rounded-xl text-xs font-medium ${
                    adj.flipH ? 'bg-[var(--accent)] text-white' : 'bg-zinc-900 hover:bg-zinc-800'
                  }`}
                >
                  Flip H
                </button>
                <button
                  type="button"
                  onClick={() => updateField('flipV', !adj.flipV)}
                  className={`min-h-[40px] px-3.5 rounded-xl text-xs font-medium ${
                    adj.flipV ? 'bg-[var(--accent)] text-white' : 'bg-zinc-900 hover:bg-zinc-800'
                  }`}
                >
                  Flip V
                </button>
                {adj.cropAspect === '9:20' && (
                  <button
                    type="button"
                    onClick={() => setShowLockPreview((p) => !p)}
                    className={`min-h-[40px] px-3.5 rounded-xl text-xs font-medium ${
                      showLockPreview
                        ? 'bg-[var(--accent)] text-white'
                        : 'bg-zinc-900 hover:bg-zinc-800 text-zinc-200'
                    }`}
                  >
                    {showLockPreview ? 'Hide Clock' : 'Lock Preview'}
                  </button>
                )}
              </div>

              <div className="flex items-center justify-center gap-2 overflow-x-auto no-scrollbar">
                {ASPECT_OPTIONS.map((opt) => {
                  const active = adj.cropAspect === opt.id;
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => updateField('cropAspect', opt.id)}
                      className={`min-h-[36px] px-3 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 transition ${
                        active
                          ? 'bg-[var(--accent)] text-white'
                          : 'bg-zinc-900 text-zinc-400 hover:text-white'
                      }`}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Mode Tabs */}
          <div className="flex items-center justify-center gap-6 pt-2 border-t border-zinc-900">
            {[
              { id: 'adjust' as const, label: 'Adjust', icon: SlidersFillIcon },
              { id: 'filters' as const, label: 'Filters', icon: MemoriesFillIcon },
              { id: 'crop' as const, label: 'Crop & Rotate', icon: PhotosFillIcon },
            ].map((tab) => {
              const Icon = tab.icon;
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`min-h-[44px] px-3 flex items-center gap-1.5 text-xs font-medium transition ${
                    active ? 'text-[var(--accent)]' : 'text-zinc-400 hover:text-zinc-200'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
