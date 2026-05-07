import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Label, TextField } from "@heroui/react";
import { restApi } from "../api/rest";
import { useGame } from "../store/gameStore";
import { getSocket } from "../App";

export function JoinPage(): JSX.Element {
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
    <main className="flex flex-1 flex-col gap-6 pt-6">
      <button
        type="button"
        className="text-zinc-400 text-sm self-start tap-target"
        onClick={() => navigate("/")}
      >
        ← назад
      </button>

      <h1 className="text-2xl font-bold">Подключение к квизу</h1>

      <div className="space-y-4">
        <TextField
          value={code}
          onChange={(v) => setCode(v.toUpperCase().slice(0, 8))}
        >
          <Label>Код комнаты</Label>
          <Input
            placeholder="ABCDE"
            autoCapitalize="characters"
            autoComplete="off"
            autoCorrect="off"
            spellCheck={false}
          />
        </TextField>
        <TextField
          value={name}
          onChange={(v) => setName(v.slice(0, 20))}
        >
          <Label>Твое имя</Label>
          <Input placeholder="Ваня forever" autoComplete="given-name" />
        </TextField>
      </div>

      <Button
        color="danger"
        size="lg"
        radius="lg"
        className="h-14 text-base font-semibold"
        isLoading={busy}
        onPress={submit}
      >
        Войти в комнату
      </Button>
    </main>
  );
}
