import { Button } from "@heroui/react";
import type { ReactElement } from "react";
import { useGame } from "../store/gameStore";
import { getSocket } from "../App";

export function ResultPage(): ReactElement {
  const result = useGame((s) => s.questionResult);
  const me = useGame((s) => s.myPlayer);
  const role = useGame((s) => s.role);
  const q = useGame((s) => s.currentQuestion);

  if (!result) {
    return (
      <main className="flex flex-1 items-center justify-center text-zinc-400">
        Считаем результаты...
      </main>
    );
  }

  const myAnswer =
    me && result.playerAnswers.find((p) => p.playerId === me.id);
  const correctSet = new Set(result.correctOptions);
  const total = Object.values(result.distribution).reduce((a, b) => a + b, 0);

  const next = (): void => getSocket().send("NEXT_QUESTION");

  return (
    <main className="page">
      <header className="space-y-2 text-center">
        <p className="eyebrow">результат вопроса</p>
        <h1 className="screen-title">
          Верный ответ: {result.correctOptions.join(", ")}
        </h1>
      </header>

      {q?.text && (
        <p className="text-center text-sm leading-6 text-zinc-400">{q.text}</p>
      )}

      {result.comment && (
        <div className="soft-panel p-4 text-sm leading-6 text-zinc-200">
          {result.comment}
        </div>
      )}

      {me && myAnswer && (
        <div
          className={`rounded-lg border p-4 ${
            myAnswer.isCorrect
              ? "border-radar/35 bg-radar/12"
              : "border-accent/35 bg-accent/12"
          }`}
        >
          <p className="text-xs font-bold uppercase tracking-[0.16em] text-zinc-400">
            Твой ответ
          </p>
          <p className="font-bold text-lg">
            {myAnswer.selectedOptions.join(", ") || "пропущен"}
          </p>
          <p className="text-sm text-zinc-300">
            +{myAnswer.pointsEarned} баллов, всего {myAnswer.totalScore}
          </p>
        </div>
      )}

      <section className="soft-panel p-4">
        <h2 className="mb-3 text-sm font-bold text-zinc-200">
          Распределение
        </h2>
        <ul className="space-y-2">
          {Object.entries(result.distribution).map(([key, count]) => {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            const correct = correctSet.has(key);
            return (
              <li key={key}>
                <div className="flex items-baseline justify-between text-sm">
                  <span className={correct ? "font-bold text-radar" : ""}>
                    {key}
                  </span>
                  <span className="text-zinc-400">
                    {count} ({pct}%)
                  </span>
                </div>
                <div className="mt-1 h-2 overflow-hidden rounded-full bg-ink-800">
                  <div
                    className={`h-full ${
                      correct ? "bg-radar" : "bg-sky/55"
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="soft-panel p-4">
        <h2 className="mb-3 text-sm font-bold text-zinc-200">
          Топ-5 рейтинга
        </h2>
        <ol className="space-y-2 text-sm">
          {result.ranking.slice(0, 5).map((p, i) => (
            <li
              key={p.id}
              className={`flex justify-between rounded-lg px-3 py-2.5 ${
                p.id === me?.id ? "bg-accent text-white" : "bg-panel-2"
              }`}
            >
              <span>
                {i + 1}. {p.name}
              </span>
              <span className="font-mono">{p.score}</span>
            </li>
          ))}
        </ol>
      </section>

      {role === "organizer" && (
        <div className="sticky-actions">
          <Button
            size="lg"
            className="primary-action text-base"
            onPress={next}
          >
            Следующий вопрос
          </Button>
        </div>
      )}
    </main>
  );
}
