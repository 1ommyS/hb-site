import { useEffect, useState } from "react";
import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { restApi } from "../api/rest";

interface StatsView {
  mostPopularWrong: string | null;
  hardestQuestionId: string | null;
  unanimouslyCorrectQuestionId: string | null;
  confusingQuestionId: string | null;
  perQuestion: Array<{
    questionId: string;
    questionNumber: number;
    text: string;
    correctOptions: string[];
    distribution: Record<string, number>;
  }>;
}

export function FinalRankingPage(): JSX.Element {
  const final = useGame((s) => s.finalRanking);
  const me = useGame((s) => s.myPlayer);
  const code = useGame((s) => s.roomCode);
  const reset = useGame((s) => s.reset);
  const [stats, setStats] = useState<StatsView | null>(null);
  const [showStats, setShowStats] = useState(false);

  useEffect(() => {
    if (!showStats || !code) return;
    restApi.stats(code).then((s) => setStats(s as StatsView)).catch(() => {});
  }, [showStats, code]);

  if (!final) {
    return (
      <main className="flex flex-1 items-center justify-center text-zinc-400">
        Подбиваем итоги...
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-5 pt-4">
      <header className="text-center">
        <p className="uppercase text-xs tracking-widest text-zinc-500">
          Финал
        </p>
        <h1 className="text-3xl font-black mt-1">Крылья сложены ✈️</h1>
        <p className="text-sm text-zinc-400 mt-2">Итоговый рейтинг</p>
      </header>

      <ol className="space-y-2">
        {final.ranking.map((row) => (
          <li
            key={row.playerId}
            className={`flex items-center justify-between rounded-2xl px-4 py-3 ${
              row.playerId === me?.id ? "bg-accent text-white" : "bg-ink-900"
            }`}
          >
            <div className="flex items-center gap-3">
              <span className="text-xl font-mono w-8">{row.rank}.</span>
              <div>
                <p className="font-semibold">{row.name}</p>
                <p className="text-xs opacity-70">
                  {row.correctAnswers} прав. · среднее{" "}
                  {Math.round(row.averageAnswerTimeMs / 100) / 10} c
                </p>
              </div>
            </div>
            <span className="font-mono text-xl">{row.score}</span>
          </li>
        ))}
      </ol>

      <Button
        variant="bordered"
        size="lg"
        radius="lg"
        className="border-zinc-700 text-zinc-100"
        onPress={() => setShowStats((v) => !v)}
      >
        {showStats ? "Скрыть статистику" : "Показать статистику ответов"}
      </Button>

      {showStats && stats && (
        <section className="rounded-2xl bg-ink-900 p-4 text-sm space-y-3">
          {stats.mostPopularWrong && (
            <p>
              Самый популярный неправильный:{" "}
              <strong>{stats.mostPopularWrong}</strong>
            </p>
          )}
          <div className="space-y-2">
            {stats.perQuestion.map((q) => {
              const total = Object.values(q.distribution).reduce(
                (a, b) => a + b,
                0,
              );
              return (
                <details
                  key={q.questionId}
                  className="rounded-xl bg-ink-800 p-3"
                >
                  <summary className="cursor-pointer">
                    <span className="text-zinc-400 font-mono mr-2">
                      {q.questionNumber}.
                    </span>
                    {q.text}
                  </summary>
                  <ul className="mt-2 space-y-1">
                    {Object.entries(q.distribution).map(([k, v]) => (
                      <li
                        key={k}
                        className={`flex justify-between ${
                          q.correctOptions.includes(k) ? "text-emerald-400" : ""
                        }`}
                      >
                        <span>
                          {k} {q.correctOptions.includes(k) && "✓"}
                        </span>
                        <span className="text-zinc-400">
                          {v}/{total}
                        </span>
                      </li>
                    ))}
                  </ul>
                </details>
              );
            })}
          </div>
        </section>
      )}

      <Button
        variant="light"
        className="text-zinc-400"
        onPress={() => {
          reset();
          window.location.href = "/";
        }}
      >
        В начало
      </Button>
    </main>
  );
}
