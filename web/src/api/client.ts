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

export const runSimulation = async (): Promise<SimResult> => {
  const { data } = await api.post('/run');
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
