import { useEffect, useMemo } from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import { GameSocket, buildWsUrl } from "./api/ws";
import { useGame } from "./store/gameStore";
import { HomePage } from "./pages/HomePage";
import { JoinPage } from "./pages/JoinPage";
import { OrganizerLobbyPage } from "./pages/OrganizerLobbyPage";
import { PlayerLobbyPage } from "./pages/PlayerLobbyPage";
import { QuestionPage } from "./pages/QuestionPage";
import { ResultPage } from "./pages/ResultPage";
import { FinalRankingPage } from "./pages/FinalRankingPage";
import { ErrorBanner } from "./components/ErrorBanner";
import type {
  AnswerAcceptedPayload,
  AnswerRejectedPayload,
  ErrorPayload,
  PlayerJoinedPayload,
  QuestionResultPayload,
  QuestionStartedPayload,
  QuizFinishedPayload,
  RoomStateSyncPayload,
} from "./types";

let socketSingleton: GameSocket | null = null;

export function getSocket(): GameSocket {
  if (!socketSingleton) {
    socketSingleton = new GameSocket(buildWsUrl());
    socketSingleton.connect();
  }
  return socketSingleton;
}

function useSocketBridge(): void {
  const setError = useGame((s) => s.setError);
  const applyStateSync = useGame((s) => s.applyStateSync);
  const setQuestionStarted = useGame((s) => s.setQuestionStarted);
  const setQuestionResult = useGame((s) => s.setQuestionResult);
  const setQuizFinished = useGame((s) => s.setQuizFinished);
  const setPlayers = useGame((s) => s.setPlayers);
  const markSubmitted = useGame((s) => s.markAnswerSubmitted);

  useEffect(() => {
    const socket = getSocket();
    return socket.on((env) => {
      switch (env.type) {
        case "ROOM_STATE_SYNC":
          applyStateSync(env.payload as RoomStateSyncPayload);
          break;
        case "PLAYER_JOINED":
        case "PLAYER_LEFT": {
          const p = env.payload as PlayerJoinedPayload;
          setPlayers(p.players);
          break;
        }
        case "QUIZ_STARTED":
          break;
        case "QUESTION_STARTED":
          setQuestionStarted(env.payload as QuestionStartedPayload);
          break;
        case "ANSWER_ACCEPTED":
          markSubmitted();
          void (env.payload as AnswerAcceptedPayload);
          break;
        case "ANSWER_REJECTED": {
          const p = env.payload as AnswerRejectedPayload;
          setError(p.reason);
          break;
        }
        case "QUESTION_FINISHED":
          break;
        case "QUESTION_RESULT":
          setQuestionResult(env.payload as QuestionResultPayload);
          break;
        case "QUIZ_FINISHED":
          setQuizFinished(env.payload as QuizFinishedPayload);
          break;
        case "ERROR": {
          const p = env.payload as ErrorPayload;
          setError(p.message);
          break;
        }
        default:
          break;
      }
    });
  }, [applyStateSync, setQuestionStarted, setQuestionResult, setQuizFinished, setPlayers, markSubmitted, setError]);
}

function GameRouter(): JSX.Element {
  const status = useGame((s) => s.status);
  const role = useGame((s) => s.role);
  const location = useLocation();

  const target = useMemo(() => {
    if (!role) return null;
    if (status === "QUESTION_ACTIVE") return "/question";
    if (status === "QUESTION_RESULT") return "/result";
    if (status === "FINISHED") return "/final";
    if (status === "WAITING" || status === "IN_PROGRESS") {
      return role === "organizer" ? "/organizer" : "/player";
    }
    return null;
  }, [status, role]);

  useEffect(() => {
    if (target && location.pathname !== target) {
      window.history.replaceState({}, "", target);
    }
  }, [target, location.pathname]);

  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/join" element={<JoinPage />} />
      <Route path="/organizer" element={<OrganizerLobbyPage />} />
      <Route path="/player" element={<PlayerLobbyPage />} />
      <Route path="/question" element={<QuestionPage />} />
      <Route path="/result" element={<ResultPage />} />
      <Route path="/final" element={<FinalRankingPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App(): JSX.Element {
  useSocketBridge();
  return (
    <div className="app bg-zinc-950 text-zinc-100">
      <div className="landscape-blocker">
        Поверни телефон обратно, самолёту нужен вертикальный взлёт ✈️
      </div>
      <ErrorBanner />
      <div className="quiz-container">
        <GameRouter />
      </div>
    </div>
  );
}
