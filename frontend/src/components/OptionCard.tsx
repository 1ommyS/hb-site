import { motion } from "framer-motion";
import type { ReactElement } from "react";
import type { OptionDto } from "../types";

interface Props {
  option: OptionDto;
  selected: boolean;
  disabled: boolean;
  highlight?: "correct" | "wrong" | null;
  onPick: (id: string) => void;
}

export function OptionCard({
  option,
  selected,
  disabled,
  highlight,
  onPick,
}: Props): ReactElement {
  const base =
    "tap-target w-full rounded-lg border px-4 py-4 text-left text-base font-semibold transition-[transform,background,border-color] active:scale-[0.98] disabled:opacity-90";
  let cls = `${base} border-white/10 bg-panel-2 text-zinc-100 shadow-[0_10px_28px_rgba(0,0,0,0.18)]`;
  if (highlight === "correct") cls = `${base} border-radar/50 bg-radar/18 text-white`;
  else if (highlight === "wrong") cls = `${base} border-accent/50 bg-accent/18 text-white`;
  else if (selected) cls = `${base} border-accent bg-accent text-white shadow-[0_14px_30px_rgba(255,67,101,0.18)]`;
  return (
    <motion.button
      type="button"
      className={cls}
      disabled={disabled}
      onClick={() => onPick(option.id)}
      whileTap={{ scale: 0.97 }}
    >
      <span className="flex items-start gap-3">
        <span
          className={`grid h-7 w-7 shrink-0 place-items-center rounded-full border text-sm font-black ${
            selected ? "border-white/40 bg-white/16 text-white" : "border-sky/40 bg-sky/10 text-sky"
          }`}
        >
          {option.id}
        </span>
        <span className="min-w-0 leading-snug">{option.text}</span>
      </span>
    </motion.button>
  );
}
