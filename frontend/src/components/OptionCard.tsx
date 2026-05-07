import { motion } from "framer-motion";
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
}: Props): JSX.Element {
  const base =
    "tap-target w-full rounded-2xl px-4 py-4 text-left text-base font-medium transition-[transform,background] active:scale-[0.98]";
  let cls = `${base} bg-ink-800 text-zinc-100`;
  if (highlight === "correct") cls = `${base} bg-emerald-600 text-white`;
  else if (highlight === "wrong") cls = `${base} bg-red-600 text-white`;
  else if (selected) cls = `${base} bg-accent text-white`;
  return (
    <motion.button
      type="button"
      className={cls}
      disabled={disabled}
      onClick={() => onPick(option.id)}
      whileTap={{ scale: 0.97 }}
    >
      <span className="font-mono mr-2 text-zinc-400">{option.id}.</span>
      {option.text}
    </motion.button>
  );
}
