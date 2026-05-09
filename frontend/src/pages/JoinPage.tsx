import { useState } from "react";
import type { ReactElement } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Label, TextField } from "@heroui/react";
import { restApi } from "../api/rest";
import { useGame } from "../store/gameStore";
import { getSocket } from "../App";

export function JoinPage(): ReactElement {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const sessionId = useGame((s) => s.sessionId);
  const setMyPlayer = useGame((s) => s.setMyPlayer);
  const setError = useGame((s) => s.setError);

  const [code, setCode] = useState(params.get("code")?.toUpperCase() ?? "");
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (): Promise<void> => {
    if (busy) return;
    const cleanCode = code.trim().toUpperCase();
    const cleanName = name.trim();
    if (cleanCode.length < 4) {
      setError("Код комнаты слишком короткий");
      return;
    }
    if (cleanName.length < 2) {
      setError("Введи имя длиной от 2 символов");
      return;
    }
    setBusy(true);
    try {
      const joined = await restApi.joinRoom(cleanCode, cleanName, sessionId);
      setMyPlayer(joined.player, cleanCode, joined.roomId);
      getSocket().setJoin({
        roomCode: cleanCode,
        sessionId,
        name: cleanName,
      });
      navigate("/player");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось подключиться");
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="page">
      <button
        type="button"
        className="tap-target self-start rounded-lg px-1 text-sm font-semibold text-zinc-400 transition hover:text-zinc-100"
        onClick={() => navigate("/")}
      >
        Назад
      </button>

      <header className="space-y-3">
        <p className="eyebrow">посадка на борт</p>
        <h1 className="screen-title">Подключение к квизу</h1>
        <p className="text-sm leading-6 text-zinc-400">
          Введи код комнаты и имя, которое увидят остальные игроки.
        </p>
      </header>

      <div className="soft-panel space-y-4 p-4">
        <TextField
          value={code}
          onChange={(v) => setCode(v.toUpperCase().slice(0, 8))}
        >
          <Label className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-sky">
            Код комнаты
          </Label>
          <Input
            placeholder="ABCDE"
            autoCapitalize="characters"
            autoComplete="off"
            autoCorrect="off"
            spellCheck={false}
            className="h-14 rounded-lg border border-white/10 bg-panel-2 px-4 text-center text-xl font-black uppercase tracking-[0.24em] text-zinc-50 outline-none transition focus:border-sky/70"
          />
        </TextField>
        <TextField
          value={name}
          onChange={(v) => setName(v.slice(0, 20))}
        >
          <Label className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-sky">
            Твое имя
          </Label>
          <Input
            placeholder="Ваня forever"
            autoComplete="given-name"
            className="h-14 rounded-lg border border-white/10 bg-panel-2 px-4 text-base font-semibold text-zinc-50 outline-none transition focus:border-sky/70"
          />
        </TextField>
      </div>

      <div className="sticky-actions">
        <Button
          size="lg"
          className="primary-action text-base"
          isDisabled={busy}
          onPress={submit}
        >
          {busy ? "Подключаем..." : "Войти в комнату"}
        </Button>
      </div>
    </main>
  );
}
