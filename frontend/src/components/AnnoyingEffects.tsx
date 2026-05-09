import { useEffect, useState } from "react";
import type { ReactElement } from "react";
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
  history: "Ваш ответ очень важен для нас. Оставайтесь на линии",
  plane: "✈",
  "mai-loading": "Загрузка здравого смысла... 3%",
  dpp: "Вы победили! Чтобы забрать приз, ответьте на вопрос",
  masha: "Обнаружен уверенный тык. Осуждаем, но уважаем",
  rle: "Подсказка: один из вариантов точно вариант",
};

const SAFE_TAIL_MS = 5_000;

export function AnnoyingEffects({
  questionId,
  timeRemainingMs,
}: Props): ReactElement {
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
      className="pointer-events-none absolute inset-0 z-20 overflow-hidden"
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

function EffectView({ kind }: { kind: EffectKind }): ReactElement {
  const text = TEXTS[kind];
  if (kind === "plane") {
    return (
      <motion.div
        className="absolute top-1/3 text-4xl text-sky drop-shadow-[0_10px_24px_rgba(56,217,255,0.4)]"
        initial={{ x: "-20vw", opacity: 0.6 }}
        animate={{ x: "110vw", opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 1.15, ease: "easeInOut" }}
      >
        ✈
      </motion.div>
    );
  }
  if (kind === "dpp") {
    return (
      <motion.div
        className="absolute bottom-24 left-3 right-3 rounded-lg border border-fuel/40 bg-fuel/95 px-4 py-3 text-sm font-black text-zinc-950 shadow-[0_18px_42px_rgba(245,200,76,0.22)]"
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
        className="absolute left-3 right-3 top-24 rounded-lg border border-white/10 bg-ink-950/88 px-4 py-3 text-sm leading-5 text-zinc-100 shadow-lg backdrop-blur"
        initial={{ opacity: 0, scale: 0.94 }}
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
        className="absolute inset-x-6 bottom-32 rounded-lg border border-sky/40 bg-sky/92 px-4 py-2 text-center text-sm font-black text-ink-950"
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
        className="absolute left-1/2 top-1/4 w-[min(84vw,340px)] -translate-x-1/2 rounded-lg border border-accent/40 bg-accent/95 px-4 py-2 text-center text-sm font-black text-white shadow-lg"
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
      className="absolute left-3 right-3 top-32 rounded-lg border border-white/10 bg-panel/95 px-4 py-3 text-sm italic text-zinc-100"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      {text}
    </motion.div>
  );
}
