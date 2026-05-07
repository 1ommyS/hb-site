import { useEffect } from "react";
import { useGame } from "../store/gameStore";

export function ErrorBanner(): JSX.Element | null {
  const error = useGame((s) => s.errorMessage);
  const setError = useGame((s) => s.setError);
  useEffect(() => {
    if (!error) return;
    const t = window.setTimeout(() => setError(null), 4000);
    return () => window.clearTimeout(t);
  }, [error, setError]);
  if (!error) return null;
  return (
    <div className="fixed inset-x-0 top-0 z-40 flex justify-center px-3 pt-3">
      <div className="max-w-[440px] w-full rounded-xl bg-red-600/90 px-4 py-3 text-sm text-white shadow-lg backdrop-blur">
        {error}
      </div>
    </div>
  );
}
