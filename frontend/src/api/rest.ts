import type {
  PlayerDto,
  QuizFinishedPayload,
  RoomCreatedPayload,
  RoomInfoResponse,
} from "../types";

async function request<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(body || `HTTP ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const restApi = {
  createRoom: () =>
    request<RoomCreatedPayload>("/api/rooms", { method: "POST" }),

  getRoom: (code: string) =>
    request<RoomInfoResponse>(`/api/rooms/${encodeURIComponent(code)}`),

  joinRoom: (code: string, name: string, sessionId: string) =>
    request<{ roomId: string; player: PlayerDto; isReconnect: boolean }>(
      `/api/rooms/${encodeURIComponent(code)}/players`,
      {
        method: "POST",
        body: JSON.stringify({ name, sessionId }),
      },
    ),

  results: (code: string) =>
    request<QuizFinishedPayload>(`/api/rooms/${encodeURIComponent(code)}/results`),

  stats: (code: string) =>
    request<unknown>(`/api/rooms/${encodeURIComponent(code)}/stats`),
};
