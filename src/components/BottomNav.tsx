import React from 'react';
import { NavTab } from '../types/gallery';
import {
  AlbumsFillIcon,
  MemoriesFillIcon,
  PhotosFillIcon,
  SearchFillIcon,
} from './FilledIcons';

interface BottomNavProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

const TABS: { id: NavTab; label: string; icon: React.FC<{ className?: string }> }[] = [
  { id: 'photos', label: 'Photos', icon: PhotosFillIcon },
  { id: 'albums', label: 'Albums', icon: AlbumsFillIcon },
  { id: 'collections', label: 'Memories', icon: MemoriesFillIcon },
  { id: 'search', label: 'Search', icon: SearchFillIcon },
];

export const BottomNav: React.FC<BottomNavProps> = ({ activeTab, onTabChange }) => {
  return (
    <nav
      aria-label="Mobile bottom navigation"
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 h-[58px] bg-white/90 dark:bg-zinc-950/90 backdrop-blur-xl border-t border-zinc-200/80 dark:border-zinc-800/80 pb-[env(safe-area-inset-bottom)]"
    >
      <div className="grid grid-cols-4 h-full items-center max-w-lg mx-auto">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => onTabChange(tab.id)}
              className={`min-h-[44px] flex flex-col items-center justify-center gap-0.5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] ${
                active
                  ? 'text-[var(--accent)]'
                  : 'text-zinc-500 dark:text-zinc-400 hover:text-zinc-800 dark:hover:text-zinc-200'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[11px] font-medium tracking-tight leading-none whitespace-nowrap">
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
