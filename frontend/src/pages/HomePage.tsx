import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@heroui/react";
import { useEffect } from "react";
import { useGame } from "../store/gameStore";
import { restApi } from "../api/rest";
import { getSocket } from "../App";

export function HomePage(): JSX.Element {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setError = useGame((s) => s.setError);
  const setRoomFromCreated = useGame((s) => s.setRoomFromCreated);

  useEffect(() => {
    const code = params.get("code");
    if (code) {
      navigate(`/join?code=${encodeURIComponent(code)}`, { replace: true });
    }
  }, [params, navigate]);

  const onCreate = async (): Promise<void> => {
    try {
      const data = await restApi.createRoom();
      setRoomFromCreated({
        roomId: data.roomId,
        code: data.code,
        organizerToken: data.organizerToken,
        totalQuestions: data.totalQuestions,
      });
      getSocket().setJoin({
        roomCode: data.code,
        organizerToken: data.organizerToken,
      });
      navigate("/organizer");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Ошибка создания комнаты");
    }
  };

  return (
    <main className="flex flex-1 flex-col items-center justify-center text-center gap-8">
      <div className="space-y-3">
        <p className="text-accent uppercase tracking-widest text-xs">квиз</p>
        <h1 className="text-4xl font-black leading-tight">
          День рождения
          <br />
          <span className="text-accent">Вани</span>
        </h1>
        <p className="text-zinc-400 text-sm">
          Сколько ты знаешь про главного авиамана МАИ?
        </p>
      </div>

      <div className="w-full space-y-3">
        <Button
          color="danger"
          size="lg"
          radius="lg"
          className="w-full h-14 text-base font-semibold"
          onPress={() => navigate("/join")}
        >
          Я игрок ✈️
        </Button>
        <Button
          variant="bordered"
          size="lg"
          radius="lg"
          className="w-full h-14 text-base font-semibold border-zinc-700 text-zinc-100"
          onPress={onCreate}
        >
          Я организатор 🎤
        </Button>
      </div>
      <p className="text-xs text-zinc-500">
        Лучше всего работает на iPhone в портретном режиме.
      </p>
    </main>
  );
}
