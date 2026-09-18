import { useState, useCallback, useRef } from 'react';
import { Play, SkipForward, RotateCcw, FastForward, ChevronRight } from 'lucide-react';
import { initStep, nextStep, CycleSnapshot, StageState } from '../api/client';

import { SimConfig } from './ConfigPanel';

interface Props {
  code: string;
  config: SimConfig;
}

const ABI_NAMES = [
  'zero','ra','sp','gp','tp','t0','t1','t2',
  's0','s1','a0','a1','a2','a3','a4','a5',
  'a6','a7','s2','s3','s4','s5','s6','s7',
  's8','s9','s10','s11','t3','t4','t5','t6',
];

function hazardBadge(type: string) {
  const map: Record<string, { label: string; color: string }> = {
    NONE:         { label: '',              color: '' },
    LOAD_USE:     { label: '⚠ Load-Use',    color: '#f59e0b' },
    RAW:          { label: '⚠ RAW Hazard',  color: '#f59e0b' },
    MULTI_CYCLE:  { label: '⏳ Multi-Cycle', color: '#818cf8' },
    BRANCH_FLUSH: { label: '🔴 Flush',       color: '#ef4444' },
    CACHE_STALL:  { label: '⏱ Cache Stall', color: '#06b6d4' },
  };
  return map[type] ?? { label: type, color: '#94a3b8' };
}

function stageColor(stage: StageState): { border: string; bg: string; text: string } {
  if (stage.isFlush)  return { border: '#ef4444', bg: 'rgba(239,68,68,0.12)',  text: '#fca5a5' };
  if (stage.isStall)  return { border: '#f59e0b', bg: 'rgba(245,158,11,0.12)', text: '#fcd34d' };
  if (stage.isNop)    return { border: '#334155', bg: 'rgba(51,65,85,0.4)',    text: '#64748b' };
  return               { border: '#22d3ee', bg: 'rgba(34,211,238,0.1)',  text: '#e2e8f0' };
}

function StageBox({ stage }: { stage: StageState }) {
  const colors = stageColor(stage);
  return (
    <div style={{
      flex: 1,
      minWidth: 0,
      border: `2px solid ${colors.border}`,
      borderRadius: 10,
      background: colors.bg,
      padding: '12px 10px',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 6,
      transition: 'all 0.25s ease',
    }}>
      <span style={{
        fontSize: 10,
        fontWeight: 700,
        letterSpacing: 2,
        color: colors.border,
        textTransform: 'uppercase',
      }}>{stage.name}</span>
      <span style={{
        fontSize: 13,
        fontWeight: 600,
        color: colors.text,
        fontFamily: "'JetBrains Mono', monospace",
        textAlign: 'center',
        wordBreak: 'break-all',
        lineHeight: 1.4,
      }}>{stage.label}</span>
      {stage.isStall && (
        <span style={{ fontSize: 10, color: '#f59e0b', fontWeight: 600 }}>STALL</span>
      )}
      {stage.isFlush && (
        <span style={{ fontSize: 10, color: '#ef4444', fontWeight: 600 }}>FLUSHED</span>
      )}
    </div>
  );
}

function Arrow() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', color: '#475569', flexShrink: 0 }}>
      <ChevronRight size={18} />
    </div>
  );
}

