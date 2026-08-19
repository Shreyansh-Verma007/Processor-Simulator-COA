import { Play, Square, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import type { SimStatus } from '../hooks/useSimulator';

interface RunButtonProps {
  status: SimStatus;
  onRun: () => void;
  onStop?: () => void;
  disabled?: boolean;
  durationMs?: number | null;
}

export default function RunButton({ status, onRun, disabled, durationMs }: RunButtonProps) {
  const isRunning = status === 'running';
  const isSuccess = status === 'success';
  const isError   = status === 'error';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <button
        className={`btn ${isRunning ? 'btn-danger' : 'btn-primary'}`}
        onClick={onRun}
        disabled={disabled || isRunning}
        style={{
          minWidth: 130,
          opacity: (disabled && !isRunning) ? 0.5 : 1,
          cursor: (disabled || isRunning) ? 'not-allowed' : 'pointer',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {isRunning ? (
          <>
            <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />
            Simulating…
          </>
        ) : (
          <>
            <Play size={15} fill="currentColor" />
            Run Simulation
          </>
        )}
      </button>

      {/* Status badge */}
      {isSuccess && durationMs != null && (
        <div className="animate-fade-in" style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--accent-green)', fontSize: 12 }}>
          <CheckCircle2 size={14} />
          <span>Done in {(durationMs / 1000).toFixed(2)}s</span>
        </div>
      )}
      {isError && (
        <div className="animate-fade-in" style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--accent-orange)', fontSize: 12 }}>
          <XCircle size={14} />
          <span>Error</span>
        </div>
      )}
      {isRunning && (
        <div style={{ width: 120 }}>
          <div className="progress-bar" />
        </div>
      )}
    </div>
  );
}
