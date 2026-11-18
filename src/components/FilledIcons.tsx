import React from 'react';

interface IconProps {
  className?: string;
}

export const PhotosFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M5 4a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3H5Zm3.5 4.25a1.75 1.75 0 1 1 0 3.5 1.75 1.75 0 0 1 0-3.5ZM5.2 17.5l4.4-5.1a1 1 0 0 1 1.52.01l2.38 2.81 1.78-2.06a1 1 0 0 1 1.53.02l2.99 3.63a.75.75 0 0 1-.58 1.23H5.77a.75.75 0 0 1-.57-1.24Z" />
  </svg>
);

export const AlbumsFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M5 3.5A1.5 1.5 0 0 1 6.5 2h11A1.5 1.5 0 0 1 19 3.5V4H5v-.5ZM3.5 6A1.5 1.5 0 0 1 5 4.5h14A1.5 1.5 0 0 1 20.5 6v.5h-17V6ZM2 9a3 3 0 0 1 3-3h14a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V9Zm6.5 2a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3Zm-2.3 7h11.6a.75.75 0 0 0 .58-1.23l-3.1-3.75a1 1 0 0 0-1.53-.01l-1.95 2.29-1.72-2a1 1 0 0 0-1.53.02l-2.93 3.45A.75.75 0 0 0 6.2 18Z" />
  </svg>
);

export const MemoriesFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2.25a.75.75 0 0 1 .7.48l1.82 4.78 4.78 1.82a.75.75 0 0 1 0 1.4l-4.78 1.82-1.82 4.78a.75.75 0 0 1-1.4 0l-1.82-4.78-4.78-1.82a.75.75 0 0 1 0-1.4l4.78-1.82 1.82-4.78a.75.75 0 0 1 .7-.48Zm6.75 12.5a.65.65 0 0 1 .6.42l.75 1.98 1.98.75a.65.65 0 0 1 0 1.2l-1.98.75-.75 1.98a.65.65 0 0 1-1.2 0l-.75-1.98-1.98-.75a.65.65 0 0 1 0-1.2l1.98-.75.75-1.98a.65.65 0 0 1 .6-.42Z" />
  </svg>
);

export const SearchFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M10.5 3a7.5 7.5 0 1 0 4.65 13.38l4.23 4.24a1.1 1.1 0 0 0 1.56-1.56l-4.24-4.23A7.5 7.5 0 0 0 10.5 3Z" />
  </svg>
);

export const HeartFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35Z" />
  </svg>
);

export const TrashFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M9 3a1.5 1.5 0 0 0-1.41 1H4.75a.75.75 0 0 0 0 1.5h14.5a.75.75 0 0 0 0-1.5h-2.84A1.5 1.5 0 0 0 15 3H9ZM5.5 7h13l-.92 12.15A2.5 2.5 0 0 1 15.09 21.5H8.91a2.5 2.5 0 0 1-2.49-2.35L5.5 7Z" />
  </svg>
);

export const LockFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a5 5 0 0 0-5 5v2H6a3 3 0 0 0-3 3v7a3 3 0 0 0 3 3h12a3 3 0 0 0 3-3v-7a3 3 0 0 0-3-3h-1V7a5 5 0 0 0-5-5Zm-3 7V7a3 3 0 1 1 6 0v2H9Zm3 4.25a1.75 1.75 0 0 1 .75 3.33V18a.75.75 0 0 1-1.5 0v-1.42a1.75 1.75 0 0 1 .75-3.33Z" />
  </svg>
);

export const ShareFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92Z" />
  </svg>
);

export const EditFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25Zm17.71-10.21a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83Z" />
  </svg>
);

export const InfoFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.75 14.5a.75.75 0 0 1-1.5 0v-5.5a.75.75 0 0 1 1.5 0v5.5ZM12 8.75a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z" />
  </svg>
);

export const FolderPlusFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M10.4 4.5H4.5A2.5 2.5 0 0 0 2 7v10.5A2.5 2.5 0 0 0 4.5 20h15a2.5 2.5 0 0 0 2.5-2.5V9a2.5 2.5 0 0 0-2.5-2.5h-6.4l-1.9-1.67a1.5 1.5 0 0 0-.8-.33Zm2.35 5.75v2h2a.75.75 0 0 1 0 1.5h-2v2a.75.75 0 0 1-1.5 0v-2h-2a.75.75 0 0 1 0-1.5h2v-2a.75.75 0 0 1 1.5 0Z" />
  </svg>
);

