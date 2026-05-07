import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { getSocket } from "../App";

export function ResultPage(): JSX.Element {
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
    <main className="flex flex-1 flex-col gap-5 pt-4">
      <header className="text-center">
        <p className="uppercase text-xs tracking-widest text-zinc-500">
          Результат
        </p>
        <h1 className="text-2xl font-bold mt-1">
          Правильно: {result.correctOptions.join(", ")}
        </h1>
      </header>

      {q?.text && (
        <p className="text-center text-zinc-400 text-sm">{q.text}</p>
      )}

      {result.comment && (
        <div className="rounded-2xl bg-ink-900 border border-ink-800 p-4 text-sm">
          {result.comment}
        </div>
      )}

      {me && myAnswer && (
        <div
          className={`rounded-2xl p-4 ${
            myAnswer.isCorrect ? "bg-emerald-700/30" : "bg-red-700/30"
          }`}
        >
          <p className="text-xs uppercase tracking-widest text-zinc-400">
            Твой ответ
          </p>
          <p className="font-bold text-lg">
            {myAnswer.selectedOptions.join(", ") || "пропущен"}{" "}
            {myAnswer.isCorrect ? "✓" : "✗"}
          </p>
          <p className="text-sm text-zinc-300">
            +{myAnswer.pointsEarned} баллов · всего {myAnswer.totalScore}
          </p>
        </div>
      )}

      <section>
        <h2 className="text-sm font-semibold mb-2 text-zinc-300">
          Распределение
        </h2>
        <ul className="space-y-2">
          {Object.entries(result.distribution).map(([key, count]) => {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            const correct = correctSet.has(key);
            return (
              <li key={key}>
                <div className="flex items-baseline justify-between text-sm">
                  <span className={correct ? "text-emerald-400" : ""}>
                    {key} {correct && "✓"}
                  </span>
                  <span className="text-zinc-400">
                    {count} ({pct}%)
                  </span>
                </div>
                <div className="h-2 rounded-full bg-ink-800 overflow-hidden mt-1">
                  <div
                    className={`h-full ${
                      correct ? "bg-emerald-500" : "bg-zinc-500"
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </li>
            );
          })}
        </ul>
      </section>

      <section>
        <h2 className="text-sm font-semibold mb-2 text-zinc-300">
          Топ-5 рейтинга
        </h2>
        <ol className="space-y-1 text-sm">
          {result.ranking.slice(0, 5).map((p, i) => (
            <li
              key={p.id}
              className={`flex justify-between rounded-xl px-3 py-2 ${
                p.id === me?.id ? "bg-accent text-white" : "bg-ink-900"
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
        <Button
          color="danger"
          size="lg"
          radius="lg"
          className="h-14 text-base font-semibold"
          onPress={next}
        >
          Следующий вопрос →
        </Button>
      )}
    </main>
  );
}
