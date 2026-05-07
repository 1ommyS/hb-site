import { useEffect, useRef } from "react";
import { Howl } from "howler";

const beepData =
  // короткий «бип» через WebAudio data-uri (синусоида 880 Hz, 0.25 сек)
  // на iOS требуется первое пользовательское взаимодействие, иначе блок
  "data:audio/wav;base64,UklGRoQDAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQADAACAgICAgICAgICAgICAgICAgIB/f4B/f3+Af39/f4B/f39/gH9/f3+Af39/f4B/f3+Af39/f4B/f3+Af39/f4CAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIA=";

export function useEndOfQuestionWarning(active: boolean): {
  playWarning: () => void;
} {
  const howlRef = useRef<Howl | null>(null);

  useEffect(() => {
    howlRef.current = new Howl({
      src: [beepData],
      volume: 0.6,
      preload: true,
    });
    return () => {
      howlRef.current?.unload();
      howlRef.current = null;
    };
  }, []);

  const playWarning = (): void => {
    if (!active) return;
    try {
      howlRef.current?.play();
    } catch {
      /* iOS may block; fallback is vibration */
    }
    if (typeof navigator.vibrate === "function") {
      navigator.vibrate([60, 40, 60]);
    }
  };

  return { playWarning };
}
