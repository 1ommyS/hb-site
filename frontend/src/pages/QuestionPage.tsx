import { useMemo } from "react";
import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { Timer } from "../components/Timer";
import { OptionCard } from "../components/OptionCard";
import { useEndOfQuestionWarning } from "../hooks/useSound";
import { getSocket } from "../App";
import { AnnoyingEffects } from "../components/AnnoyingEffects";

export function QuestionPage(): JSX.Element {
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
    <main className="flex flex-1 flex-col gap-5 pt-3 relative">
      {annoyingMode && remainingHint != null && (
        <AnnoyingEffects timeRemainingMs={remainingHint} questionId={q.questionId} />
      )}
      <header className="flex items-center justify-between text-xs text-zinc-500 uppercase tracking-widest">
        <span>
          Вопрос {q.questionNumber} / {q.totalQuestions}
        </span>
        <span>{isMulti ? "несколько вариантов" : "один вариант"}</span>
      </header>

      <Timer startedAt={q.startedAt} endsAt={q.endsAt} onWarning={playWarning} />

      <h1 className="text-xl font-bold leading-snug">{q.text}</h1>

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
        <Button
          color="danger"
          size="lg"
          radius="lg"
          className="h-14 text-base font-semibold"
          isDisabled={submitted || selected.length === 0}
          onPress={() => submit()}
        >
          {submitted ? "Ответ принят" : "Ответить"}
        </Button>
      )}

      {submitted && (
        <p className="text-center text-zinc-400 text-sm">
          Ответ принят. Ждём остальных.
        </p>
      )}
    </main>
  );
}