export const FolderSyncFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M10.4 4.5H4.5A2.5 2.5 0 0 0 2 7v10.5A2.5 2.5 0 0 0 4.5 20h15a2.5 2.5 0 0 0 2.5-2.5V9a2.5 2.5 0 0 0-2.5-2.5h-6.4l-1.9-1.67a1.5 1.5 0 0 0-.8-.33Zm1.6 5.25a3.75 3.75 0 0 1 3.55 2.55.75.75 0 1 1-1.42.48 2.25 2.25 0 0 0-4.26-.02l.48.49a.5.5 0 0 1-.35.85H8.5a.5.5 0 0 1-.5-.5V12.1a.5.5 0 0 1 .85-.35l.33.33A3.75 3.75 0 0 1 12 9.75Z" />
  </svg>
);

export const DownloadFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 3a1 1 0 0 1 1 1v8.59l2.3-2.3a1 1 0 1 1 1.4 1.42l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 1 1 1.4-1.42l2.3 2.3V4a1 1 0 0 1 1-1ZM4.5 17a1 1 0 0 1 1 1v1h13v-1a1 1 0 1 1 2 0v1.5A1.5 1.5 0 0 1 19 21H5a1.5 1.5 0 0 1-1.5-1.5V18a1 1 0 0 1 1-1Z" />
  </svg>
);

export const PlusFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.85 5.5v3.65h3.65a.85.85 0 0 1 0 1.7h-3.65v3.65a.85.85 0 0 1-1.7 0v-3.65H7.5a.85.85 0 0 1 0-1.7h3.65V7.5a.85.85 0 0 1 1.7 0Z" />
  </svg>
);

export const CheckCircleFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm4.53 8.03-5.25 5.25a.75.75 0 0 1-1.06 0l-2.5-2.5a.75.75 0 1 1 1.06-1.06l1.97 1.97 4.72-4.72a.75.75 0 1 1 1.06 1.06Z" />
  </svg>
);

export const SunFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10Zm0-5a1 1 0 0 1 1 1v1.5a1 1 0 1 1-2 0V3a1 1 0 0 1 1-1Zm0 16.5a1 1 0 0 1 1 1V21a1 1 0 1 1-2 0v-1.5a1 1 0 0 1 1-1ZM4.22 4.22a1 1 0 0 1 1.42 0l1.06 1.06a1 1 0 1 1-1.42 1.42L4.22 5.64a1 1 0 0 1 0-1.42Zm13.08 13.08a1 1 0 0 1 1.42 0l1.06 1.06a1 1 0 0 1-1.42 1.42l-1.06-1.06a1 1 0 0 1 0-1.42ZM2 12a1 1 0 0 1 1-1h1.5a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1Zm16.5 0a1 1 0 0 1 1-1H21a1 1 0 1 1 0 2h-1.5a1 1 0 0 1-1-1ZM6.7 17.3a1 1 0 0 1 0 1.42l-1.06 1.06a1 1 0 1 1-1.42-1.42l1.06-1.06a1 1 0 0 1 1.42 0Zm13.08-13.08a1 1 0 0 1 0 1.42l-1.06 1.06a1 1 0 0 1-1.42-1.42l1.06-1.06a1 1 0 0 1 1.42 0Z" />
  </svg>
);

export const MoonFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
  </svg>
);

export const PaletteFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10c1.38 0 2.5-1.12 2.5-2.5 0-.61-.23-1.2-.64-1.67a.528.528 0 0 1-.13-.33c0-.28.22-.5.5-.5H16c3.31 0 6-2.69 6-6 0-4.96-4.49-9-10-9ZM6.5 13a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Z" />
  </svg>
);

export const FilmFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M4 4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H4Zm1.5 2.5h2v2h-2v-2Zm0 4.5h2v2h-2v-2Zm0 4.5h2v2h-2v-2Zm11-9h2v2h-2v-2Zm0 4.5h2v2h-2v-2Zm0 4.5h2v2h-2v-2Zm-6-7.25 4.2 2.7a.65.65 0 0 1 0 1.1l-4.2 2.7A.65.65 0 0 1 9.5 14.7V9.3a.65.65 0 0 1 1-.55Z" />
  </svg>
);

export const PlayFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M8 5.14v14l11-7-11-7Z" />
  </svg>
);

export const PauseFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M6 5a1.5 1.5 0 0 1 1.5-1.5h2A1.5 1.5 0 0 1 11 5v14a1.5 1.5 0 0 1-1.5 1.5h-2A1.5 1.5 0 0 1 6 19V5Zm7 0a1.5 1.5 0 0 1 1.5-1.5h2A1.5 1.5 0 0 1 18 5v14a1.5 1.5 0 0 1-1.5 1.5h-2A1.5 1.5 0 0 1 13 19V5Z" />
  </svg>
);

