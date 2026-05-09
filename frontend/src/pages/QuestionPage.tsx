import { useMemo } from "react";
import type { ReactElement } from "react";
import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { Timer } from "../components/Timer";
import { OptionCard } from "../components/OptionCard";
import { useEndOfQuestionWarning } from "../hooks/useSound";
import { getSocket } from "../App";
import { AnnoyingEffects } from "../components/AnnoyingEffects";

export function QuestionPage(): ReactElement {
  const q = useGame((s) => s.currentQuestion);
  const selected = useGame((s) => s.selectedOptions);
  const submitted = useGame((s) => s.answerSubmitted);
  const annoyingMode = useGame((s) => s.annoyingModeEnabled);
  const role = useGame((s) => s.role);
  const togglePick = useGame((s) => s.toggleSelected);
  const { playWarning } = useEndOfQuestionWarning(role === "player");

  const isMulti = q?.type === "multiple";

  const onPick = (id: string): void => {
    if (!q || submitted) return;
    togglePick(id, isMulti);
    if (!isMulti) submit([id]);
  };

  const submit = (override?: string[]): void => {
    if (!q || submitted) return;
    const choice = override ?? selected;
    if (choice.length === 0) return;
    getSocket().send("SUBMIT_ANSWER", {
      questionId: q.questionId,
      selectedOptions: choice,
    });
  };

  const remainingHint = useMemo(() => {
    if (!q) return null;
    return new Date(q.endsAt).getTime() - Date.now();
  }, [q]);

  if (!q) {
    return (
      <main className="flex flex-1 items-center justify-center text-zinc-400">
        Ждём вопрос...
      </main>
    );
  }

  return (
    <main className="page relative">
      {annoyingMode && remainingHint != null && (
        <AnnoyingEffects timeRemainingMs={remainingHint} questionId={q.questionId} />
      )}
      <header className="flex items-center justify-between gap-2">
        <span className="status-chip">
          Вопрос {q.questionNumber} / {q.totalQuestions}
        </span>
        <span className="status-chip">{isMulti ? "несколько" : "один"}</span>
      </header>

      <Timer startedAt={q.startedAt} endsAt={q.endsAt} onWarning={playWarning} />

      <section className="soft-panel p-4">
        <h1 className="text-xl font-black leading-snug text-zinc-50">
          {q.text}
        </h1>
      </section>

      <div className="flex flex-col gap-3">
        {q.options.map((o) => (
          <OptionCard
            key={o.id}
            option={o}
            selected={selected.includes(o.id)}
            disabled={submitted}
            onPick={onPick}
          />
        ))}
      </div>

      {isMulti && (
        <div className="sticky-actions">
          <Button
            size="lg"
            className="primary-action text-base disabled:opacity-50"
            isDisabled={submitted || selected.length === 0}
            onPress={() => submit()}
          >
            {submitted ? "Ответ принят" : "Ответить"}
          </Button>
        </div>
      )}

      {submitted && (
        <p className="text-center text-sm font-semibold text-radar">
          Ответ принят. Ждём остальных.
        </p>
      )}
    </main>
  );
}
