import { useEffect, useState } from "react";

interface Props {
  startedAt: string;
  endsAt: string;
  onWarning?: () => void;
  onExpired?: () => void;
}

export function Timer({ startedAt, endsAt, onWarning, onExpired }: Props): JSX.Element {
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
    <div className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between">
        <span className="text-zinc-400 text-xs uppercase tracking-widest">
          Таймер
        </span>
        <span
          className={`font-mono text-3xl font-bold ${
            danger ? "text-accent animate-pulse" : "text-zinc-100"
          }`}
        >
          {remainingSec}
        </span>
      </div>
      <div className="h-2 rounded-full bg-ink-800 overflow-hidden">
        <div
          className={`h-full transition-[width] duration-200 ${
            danger ? "bg-accent" : "bg-emerald-500"
          }`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
