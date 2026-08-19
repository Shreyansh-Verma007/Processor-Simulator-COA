import { useState, useCallback, useRef } from 'react';
import {
  getAsm, saveAsm, runSimulation,
  getConsole, getOutput, getSwap,
  getStatus,
} from '../api/client';

export type SimStatus = 'idle' | 'running' | 'success' | 'error' | 'offline';

export interface SimulatorState {
  status: SimStatus;
  asmCode: string;
  consoleContent: string;
  outputContent: string;
  swapContent: string;
  errorMessage: string;
  runDurationMs: number | null;
  backendOnline: boolean;
}

export function useSimulator() {
  const [state, setState] = useState<SimulatorState>({
    status: 'idle',
    asmCode: '',
    consoleContent: '',
    outputContent: '',
    swapContent: '',
    errorMessage: '',
    runDurationMs: null,
    backendOnline: false,
  });

  const startTimeRef = useRef<number>(0);

  const checkBackend = useCallback(async () => {
    try {
      await getStatus();
      setState(s => ({ ...s, backendOnline: true }));
      return true;
    } catch {
      setState(s => ({ ...s, backendOnline: false, status: 'offline' }));
      return false;
    }
  }, []);

  const loadAsm = useCallback(async () => {
    const online = await checkBackend();
    if (!online) return;
    try {
      const code = await getAsm();
      setState(s => ({ ...s, asmCode: code }));
    } catch {
      // Silently ignore — editor starts empty
    }
  }, [checkBackend]);

  const loadOutputFiles = useCallback(async () => {
    const [con, out, swp] = await Promise.all([
      getConsole(), getOutput(), getSwap(),
    ]);
    setState(s => ({
      ...s,
      consoleContent: con,
      outputContent: out,
      swapContent: swp,
    }));
  }, []);

  const run = useCallback(async (asmCode: string) => {
    const online = await checkBackend();
    if (!online) return;

    setState(s => ({ ...s, status: 'running', errorMessage: '' }));
    startTimeRef.current = Date.now();

    try {
      // Save ASM first
      await saveAsm(asmCode);

      // Run simulation (blocks until done)
      const result = await runSimulation();

      const elapsed = Date.now() - startTimeRef.current;

      if (result.ok) {
        await loadOutputFiles();
        setState(s => ({
          ...s,
          status: 'success',
          asmCode,
          runDurationMs: elapsed,
        }));
      } else {
        setState(s => ({
          ...s,
          status: 'error',
          errorMessage: result.error ?? 'Unknown error',
          runDurationMs: elapsed,
        }));
      }
    } catch (err: unknown) {
      const elapsed = Date.now() - startTimeRef.current;
      const msg = err instanceof Error ? err.message : String(err);
      setState(s => ({
        ...s,
        status: 'error',
        errorMessage: msg,
        runDurationMs: elapsed,
      }));
    }
  }, [checkBackend, loadOutputFiles]);

  const setAsmCode = useCallback((code: string) => {
    setState(s => ({ ...s, asmCode: code }));
  }, []);

  return {
    ...state,
    checkBackend,
    loadAsm,
    loadOutputFiles,
    run,
    setAsmCode,
  };
}
