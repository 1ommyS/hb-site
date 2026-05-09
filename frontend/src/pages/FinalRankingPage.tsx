import { useEffect, useState } from "react";
import type { ReactElement } from "react";
import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { restApi } from "../api/rest";
import type { StatsResponse } from "../types";

export function FinalRankingPage(): ReactElement {
  const final = useGame((s) => s.finalRanking);
  const me = useGame((s) => s.myPlayer);
  const code = useGame((s) => s.roomCode);
  const reset = useGame((s) => s.reset);
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [showStats, setShowStats] = useState(false);

  useEffect(() => {
    if (!showStats || !code) return;
    restApi.stats(code).then(setStats).catch(() => {});
  }, [showStats, code]);

  if (!final) {
    return (
      <main className="flex flex-1 items-center justify-center text-zinc-400">
        Подбиваем итоги...
      </main>
    );
  }

  return (
    <main className="page">
      <header className="space-y-2 text-center">
        <p className="eyebrow">финал</p>
        <h1 className="hero-title">Крылья сложены</h1>
        <p className="text-sm text-zinc-400">Итоговый рейтинг</p>
      </header>

      <ol className="space-y-2">
        {final.ranking.map((row) => (
          <li
            key={row.playerId}
            className={`flex items-center justify-between gap-3 rounded-lg px-4 py-3 ${
              row.playerId === me?.id
                ? "bg-accent text-white shadow-[0_14px_28px_rgba(255,67,101,0.18)]"
                : "soft-panel"
            }`}
          >
            <div className="min-w-0 flex items-center gap-3">
              <span className="w-8 shrink-0 font-mono text-xl">{row.rank}</span>
              <div className="min-w-0">
                <p className="font-semibold">{row.name}</p>
                <p className="text-xs opacity-70">
                  {row.correctAnswers} прав., среднее{" "}
                  {Math.round(row.averageAnswerTimeMs / 100) / 10} c
                </p>
              </div>
            </div>
            <span className="shrink-0 font-mono text-xl">{row.score}</span>
          </li>
        ))}
      </ol>

      <Button
        variant="outline"
        size="lg"
        className="secondary-action text-base"
        onPress={() => setShowStats((v) => !v)}
      >
        {showStats ? "Скрыть статистику" : "Показать статистику ответов"}
      </Button>

      {showStats && stats && (
        <section className="soft-panel space-y-3 p-4 text-sm">
          <div className="grid grid-cols-1 gap-2">
            {stats.mostPopularWrong && (
              <p className="rounded-lg bg-panel-2 px-3 py-2">
                Самый популярный неправильный:{" "}
                <strong>{stats.mostPopularWrong}</strong>
              </p>
            )}
            {stats.hardestQuestionId && (
              <p className="rounded-lg bg-panel-2 px-3 py-2">
                Самый сложный вопрос:{" "}
                <strong>{stats.hardestQuestionId}</strong>
              </p>
            )}
            {stats.confusingQuestionId && (
              <p className="rounded-lg bg-panel-2 px-3 py-2">
                Самый спорный вопрос:{" "}
                <strong>{stats.confusingQuestionId}</strong>
              </p>
            )}
          </div>
          <div className="space-y-2">
            {stats.perQuestion.map((q) => {
              const total = Object.values(q.distribution).reduce(
                (a, b) => a + b,
                0,
              );
              return (
                <details
                  key={q.questionId}
                  className="rounded-lg bg-panel-2 p-3"
                >
                  <summary className="cursor-pointer text-sm font-semibold">
                    <span className="mr-2 font-mono text-zinc-400">
                      {q.questionNumber}.
                    </span>
                    {q.text}
                  </summary>
                  <ul className="mt-3 space-y-2">
                    {Object.entries(q.distribution).map(([k, v]) => {
                      const pct = total > 0 ? Math.round((v / total) * 100) : 0;
                      const correct = q.correctOptions.includes(k);
                      return (
                        <li key={k}>
                          <div
                            className={`flex justify-between ${
                              correct ? "font-bold text-radar" : "text-zinc-300"
                            }`}
                          >
                            <span>{k}</span>
                            <span className="text-zinc-400">
                              {v}/{total}
                            </span>
                          </div>
                          <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-ink-800">
                            <div
                              className={correct ? "h-full bg-radar" : "h-full bg-sky/55"}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                </details>
              );
            })}
          </div>
        </section>
      )}

      <Button
        variant="ghost"
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
