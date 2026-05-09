import { useEffect } from "react";
import type { ReactElement } from "react";
import { useGame } from "../store/gameStore";

export function ErrorBanner(): ReactElement | null {
  const error = useGame((s) => s.errorMessage);
  const setError = useGame((s) => s.setError);
  useEffect(() => {
    if (!error) return;
    const t = window.setTimeout(() => setError(null), 4000);
    return () => window.clearTimeout(t);
  }, [error, setError]);
  if (!error) return null;
  return (
    <div className="fixed inset-x-0 top-0 z-40 flex justify-center px-3 pt-[max(12px,var(--top-safe))]">
      <div className="w-full max-w-[440px] rounded-lg border border-accent/40 bg-accent/95 px-4 py-3 text-sm font-semibold text-white shadow-lg backdrop-blur">
        {error}
      </div>
    </div>
  );
}
