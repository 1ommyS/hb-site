export type RoomStatus =
  | "WAITING"
  | "IN_PROGRESS"
  | "QUESTION_ACTIVE"
  | "QUESTION_RESULT"
  | "FINISHED";

export type QuestionType = "single" | "multiple";

export interface PlayerDto {
  id: string;
  name: string;
  score: number;
}

export interface OptionDto {
  id: string;
  text: string;
}

export interface RoomCreatedPayload {
  roomId: string;
  code: string;
  organizerToken: string;
  quizTitle: string;
  totalQuestions: number;
  joinUrl: string;
  websocketUrl: string;
}

export interface RoomInfoResponse {
  roomId: string;
  code: string;
  status: RoomStatus;
  totalQuestions: number;
  currentQuestionIndex: number;
  players: PlayerDto[];
  createdAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  annoyingModeEnabled: boolean;
  manualMode: boolean;
}

export interface RoomStateSyncPayload {
  roomId: string;
  code: string;
  status: RoomStatus;
  currentQuestionIndex: number;
  totalQuestions: number;
  players: PlayerDto[];
  questionStartedAt: string | null;
  questionEndsAt: string | null;
  annoyingModeEnabled: boolean;
}

export interface QuestionStartedPayload {
  roomId: string;
  questionId: string;
  questionNumber: number;
  totalQuestions: number;
  text: string;
  options: OptionDto[];
  type: QuestionType;
  startedAt: string;
  endsAt: string;
}

export interface PlayerAnswerStat {
  playerId: string;
  name: string;
  selectedOptions: string[];
  isCorrect: boolean;
  pointsEarned: number;
  totalScore: number;
}

export interface QuestionResultPayload {
  roomId: string;
  questionId: string;
  correctOptions: string[];
  comment: string | null;
  distribution: Record<string, number>;
  playerAnswers: PlayerAnswerStat[];
  ranking: PlayerDto[];
}

export interface FinalRankingRow {
  rank: number;
  playerId: string;
  name: string;
  score: number;
  correctAnswers: number;
  averageAnswerTimeMs: number;
}

export interface QuizFinishedPayload {
  roomId: string;
  ranking: FinalRankingRow[];
}

export interface QuestionDistributionDto {
  questionId: string;
  questionNumber: number;
  text: string;
  correctOptions: string[];
  distribution: Record<string, number>;
}

export interface StatsResponse {
  roomId: string;
  mostPopularWrong: string | null;
  hardestQuestionId: string | null;
  unanimouslyCorrectQuestionId: string | null;
  confusingQuestionId: string | null;
  perQuestion: QuestionDistributionDto[];
}

export interface PlayerJoinedPayload {
  roomId: string;
  player: PlayerDto;
  players: PlayerDto[];
}

export interface AnswerAcceptedPayload {
  questionId: string;
  receivedAt: string;
}

export interface AnswerRejectedPayload {
  questionId: string;
  reason: string;
}

export interface ErrorPayload {
  message: string;
  code?: string;
}

export type ServerEventType =
  | "ROOM_CREATED"
  | "PLAYER_JOINED"
  | "PLAYER_LEFT"
  | "QUIZ_STARTED"
  | "QUESTION_STARTED"
  | "ANSWER_ACCEPTED"
  | "ANSWER_REJECTED"
  | "QUESTION_FINISHED"
  | "QUESTION_RESULT"
  | "QUIZ_FINISHED"
  | "ROOM_STATE_SYNC"
  | "ERROR"
  | "PONG";

export type ClientEventType =
  | "JOIN_ROOM"
  | "START_QUIZ"
  | "SUBMIT_ANSWER"
  | "NEXT_QUESTION"
  | "FINISH_QUIZ"
  | "PING";

export interface WsEnvelope<T = unknown> {
  type: ServerEventType | ClientEventType;
  payload?: T;
}