export const CameraFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M9.3 3.5a2 2 0 0 0-1.66.89L6.56 6H4a2.5 2.5 0 0 0-2.5 2.5v9A2.5 2.5 0 0 0 4 20h16a2.5 2.5 0 0 0 2.5-2.5v-9A2.5 2.5 0 0 0 20 6h-2.56l-1.08-1.61a2 2 0 0 0-1.66-.89H9.3ZM12 9a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z" />
  </svg>
);

export const PinFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a7 7 0 0 0-7 7c0 4.66 5.65 11.2 6.32 11.95a.9.9 0 0 0 1.36 0C13.35 20.2 19 13.66 19 9a7 7 0 0 0-7-7Zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5Z" />
  </svg>
);

export const RestoreFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 4a8 8 0 0 0-7.45 5.08H2.75a.5.5 0 0 0-.38.82l2.9 3.4a.5.5 0 0 0 .76 0l2.9-3.4a.5.5 0 0 0-.38-.82H6.74A6 6 0 1 1 6 12a1 1 0 1 0-2 0 8 8 0 1 0 8-8Z" />
  </svg>
);

export const CloseFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm3.53 12.47a.75.75 0 1 1-1.06 1.06L12 13.06l-2.47 2.47a.75.75 0 1 1-1.06-1.06L10.94 12 8.47 9.53a.75.75 0 1 1 1.06-1.06L12 10.94l2.47-2.47a.75.75 0 1 1 1.06 1.06L13.06 12l2.47 2.47Z" />
  </svg>
);

export const CleanSparkFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M9.5 3a1.5 1.5 0 0 0-1.5 1.5V10H5.5A1.5 1.5 0 0 0 4 11.5v1.25h16V11.5A1.5 1.5 0 0 0 18.5 10H16V4.5A1.5 1.5 0 0 0 14.5 3h-5ZM4 14.25v5.25A1.5 1.5 0 0 0 5.5 21h13a1.5 1.5 0 0 0 1.5-1.5v-5.25H4Z" />
  </svg>
);

export const SlidersFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M4 6.5a1 1 0 0 1 1-1h6.17a3 3 0 0 1 5.66 0H19a1 1 0 1 1 0 2h-2.17a3 3 0 0 1-5.66 0H5a1 1 0 0 1-1-1Zm0 5.5a1 1 0 0 1 1-1h2.17a3 3 0 0 1 5.66 0H19a1 1 0 1 1 0 2h-6.17a3 3 0 0 1-5.66 0H5a1 1 0 0 1-1-1Zm1 4.5a1 1 0 1 0 0 2h8.17a3 3 0 0 0 5.66 0H19a1 1 0 1 0 0-2h-.17a3 3 0 0 0-5.66 0H5Z" />
  </svg>
);

export const ArrowLeftFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm2.53 6.47a.75.75 0 0 1 0 1.06L11.56 12l2.97 2.47a.75.75 0 1 1-.96 1.15l-3.75-3.12a.75.75 0 0 1 0-1.15l3.75-3.12a.75.75 0 0 1 1.06.24Z" />
  </svg>
);

export const ChevronRightFillIcon: React.FC<IconProps> = ({ className = 'w-4 h-4' }) => (
  <svg viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden="true">
    <path d="M7.22 4.22a.75.75 0 0 1 1.06 0l5.25 5.25a.75.75 0 0 1 0 1.06l-5.25 5.25a.75.75 0 0 1-1.06-1.06L11.94 10 7.22 5.28a.75.75 0 0 1 0-1.06Z" />
  </svg>
);

export const MoreVertFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <circle cx="12" cy="5.5" r="2" />
    <circle cx="12" cy="12" r="2" />
    <circle cx="12" cy="18.5" r="2" />
  </svg>
);

export const CompareFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M11.25 3a.75.75 0 0 1 .75.75v16.5a.75.75 0 0 1-1.5 0V19H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5.5V3.75a.75.75 0 0 1 .75-.75ZM13.5 5H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-5.5V5Z" />
  </svg>
);

export const WallpaperFillIcon: React.FC<IconProps> = ({ className = 'w-5 h-5' }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M7 2a2.5 2.5 0 0 0-2.5 2.5v15A2.5 2.5 0 0 0 7 22h10a2.5 2.5 0 0 0 2.5-2.5v-15A2.5 2.5 0 0 0 17 2H7Zm5 3a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm-4.5 13 2.8-3.4a.8.8 0 0 1 1.24 0l1.56 1.9 1.2-1.4a.8.8 0 0 1 1.22 0l1.98 2.4a.6.6 0 0 1-.46.98H7.96a.6.6 0 0 1-.46-.98Z" />
  </svg>
);

