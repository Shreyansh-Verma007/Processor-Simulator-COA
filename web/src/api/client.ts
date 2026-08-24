import axios from 'axios';

// In dev: Vite proxies /api → localhost:8080 (see vite.config.ts)
// In production: set VITE_API_URL to your deployed Java backend URL
// e.g. https://your-backend.railway.app
const BASE = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}/api`
  : '/api';

const api = axios.create({
  baseURL: BASE,
  timeout: 120_000,
});

export interface SimResult {
  ok: boolean;
  stderr?: string;
  error?: string;
}

export interface SimConfig {
  forwardingEnabled: boolean;
  l1dSizeKb: number;
  l1dBlockBytes: number;
  l1dAssoc: number;
  l1dLatency: number;
  l1iEnabled: boolean;
  l2Enabled: boolean;
  l2SizeKb: number;
  l2Assoc: number;
  memoryLatency: number;
  mulLatency: number;
  divLatency: number;
}

export const getStatus = async (): Promise<{ status: 'idle' | 'running' }> => {
  const { data } = await api.get('/status');
  return data;
};

export const getAsm = async (): Promise<string> => {
  const { data } = await api.get('/asm');
  return data.content ?? '';
};

export const saveAsm = async (content: string): Promise<void> => {
  await api.post('/asm', content, { headers: { 'Content-Type': 'text/plain' } });
};

export const runSimulation = async (asmCode: string, cfg?: SimConfig): Promise<SimResult> => {
  // Encode config as URL query params so the Java RunHandler can parse them
  const params = new URLSearchParams();
  if (cfg) {
    params.set('forwarding',  String(cfg.forwardingEnabled));
    params.set('l1dSize',     String(cfg.l1dSizeKb * 1024));
    params.set('l1dBlock',    String(cfg.l1dBlockBytes));
    params.set('l1dAssoc',    String(cfg.l1dAssoc));
    params.set('l1dLatency',  String(cfg.l1dLatency));
    params.set('l1iEnabled',  String(cfg.l1iEnabled));
    params.set('l2Enabled',   String(cfg.l2Enabled));
    params.set('l2Size',      String(cfg.l2SizeKb * 1024));
    params.set('l2Assoc',     String(cfg.l2Assoc));
    params.set('memLatency',  String(cfg.memoryLatency));
    params.set('mulLatency',  String(cfg.mulLatency));
    params.set('divLatency',  String(cfg.divLatency));
  }
  const url = params.toString() ? `/run?${params.toString()}` : '/run';
  const { data } = await api.post(url, asmCode, {
    headers: { 'Content-Type': 'text/plain' },
  });
  return data;
};

export const getConsole = async (): Promise<string> => {
  const { data } = await api.get('/console');
  return data.content ?? '';
};

export const getOutput = async (): Promise<string> => {
  const { data } = await api.get('/output');
  return data.content ?? '';
};

export const getSwap = async (): Promise<string> => {
  const { data } = await api.get('/swap');
  return data.content ?? '';
};

export interface TraceResult {
  ok: boolean;
  content?: string;
  error?: string;
}

export const runTrace = async (file: File): Promise<TraceResult> => {
  const bytes = await file.arrayBuffer();
  const { data } = await api.post('/trace', bytes, {
    headers: { 'Content-Type': 'application/octet-stream' },
    timeout: 300_000, // trace files are large — allow 5 min
  });
  return data;
};

export const listTraces = async (): Promise<string[]> => {
  const { data } = await api.get('/traces');
  return data.files ?? [];
};
