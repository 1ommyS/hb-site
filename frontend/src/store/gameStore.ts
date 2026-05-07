import { create } from "zustand";
import type {
  PlayerDto,
  QuestionResultPayload,
  QuestionStartedPayload,
  QuizFinishedPayload,
  RoomStateSyncPayload,
  RoomStatus,
} from "../types";

export type Role = "player" | "organizer" | null;

interface GameState {
  role: Role;
  sessionId: string;
  myPlayer: PlayerDto | null;
  organizerToken: string | null;
  roomCode: string | null;
  roomId: string | null;
  status: RoomStatus | null;
  totalQuestions: number;
  currentQuestionIndex: number;
  players: PlayerDto[];
  annoyingModeEnabled: boolean;

  currentQuestion: QuestionStartedPayload | null;
  questionResult: QuestionResultPayload | null;
  finalRanking: QuizFinishedPayload | null;

  selectedOptions: string[];
  answerSubmitted: boolean;
  errorMessage: string | null;

  setRole: (r: Role) => void;
  setError: (msg: string | null) => void;
  setRoomFromCreated: (data: {
    roomId: string;
    code: string;
    organizerToken: string;
    totalQuestions: number;
  }) => void;
  setMyPlayer: (p: PlayerDto, code: string, roomId: string) => void;
  applyStateSync: (p: RoomStateSyncPayload) => void;
  setQuestionStarted: (q: QuestionStartedPayload) => void;
  setQuestionResult: (r: QuestionResultPayload) => void;
  setQuizFinished: (q: QuizFinishedPayload) => void;
  setPlayers: (ps: PlayerDto[]) => void;
  toggleSelected: (optionId: string, multi: boolean) => void;
  markAnswerSubmitted: () => void;
  reset: () => void;
}

const SESSION_KEY = "hb-session-id";

function getOrCreateSessionId(): string {
  const existing = window.localStorage.getItem(SESSION_KEY);
  if (existing) return existing;
  const id = crypto.randomUUID();
  window.localStorage.setItem(SESSION_KEY, id);
  return id;
}

export const useGame = create<GameState>((set) => ({
  role: null,
  sessionId: getOrCreateSessionId(),
  myPlayer: null,
  organizerToken: null,
  roomCode: null,
  roomId: null,
  status: null,
  totalQuestions: 0,
  currentQuestionIndex: -1,
  players: [],
  annoyingModeEnabled: true,
  currentQuestion: null,
  questionResult: null,
  finalRanking: null,
  selectedOptions: [],
  answerSubmitted: false,
  errorMessage: null,

  setRole: (r) => set({ role: r }),
  setError: (msg) => set({ errorMessage: msg }),

  setRoomFromCreated: ({ roomId, code, organizerToken, totalQuestions }) =>
    set({
      role: "organizer",
      roomId,
      roomCode: code,
      organizerToken,
      totalQuestions,
      status: "WAITING",
    }),

  setMyPlayer: (p, code, roomId) =>
    set({
      myPlayer: p,
      role: "player",
      roomCode: code,
      roomId,
    }),

  applyStateSync: (p) =>
    set({
      roomId: p.roomId,
      roomCode: p.code,
      status: p.status,
      currentQuestionIndex: p.currentQuestionIndex,
      totalQuestions: p.totalQuestions,
      players: p.players,
      annoyingModeEnabled: p.annoyingModeEnabled,
    }),

  setQuestionStarted: (q) =>
    set({
      currentQuestion: q,
      questionResult: null,
      status: "QUESTION_ACTIVE",
      currentQuestionIndex: q.questionNumber - 1,
      totalQuestions: q.totalQuestions,
      selectedOptions: [],
      answerSubmitted: false,
    }),

  setQuestionResult: (r) =>
    set({
      questionResult: r,
      status: "QUESTION_RESULT",
    }),

  setQuizFinished: (q) =>
    set({
      finalRanking: q,
      status: "FINISHED",
    }),

  setPlayers: (ps) => set({ players: ps }),

  toggleSelected: (optionId, multi) =>
    set((state) => {
      if (state.answerSubmitted) return state;
      if (!multi) return { selectedOptions: [optionId] };
      const has = state.selectedOptions.includes(optionId);
      return {
        selectedOptions: has
          ? state.selectedOptions.filter((o) => o !== optionId)
          : [...state.selectedOptions, optionId],
      };
    }),

  markAnswerSubmitted: () => set({ answerSubmitted: true }),

  reset: () =>
    set({
      role: null,
      myPlayer: null,
      organizerToken: null,
      roomCode: null,
      roomId: null,
      status: null,
      currentQuestion: null,
      questionResult: null,
      finalRanking: null,
      players: [],
      selectedOptions: [],
      answerSubmitted: false,
      errorMessage: null,
    }),
}));
