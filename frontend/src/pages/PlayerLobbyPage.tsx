import { useGame } from "../store/gameStore";

export function PlayerLobbyPage(): JSX.Element {
  const me = useGame((s) => s.myPlayer);
  const players = useGame((s) => s.players);

  return (
    <main className="flex flex-1 flex-col items-center justify-center text-center gap-6">
      <div className="text-6xl">✈️</div>
      <h1 className="text-2xl font-bold leading-snug">
        Ждём, пока организатор
        <br />
        нажмёт <span className="text-accent">большую красную</span>
        <br />
        кнопку хаоса
      </h1>
      <p className="text-zinc-400 text-sm">
        Ты в комнате как <strong>{me?.name ?? "?"}</strong>.
        <br />
        Игроков на борту: {players.length}.
      </p>
      <ul className="w-full max-w-xs space-y-2">
        {players.map((p) => (
          <li
            key={p.id}
            className={`rounded-xl px-3 py-2 text-sm ${
              p.id === me?.id ? "bg-accent text-white" : "bg-ink-800"
            }`}
          >
            {p.name}
          </li>
        ))}
      </ul>
    </main>
  );
}
