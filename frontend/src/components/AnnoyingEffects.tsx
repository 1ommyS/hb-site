import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";

interface Props {
  questionId: string;
  timeRemainingMs: number;
}

type EffectKind =
  | "history"
  | "plane"
  | "mai-loading"
  | "dpp"
  | "masha"
  | "rle";

interface ActiveEffect {
  kind: EffectKind;
  id: number;
}

const TEXTS: Record<EffectKind, string> = {
  history: "Краткая историческая справка на 30 секунд...",
  plane: "✈️",
  "mai-loading": "МАИ думает...",
  dpp: "Платформа ДПП хочет оффер мечты",
  masha: "Обнаружена Маша. Ваня напрягся.",
  rle: "См. РЛЭ Су-27, глава 18.",
};

const SAFE_TAIL_MS = 5_000;

export function AnnoyingEffects({
  questionId,
  timeRemainingMs,
}: Props): JSX.Element {
  const [active, setActive] = useState<ActiveEffect[]>([]);

  useEffect(() => {
    setActive([]);
    let counter = 0;
    let cancelled = false;

    const tick = (): void => {
      if (cancelled) return;
      const remaining = timeRemainingMs;
      if (remaining > SAFE_TAIL_MS && Math.random() < 0.6) {
        const kinds: EffectKind[] = [
          "history",
          "plane",
          "mai-loading",
          "dpp",
          "masha",
          "rle",
        ];
        const kind = kinds[Math.floor(Math.random() * kinds.length)];
        const id = ++counter;
        setActive((prev) => [...prev, { kind, id }]);
        const ttl = 1000 + Math.random() * 1500;
        window.setTimeout(() => {
          setActive((prev) => prev.filter((e) => e.id !== id));
        }, ttl);
      }
    };

    const interval = window.setInterval(tick, 4500);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [questionId, timeRemainingMs]);

  return (
    <div
      className="pointer-events-none absolute inset-0 overflow-hidden"
      aria-hidden
    >
      <AnimatePresence>
        {active.map((e) => (
          <EffectView key={e.id} kind={e.kind} />
        ))}
      </AnimatePresence>
    </div>
  );
}

function EffectView({ kind }: { kind: EffectKind }): JSX.Element {
  const text = TEXTS[kind];
  if (kind === "plane") {
    return (
      <motion.div
        className="absolute top-1/3 text-3xl"
        initial={{ x: "-30%" }}
        animate={{ x: "130%" }}
        exit={{ opacity: 0 }}
        transition={{ duration: 1.2, ease: "easeInOut" }}
      >
        ✈️
      </motion.div>
    );
  }
  if (kind === "dpp") {
    return (
      <motion.div
        className="absolute bottom-24 left-3 right-3 rounded-xl bg-yellow-400/95 text-zinc-900 px-4 py-3 text-sm font-semibold shadow-lg"
        initial={{ y: "120%", opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ y: "120%", opacity: 0 }}
        transition={{ type: "spring", stiffness: 320, damping: 24 }}
      >
        {text}
      </motion.div>
    );
  }
  if (kind === "history") {
    return (
      <motion.div
        className="absolute top-24 left-3 right-3 rounded-xl bg-black/80 text-zinc-100 px-4 py-3 text-sm shadow-lg backdrop-blur"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0 }}
      >
        {text}
      </motion.div>
    );
  }
  if (kind === "mai-loading") {
    return (
      <motion.div
        className="absolute inset-x-6 bottom-32 rounded-xl bg-blue-500/90 text-white px-4 py-2 text-sm text-center"
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ opacity: 0 }}
      >
        {text}
      </motion.div>
    );
  }
  if (kind === "masha") {
    return (
      <motion.div
        className="absolute top-1/4 left-1/2 -translate-x-1/2 rounded-xl bg-pink-600/95 text-white px-4 py-2 text-sm shadow-lg"
        initial={{ scale: 0.6, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.8, opacity: 0 }}
      >
        {text}
      </motion.div>
    );
  }
  return (
    <motion.div
      className="absolute top-32 left-3 right-3 rounded-xl bg-zinc-700/95 text-zinc-100 px-4 py-3 text-sm italic"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      {text}
    </motion.div>
  );
}
