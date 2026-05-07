import type { ClientEventType, ServerEventType, WsEnvelope } from "../types";

type Listener = (env: WsEnvelope) => void;

export class GameSocket {
  private socket: WebSocket | null = null;
  private url: string;
  private listeners = new Set<Listener>();
  private explicitClose = false;
  private reconnectDelay = 800;
  private pingTimer?: number;
  private pendingJoin: object | null = null;

  constructor(url: string) {
    this.url = url;
  }

  connect(): void {
    this.explicitClose = false;
    const ws = new WebSocket(this.url);
    this.socket = ws;
    ws.onopen = () => {
      this.reconnectDelay = 800;
      if (this.pendingJoin) this.sendRaw("JOIN_ROOM", this.pendingJoin);
      this.pingTimer = window.setInterval(() => this.sendRaw("PING"), 25_000);
    };
    ws.onmessage = (ev) => {
      try {
        const env = JSON.parse(ev.data) as WsEnvelope;
        this.listeners.forEach((l) => l(env));
      } catch {
        /* ignore parse errors */
      }
    };
    ws.onclose = () => {
      window.clearInterval(this.pingTimer);
      this.socket = null;
      if (!this.explicitClose) {
        const delay = Math.min(this.reconnectDelay, 8000);
        this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 8000);
        window.setTimeout(() => this.connect(), delay);
      }
    };
    ws.onerror = () => {
      ws.close();
    };
  }

  setJoin(payload: object): void {
    this.pendingJoin = payload;
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.sendRaw("JOIN_ROOM", payload);
    }
  }

  send(type: ClientEventType, payload?: unknown): void {
    this.sendRaw(type, payload);
  }

  private sendRaw(type: string, payload?: unknown): void {
    if (this.socket?.readyState !== WebSocket.OPEN) return;
    this.socket.send(JSON.stringify({ type, payload }));
  }

  on(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  close(): void {
    this.explicitClose = true;
    window.clearInterval(this.pingTimer);
    this.socket?.close();
    this.socket = null;
  }
}

export function buildWsUrl(): string {
  const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${proto}//${window.location.host}/ws`;
}

export type ServerListener = (
  type: ServerEventType,
  payload: unknown,
) => void;
