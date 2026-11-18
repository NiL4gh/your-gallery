import React, { useEffect, useRef, useState } from 'react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { NavTab } from '../types/gallery';
import {
  DownloadFillIcon,
  FolderSyncFillIcon,
  MoreVertFillIcon,
  PaletteFillIcon,
  PhotosFillIcon,
} from './FilledIcons';

interface TopBarProps {
  title?: string;
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  onOpenImport: () => void;
  onOpenThemesAndQoL: () => void;
  needsFolderPermissionResume: boolean;
  onResumeFolderAutoSync: () => void;
}

const NAV_ITEMS: { id: NavTab; label: string }[] = [
  { id: 'photos', label: 'Photos' },
  { id: 'albums', label: 'Albums' },
  { id: 'collections', label: 'Collections' },
  { id: 'search', label: 'Search' },
];

export const TopBar: React.FC<TopBarProps> = ({
  title,
  activeTab,
  onTabChange,
  onOpenImport,
  onOpenThemesAndQoL,
  needsFolderPermissionResume,
  onResumeFolderAutoSync,
}) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const { isInstallable, isInstalled, install } = usePWAInstall();

  useEffect(() => {
    if (!menuOpen) return;
    const onDown = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    window.addEventListener('mousedown', onDown);
    return () => window.removeEventListener('mousedown', onDown);
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-30 h-[52px] md:h-16 bg-white/85 dark:bg-black/85 backdrop-blur-xl border-b border-zinc-200/80 dark:border-zinc-800/80">
      <div className="max-w-7xl mx-auto h-full px-4 md:px-6 flex items-center justify-between gap-8">
        <button
          type="button"
          onClick={() => onTabChange('photos')}
          className="text-lg md:text-xl font-bold tracking-tight text-zinc-950 dark:text-white whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] rounded-md"
        >
          {title || 'Your Gallery'}
        </button>

        <nav
          aria-label="Primary navigation"
          className="hidden md:flex items-center gap-6 text-sm font-medium"
        >
          {NAV_ITEMS.map((item) => {
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onTabChange(item.id)}
                className={`py-1.5 whitespace-nowrap shrink-0 transition-colors border-b-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] ${
                  active
                    ? 'text-zinc-950 dark:text-white border-[var(--accent)]'
                    : 'text-zinc-500 dark:text-zinc-400 border-transparent hover:text-zinc-900 dark:hover:text-zinc-100'
                }`}
              >
                {item.label}
              </button>
            );
          })}
        </nav>

        <div className="relative shrink-0" ref={menuRef}>
          <button
            type="button"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label="Gallery menu, themes, and smart tools"
            aria-expanded={menuOpen}
            className="min-h-[42px] min-w-[42px] rounded-full flex items-center justify-center text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]"
          >
            <MoreVertFillIcon className="w-5 h-5" />
          </button>

          {menuOpen && (
            <div className="absolute right-0 mt-2 w-60 rounded-2xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 shadow-2xl py-1.5 z-50">
              <button
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  onOpenThemesAndQoL();
                }}
                className="w-full min-h-[44px] px-4 flex items-center gap-3 text-left text-sm font-medium text-zinc-900 dark:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition"
              >
                <PaletteFillIcon className="w-4 h-4 text-[var(--accent)] shrink-0" />
                <span>Themes &amp; Smart Tools</span>
              </button>

              {needsFolderPermissionResume && (
                <button
                  type="button"
                  onClick={() => {
                    setMenuOpen(false);
                    onResumeFolderAutoSync();
                  }}
                  className="w-full min-h-[44px] px-4 flex items-center gap-3 text-left text-sm font-medium text-[var(--accent)] hover:bg-zinc-100 dark:hover:bg-zinc-900 transition"
                >
                  <FolderSyncFillIcon className="w-4 h-4 shrink-0" />
                  <span>Resume Folder Sync</span>
                </button>
              )}

              <button
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  onOpenImport();
                }}
                className="w-full min-h-[44px] px-4 flex items-center gap-3 text-left text-sm font-medium text-zinc-900 dark:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition"
              >
                <PhotosFillIcon className="w-4 h-4 text-zinc-500 dark:text-zinc-400 shrink-0" />
                <span>Add or Link Photos</span>
              </button>

              {!isInstalled && isInstallable && (
                <button
                  type="button"
                  onClick={() => {
                    setMenuOpen(false);
                    install();
                  }}
                  className="w-full min-h-[44px] px-4 flex items-center gap-3 text-left text-sm font-medium text-zinc-900 dark:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-900 transition border-t border-zinc-100 dark:border-zinc-900 mt-1 pt-1"
                >
                  <DownloadFillIcon className="w-4 h-4 text-[var(--accent)] shrink-0" />
                  <span>Install Web App</span>
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
