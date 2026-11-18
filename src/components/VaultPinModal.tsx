import React, { useState } from 'react';
import { ArrowLeftFillIcon, CloseFillIcon, LockFillIcon } from './FilledIcons';

interface VaultPinModalProps {
  hasExistingPin: boolean;
  onVerifyPin: (pin: string) => boolean;
  onSetPin: (pin: string) => void;
  onClose: () => void;
}

export const VaultPinModal: React.FC<VaultPinModalProps> = ({
  hasExistingPin,
  onVerifyPin,
  onSetPin,
  onClose,
}) => {
  const [pin, setPin] = useState('');
  const [confirmPin, setConfirmPin] = useState<string | null>(null);
  const [error, setError] = useState('');

  const handleDigit = (digit: string) => {
    if (pin.length >= 4) return;
    const next = pin + digit;
    setError('');
    setPin(next);

    if (next.length === 4) {
      if (hasExistingPin) {
        const ok = onVerifyPin(next);
        if (!ok) {
          setError('Incorrect PIN. Try again.');
          setPin('');
        }
      } else {
        if (confirmPin === null) {
          setConfirmPin(next);
          setPin('');
        } else if (confirmPin === next) {
          onSetPin(next);
        } else {
          setError('PINs did not match. Start over.');
          setConfirmPin(null);
          setPin('');
        }
      }
    }
  };

  const title = hasExistingPin
    ? 'Unlock Vault'
    : confirmPin === null
    ? 'Set Vault PIN'
    : 'Confirm Vault PIN';

  const subtitle = hasExistingPin
    ? 'Enter your 4-digit PIN to view hidden photos.'
    : confirmPin === null
    ? 'Choose a 4-digit PIN to protect private photos locally.'
    : 'Re-enter your 4-digit PIN to confirm.';

  return (
    <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-4">
      <div className="w-full max-w-xs rounded-3xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 p-6 shadow-2xl text-center">
        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            aria-label="Close PIN prompt"
            className="min-h-[40px] min-w-[40px] -mr-2 -mt-2 flex items-center justify-center text-zinc-500 hover:text-zinc-900 dark:hover:text-white"
          >
            <CloseFillIcon className="w-6 h-6" />
          </button>
        </div>

        <div className="w-12 h-12 rounded-2xl bg-[var(--accent-soft)] mx-auto flex items-center justify-center text-[var(--accent)] mb-3">
          <LockFillIcon className="w-5 h-5" />
        </div>

        <h2 className="text-lg font-bold text-zinc-950 dark:text-white">{title}</h2>
        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">{subtitle}</p>

        <div className="flex items-center justify-center gap-3 my-5">
          {[0, 1, 2, 3].map((i) => (
            <span
              key={i}
              className={`w-3.5 h-3.5 rounded-full border transition ${
                i < pin.length
                  ? 'bg-[var(--accent)] border-[var(--accent)]'
                  : 'border-zinc-300 dark:border-zinc-700'
              }`}
            />
          ))}
        </div>

        {error && <p className="text-xs text-red-500 mb-3">{error}</p>}

        <div className="grid grid-cols-3 gap-2.5">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del'].map((key, idx) => {
            if (key === '') return <div key={idx} />;
            if (key === 'del') {
              return (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setPin((p) => p.slice(0, -1))}
                  aria-label="Backspace"
                  className="h-14 rounded-2xl bg-zinc-100 dark:bg-zinc-900 hover:bg-zinc-200 dark:hover:bg-zinc-800 flex items-center justify-center text-zinc-700 dark:text-zinc-300 transition"
                >
                  <ArrowLeftFillIcon className="w-5 h-5" />
                </button>
              );
            }
            return (
              <button
                key={idx}
                type="button"
                onClick={() => handleDigit(key)}
                className="h-14 rounded-2xl bg-zinc-100 dark:bg-zinc-900 hover:bg-zinc-200 dark:hover:bg-zinc-800 text-lg font-bold text-zinc-950 dark:text-white active:scale-95 transition"
              >
                {key}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