export default function StepDebugger({ code, config }: Props) {
  const [snapshot, setSnapshot] = useState<CycleSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState<CycleSnapshot[]>([]);
  const runningRef = useRef(false);

  const handleInit = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const snap = await initStep(code, config);
      setSnapshot(snap);
      setHistory([snap]);
    } catch (e: any) {
      setError(e?.response?.data?.error ?? e.message ?? 'Failed to initialize');
    } finally {
      setLoading(false);
    }
  }, [code]);

  const handleNext = useCallback(async () => {
    if (!snapshot || snapshot.done) return;
    setLoading(true);
    setError(null);
    try {
      const snap = await nextStep();
      setSnapshot(snap);
      setHistory(h => [...h, snap]);
    } catch (e: any) {
      setError(e?.response?.data?.error ?? e.message ?? 'Step failed');
    } finally {
      setLoading(false);
    }
  }, [snapshot]);

  const handleRunToEnd = useCallback(async () => {
    if (!snapshot || snapshot.done) return;
    runningRef.current = true;
    setError(null);
    while (runningRef.current) {
      try {
        const snap = await nextStep();
        setSnapshot(snap);
        setHistory(h => [...h, snap]);
        if (snap.done) { runningRef.current = false; break; }
        await new Promise(r => setTimeout(r, 80)); // small delay for animation
      } catch (e: any) {
        setError(e?.response?.data?.error ?? e.message ?? 'Step failed');
        runningRef.current = false;
        break;
      }
    }
  }, [snapshot]);

  const handleReset = useCallback(() => {
    runningRef.current = false;
    setSnapshot(null);
    setHistory([]);
    setError(null);
  }, []);

  const regs = snapshot?.registers ?? new Array(32).fill(0);

  return (
    <div style={{
      position: 'absolute',
      inset: 0,
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
      padding: '20px',
      overflowY: 'auto',
      fontFamily: "'Inter', sans-serif",
    }}>

      {/* Controls */}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
        <button onClick={handleInit} disabled={loading} style={btnStyle('#22d3ee')}>
          <Play size={14} /> Initialize
        </button>
        <button onClick={handleNext} disabled={!snapshot || snapshot.done || loading} style={btnStyle('#4ade80')}>
          <SkipForward size={14} /> Step →
        </button>
        <button onClick={handleRunToEnd} disabled={!snapshot || snapshot.done || loading} style={btnStyle('#818cf8')}>
          <FastForward size={14} /> Run to End
        </button>
        <button onClick={() => { runningRef.current = false; }} disabled={!runningRef.current} style={btnStyle('#f59e0b')}>
          ⏸ Pause
        </button>
        <button onClick={handleReset} style={btnStyle('#ef4444')}>
          <RotateCcw size={14} /> Reset
        </button>

        {snapshot && (
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 20, fontSize: 13 }}>
            <Stat label="Cycle" value={snapshot.totalCycles} />
            <Stat label="Stalls" value={snapshot.totalStalls} color="#f59e0b" />
            <Stat label="Flushes" value={snapshot.totalFlushes} color="#ef4444" />
            <Stat label="Retired" value={snapshot.instructionsRetired} color="#4ade80" />
            <Stat label="IPC" value={
              snapshot.totalCycles > 0
                ? (snapshot.instructionsRetired / snapshot.totalCycles).toFixed(3)
                : '—'
            } color="#22d3ee" />
          </div>
        )}
      </div>

      {error && (
        <div style={{
          background: 'rgba(239,68,68,0.15)', border: '1px solid #ef4444',
          borderRadius: 8, padding: '10px 14px', color: '#fca5a5', fontSize: 13,
        }}>{error}</div>
      )}

      {/* Info banner — always visible */}
      <div style={{
        display: 'flex', alignItems: 'flex-start', gap: 10,
        background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.35)',
        borderRadius: 8, padding: '10px 14px', fontSize: 12, color: '#a5b4fc',
      }}>
        <span style={{ fontSize: 16, flexShrink: 0 }}>ℹ️</span>
        <span>
          <strong style={{ color: '#c7d2fe' }}>Tip:</strong> If you see many{' '}
          <span style={{ color: '#06b6d4' }}>⏱ Cache Stall</span> cycles and want to focus purely on 
          pipeline hazards (RAW, load-use, branches), you can disable the <strong>L1D Cache</strong> in the{' '}
          <strong style={{ color: '#c7d2fe' }}>Config</strong> tab.
        </span>
      </div>

      {!snapshot && (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', flex: 1, gap: 12, color: '#475569',
        }}>
          <SkipForward size={40} strokeWidth={1} />
          <p style={{ margin: 0, fontSize: 15 }}>Click <strong style={{ color: '#22d3ee' }}>Initialize</strong> to load your assembly into the debugger.</p>
        </div>
      )}

      {snapshot && (
        <>
          {/* Hazard badge */}
          {snapshot.hazardType !== 'NONE' && (() => {
            const badge = hazardBadge(snapshot.hazardType);
            return (
              <div style={{
                alignSelf: 'flex-start', padding: '4px 14px', borderRadius: 20,
                background: `${badge.color}22`, border: `1px solid ${badge.color}`,
                color: badge.color, fontSize: 12, fontWeight: 700,
              }}>{badge.label}</div>
            );
          })()}

          {/* Pipeline diagram */}
          <div style={{
            display: 'flex', gap: 6, alignItems: 'stretch',
            background: 'rgba(15,23,42,0.6)', border: '1px solid #1e293b',
            borderRadius: 12, padding: 16,
          }}>
            {snapshot.stages.map((stage, i) => (
              <>
                <StageBox key={stage.name} stage={stage} />
                {i < 4 && <Arrow key={`arrow-${i}`} />}
              </>
            ))}
          </div>

          {snapshot.done && (
            <div style={{
              textAlign: 'center', padding: '10px', borderRadius: 8,
              background: 'rgba(74,222,128,0.1)', border: '1px solid #4ade80',
              color: '#4ade80', fontWeight: 700, fontSize: 14,
            }}>✓ Simulation Complete</div>
          )}

          {/* Register File */}
          <div style={{
            background: 'rgba(15,23,42,0.6)', border: '1px solid #1e293b',
            borderRadius: 12, padding: 16,
          }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#64748b', letterSpacing: 2, marginBottom: 12 }}>
              REGISTER FILE
            </div>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: '6px 12px',
            }}>
              {regs.map((val, i) => {
                const changed = history.length > 1 &&
                  history[history.length - 2].registers[i] !== val;
                return (
                  <div key={i} style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '4px 8px', borderRadius: 6,
                    background: changed ? 'rgba(34,211,238,0.15)' : 'rgba(30,41,59,0.6)',
                    border: changed ? '1px solid rgba(34,211,238,0.4)' : '1px solid transparent',
                    transition: 'all 0.3s ease',
                  }}>
                    <span style={{ fontSize: 11, color: '#64748b', fontFamily: 'monospace' }}>
                      x{i}
                    </span>
                    <span style={{ fontSize: 11, color: '#94a3b8', fontFamily: 'monospace' }}>
                      {ABI_NAMES[i]}
                    </span>
                    <span style={{
                      fontSize: 12, fontWeight: 600, fontFamily: 'monospace',
                      color: changed ? '#22d3ee' : (val !== 0 ? '#e2e8f0' : '#475569'),
                    }}>{val}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Cycle history log */}
          {history.length > 1 && (
            <div style={{
              background: 'rgba(15,23,42,0.6)', border: '1px solid #1e293b',
              borderRadius: 12, padding: 16, minHeight: 300, overflowY: 'auto',
            }}>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#64748b', letterSpacing: 2, marginBottom: 10 }}>
                CYCLE HISTORY
              </div>
              {[...history].reverse().map((snap, idx) => (
                <div key={idx} style={{
                  display: 'flex', gap: 10, alignItems: 'center',
                  padding: '4px 0', borderBottom: '1px solid rgba(30,41,59,0.6)',
                  fontSize: 12, fontFamily: 'monospace',
                }}>
                  <span style={{ color: '#cbd5e1', minWidth: 60 }}>Cycle {snap.cycle}</span>
                  {snap.hazardType !== 'NONE' && (
                    <span style={{ color: hazardBadge(snap.hazardType).color, fontSize: 11 }}>
                      {hazardBadge(snap.hazardType).label}
                    </span>
                  )}
                  {snap.stages.filter(s => !s.isNop).map(s => (
                    <span key={s.name} style={{ color: '#f8fafc' }}>[{s.name}: {s.label}]</span>
                  ))}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function btnStyle(color: string) {
  return {
    display: 'flex', alignItems: 'center', gap: 6,
    padding: '7px 14px', borderRadius: 8, border: `1px solid ${color}44`,
    background: `${color}18`, color: color, cursor: 'pointer',
    fontSize: 13, fontWeight: 600, transition: 'all 0.2s',
  } as React.CSSProperties;
}

function Stat({ label, value, color = '#e2e8f0' }: { label: string; value: any; color?: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
      <span style={{ fontSize: 10, color: '#64748b', letterSpacing: 1 }}>{label.toUpperCase()}</span>
      <span style={{ fontSize: 16, fontWeight: 700, color, fontFamily: 'monospace' }}>{value}</span>
    </div>
  );
}
