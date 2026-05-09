import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@heroui/react";
import { useEffect } from "react";
import type { ReactElement } from "react";
import { useGame } from "../store/gameStore";
import { restApi } from "../api/rest";
import { getSocket } from "../App";

export function HomePage(): ReactElement {
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
    <main className="page-centered">
      <section className="runway" aria-hidden>
        <div className="absolute inset-x-0 top-4 flex justify-center gap-2">
          <span className="h-1.5 w-10 rounded-full bg-sky/65" />
          <span className="h-1.5 w-10 rounded-full bg-fuel/70" />
          <span className="h-1.5 w-10 rounded-full bg-radar/65" />
        </div>
        <div className="runway-plane">✈</div>
      </section>

      <div className="space-y-4">
        <p className="eyebrow">квиз на день рождения</p>
        <h1 className="hero-title">
          День рождения
          <br />
          <span className="text-accent">Вани</span>
        </h1>
        <p className="mx-auto max-w-[320px] text-sm leading-6 text-zinc-300">
          Быстрый квиз про главного авиамана МАИ: подключайся по коду,
          отвечай с телефона и забирай место в рейтинге.
        </p>
      </div>

      <div className="w-full space-y-3">
        <Button
          size="lg"
          className="primary-action text-base"
          onPress={() => navigate("/join")}
        >
          Я игрок
        </Button>
        <Button
          variant="outline"
          size="lg"
          className="secondary-action text-base"
          onPress={onCreate}
        >
          Я организатор
        </Button>
      </div>
      <p className="text-xs leading-5 text-zinc-500">
        Оптимально для телефона в портретном режиме.
      </p>
    </main>
  );
}
