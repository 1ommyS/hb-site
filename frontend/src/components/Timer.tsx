import { useEffect, useState } from "react";
import type { ReactElement } from "react";

interface Props {
  startedAt: string;
  endsAt: string;
  onWarning?: () => void;
  onExpired?: () => void;
}

export function Timer({ startedAt, endsAt, onWarning, onExpired }: Props): ReactElement {
  const [now, setNow] = useState(() => Date.now());
  const [warned, setWarned] = useState(false);
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    setWarned(false);
    setExpired(false);
  }, [endsAt]);

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 100);
    return () => window.clearInterval(id);
  }, []);

  const start = new Date(startedAt).getTime();
  const end = new Date(endsAt).getTime();
  const total = Math.max(end - start, 1);
  const remaining = Math.max(end - now, 0);
  const remainingSec = Math.ceil(remaining / 1000);
  const pct = Math.max(0, Math.min(100, (remaining / total) * 100));

  useEffect(() => {
    if (!warned && remaining <= 10_000 && remaining > 0) {
      setWarned(true);
      onWarning?.();
    }
    if (!expired && remaining === 0) {
      setExpired(true);
      onExpired?.();
    }
  }, [remaining, warned, expired, onWarning, onExpired]);

  const danger = remaining <= 10_000;

  return (
    <div className={`soft-panel flex flex-col gap-3 p-3 ${danger ? "border-accent/45" : ""}`}>
      <div className="flex items-baseline justify-between">
        <span className="eyebrow">
          До отсечки
        </span>
        <span
          className={`font-mono text-3xl font-bold ${
            danger ? "text-accent animate-pulse" : "text-sky"
          }`}
        >
          {remainingSec}<span className="text-base text-zinc-400">с</span>
        </span>
      </div>
      <div className="h-3 rounded-full bg-black/30 overflow-hidden border border-white/5">
        <div
          className={`h-full rounded-full transition-[width] duration-200 ${
            danger ? "bg-accent" : "bg-radar"
          }`}
          style={{ width: `${pct}%` }}
        />
      </div>
      {danger && (
        <p className="text-xs font-semibold text-fuel">
          10 секунд, думай быстрее, профессор авиации не ждёт
        </p>
      )}
    </div>
  );
}
