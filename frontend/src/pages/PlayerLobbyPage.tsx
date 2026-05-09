import type { ReactElement } from "react";
import { useGame } from "../store/gameStore";

export function PlayerLobbyPage(): ReactElement {
  const me = useGame((s) => s.myPlayer);
  const players = useGame((s) => s.players);

  return (
    <main className="page-centered">
      <section className="runway w-full" aria-hidden>
        <div className="runway-plane animate-pulse">✈</div>
      </section>

      <header className="space-y-3">
        <p className="eyebrow">ожидание старта</p>
        <h1 className="screen-title">
          Организатор готовит первый вопрос
        </h1>
        <p className="text-sm leading-6 text-zinc-400">
          Ты в комнате как <strong className="text-zinc-100">{me?.name ?? "?"}</strong>.
          Игроков на борту: {players.length}.
        </p>
      </header>

      <ul className="w-full space-y-2 text-left">
        {players.map((p) => (
          <li
            key={p.id}
            className={`flex items-center justify-between rounded-lg px-3 py-2.5 text-sm ${
              p.id === me?.id
                ? "bg-accent text-white shadow-[0_14px_28px_rgba(255,67,101,0.2)]"
                : "bg-panel-2 text-zinc-200"
            }`}
          >
            <span className="min-w-0 truncate font-semibold">{p.name}</span>
            {p.id === me?.id && <span className="text-xs font-bold">ты</span>}
          </li>
        ))}
      </ul>
    </main>
  );
}
