import { Button } from "@heroui/react";
import type { ReactElement } from "react";
import { useGame } from "../store/gameStore";
import { QrCard } from "../components/QrCard";
import { getSocket } from "../App";

export function OrganizerLobbyPage(): ReactElement {
  const code = useGame((s) => s.roomCode);
  const players = useGame((s) => s.players);
  const status = useGame((s) => s.status);

  const joinUrl = code
    ? `${window.location.origin}/?code=${encodeURIComponent(code)}`
    : window.location.origin;

  const start = (): void => {
    getSocket().send("START_QUIZ");
  };

  return (
    <main className="page">
      <header className="space-y-3 text-center">
        <p className="eyebrow">лобби организатора</p>
        <h1 className="screen-title">Код комнаты</h1>
        <div className="soft-panel inline-flex px-6 py-3 text-4xl font-black mono-code text-zinc-50">
          {code ?? "—"}
        </div>
      </header>

      <section className="soft-panel space-y-3 p-4">
        <QrCard text={joinUrl} />
        <p className="break-all text-center text-xs leading-5 text-zinc-400">
          {joinUrl}
        </p>
      </section>

      <section className="soft-panel p-4">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="font-semibold">
            Игроки <span className="text-zinc-500">({players.length})</span>
          </h2>
          <span className="status-chip">{status}</span>
        </div>
        <ul className="space-y-2">
          {players.length === 0 && (
            <li className="rounded-lg border border-dashed border-white/10 px-3 py-5 text-center text-sm text-zinc-500">
              Никого нет. Раздавай ссылку.
            </li>
          )}
          {players.map((p) => (
            <li
              key={p.id}
              className="flex items-center justify-between gap-3 rounded-lg bg-panel-2 px-3 py-2.5"
            >
              <span className="min-w-0 truncate font-semibold">{p.name}</span>
              <span className="text-xs font-semibold text-radar">готов</span>
            </li>
          ))}
        </ul>
      </section>

      <div className="sticky-actions">
        <Button
          size="lg"
          className="primary-action text-base disabled:opacity-50"
          isDisabled={players.length === 0}
          onPress={start}
        >
          Запустить квиз
        </Button>
      </div>
    </main>
  );
}
