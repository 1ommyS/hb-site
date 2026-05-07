import { Button } from "@heroui/react";
import { useGame } from "../store/gameStore";
import { QrCard } from "../components/QrCard";
import { getSocket } from "../App";

export function OrganizerLobbyPage(): JSX.Element {
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
    <main className="flex flex-1 flex-col gap-5 pt-4">
      <header className="text-center">
        <p className="uppercase text-xs tracking-widest text-zinc-500">
          Лобби организатора
        </p>
        <h1 className="text-3xl font-black mt-1">Код комнаты</h1>
        <div className="mt-3 inline-flex rounded-2xl bg-ink-800 px-6 py-3 text-4xl font-mono tracking-[0.4em]">
          {code ?? "—"}
        </div>
      </header>

      <QrCard text={joinUrl} />
      <p className="text-center text-sm text-zinc-400 break-all">
        {joinUrl}
      </p>

      <section className="rounded-2xl bg-ink-900 p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold">
            Игроки <span className="text-zinc-500">({players.length})</span>
          </h2>
          <span className="text-xs text-zinc-500">{status}</span>
        </div>
        <ul className="space-y-2">
          {players.length === 0 && (
            <li className="text-sm text-zinc-500">
              Никого нет. Раздавай ссылку.
            </li>
          )}
          {players.map((p) => (
            <li
              key={p.id}
              className="flex items-center justify-between rounded-xl bg-ink-800 px-3 py-2"
            >
              <span>{p.name}</span>
              <span className="text-xs text-zinc-500">подключен</span>
            </li>
          ))}
        </ul>
      </section>

      <Button
        color="danger"
        size="lg"
        radius="lg"
        className="h-14 text-base font-semibold"
        isDisabled={players.length === 0}
        onPress={start}
      >
        🚀 Запустить квиз
      </Button>
    </main>
  );
}
