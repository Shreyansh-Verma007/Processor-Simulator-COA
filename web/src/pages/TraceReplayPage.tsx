import { useState, useCallback, useEffect, useRef } from 'react';
import {
  Upload, FileText, Play, CheckCircle2, XCircle, Loader2,
  Activity, Layers, HardDrive, Cpu, Clock, Zap,
} from 'lucide-react';
import { runTrace, listTraces } from '../api/client';

// ── Types ─────────────────────────────────────────────────────────────────────

type RunState = 'idle' | 'running' | 'success' | 'error';

interface ParsedTraceStats {
  // Execution
  totalCycles:           number | null;
  instructionsRetired:   number | null;
  ipc:                   number | null;
  stalls:                number | null;
  // VM
  tlbHits:               number | null;
  tlbMisses:             number | null;
  tlbHitRate:            number | null;
  pageWalks:             number | null;
  pageFaults:            number | null;
  pageEvictions:         number | null;
  dirtyEvictions:        number | null;
  swapOuts:              number | null;
  swapIns:               number | null;
  translationPenalty:    number | null;
  // Cache
  l1iHits:  number | null; l1iMisses:  number | null;
  l1dHits:  number | null; l1dMisses:  number | null;
  l2Hits:   number | null; l2Misses:   number | null;
}

// ── Parsing ───────────────────────────────────────────────────────────────────

function getNum(lines: string[], key: string): number | null {
  for (const l of lines) {
    const r = new RegExp(key + '\\s*:\\s*([\\d.]+)');
    const m = l.match(r);
    if (m) return parseFloat(m[1]);
  }
  return null;
}

function parseTraceStats(raw: string): ParsedTraceStats {
  const lines = raw.split(/\r?\n/);
  return {
    totalCycles:         getNum(lines, 'Total Cycles'),
    instructionsRetired: getNum(lines, 'Instructions Retired'),
    ipc:                 getNum(lines, 'IPC'),
    stalls:              getNum(lines, 'Stalls'),
    tlbHits:             getNum(lines, 'TLB Hits'),
    tlbMisses:           getNum(lines, 'TLB Misses'),
    tlbHitRate:          getNum(lines, 'TLB Hit Rate'),
    pageWalks:           getNum(lines, 'Page Walks'),
    pageFaults:          getNum(lines, 'Page Faults'),
    pageEvictions:       getNum(lines, 'Page Evictions'),
    dirtyEvictions:      getNum(lines, 'Dirty Evictions'),
    swapOuts:            getNum(lines, 'Swap Outs'),
    swapIns:             getNum(lines, 'Swap Ins'),
    translationPenalty:  getNum(lines, 'Translation Penalty Cycles'),
    l1iHits:   getNum(lines, 'L1I Hits'),   l1iMisses:  getNum(lines, 'L1I Misses'),
    l1dHits:   getNum(lines, 'L1D Hits'),   l1dMisses:  getNum(lines, 'L1D Misses'),
    l2Hits:    getNum(lines, 'L2 Hits'),    l2Misses:   getNum(lines, 'L2 Misses'),
  };
}

function fmt(n: number | null, dec = 0): string {
  if (n === null) return '—';
  return dec > 0 ? n.toFixed(dec) : n.toLocaleString();
}

function missRate(h: number | null, m: number | null): string {
  if (h === null || m === null) return '—';
  const t = h + m;
  return t === 0 ? '0.0000' : (m / t).toFixed(4);
}

// ── Sub-components ────────────────────────────────────────────────────────────

function StatCard({ label, value, sub, icon, color = 'var(--accent-cyan)' }: {
  label: string; value: string; sub?: string; icon: React.ReactNode; color?: string;
}) {
  return (
    <div className="stat-card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span style={{ fontSize: 10, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          {label}
        </span>
        <span style={{ color, opacity: 0.8 }}>{icon}</span>
      </div>
      <div style={{ fontSize: 24, fontWeight: 700, color, fontFamily: 'var(--font-mono)', lineHeight: 1 }}>
        {value}
      </div>
      {sub && <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>{sub}</div>}
    </div>
  );
}

function SectionHeader({ icon, title, color = 'var(--accent-cyan)' }: { icon: React.ReactNode; title: string; color?: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
      <span style={{ color }}>{icon}</span>
      <span style={{ fontWeight: 700, fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>{title}</span>
    </div>
  );
}

function CacheRow({ name, hits, misses }: { name: string; hits: number | null; misses: number | null }) {
  const mr = missRate(hits, misses);
  const n = parseFloat(mr);
  const c = isNaN(n) ? 'var(--text-muted)'
    : n === 0 ? 'var(--accent-green)'
    : n < 0.05 ? 'var(--accent-yellow)'
    : 'var(--accent-orange)';
  return (
    <tr>
      <td style={{ fontWeight: 600 }}>{name}</td>
      <td style={{ color: 'var(--accent-green)', fontFamily: 'var(--font-mono)' }}>{fmt(hits)}</td>
      <td style={{ color: 'var(--accent-orange)', fontFamily: 'var(--font-mono)' }}>{fmt(misses)}</td>
      <td style={{ color: c, fontFamily: 'var(--font-mono)' }}>{mr}</td>
    </tr>
  );
}

// ── Stats display ─────────────────────────────────────────────────────────────

function TraceStatsDisplay({ raw, fileName }: { raw: string; fileName: string }) {
  const s = parseTraceStats(raw);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* File name badge */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <FileText size={14} color="var(--accent-cyan)" />
        <span style={{ fontWeight: 600, fontSize: 14, color: 'var(--text-primary)' }}>
          {fileName}
        </span>
        <span className="badge badge-cyan">Trace Replay</span>
      </div>

      {/* Execution */}
      <section>
        <SectionHeader icon={<Cpu size={13} />} title="Execution" />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 10 }}>
          <StatCard label="Total Cycles"   value={fmt(s.totalCycles)}         icon={<Clock size={15} />} color="var(--accent-cyan)" />
          <StatCard label="Instructions"   value={fmt(s.instructionsRetired)}  icon={<Zap size={15} />}  color="var(--accent-green)" />
          <StatCard label="IPC"            value={fmt(s.ipc, 4)}              icon={<Activity size={15} />} color="var(--accent-blue)" sub="Instr per cycle" />
          <StatCard label="Stalls"         value={fmt(s.stalls)}              icon={<Activity size={15} />} color="var(--accent-orange)" />
        </div>
      </section>

      {/* Virtual Memory */}
      <section>
        <SectionHeader icon={<HardDrive size={13} />} title="Virtual Memory" color="var(--accent-purple)" />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 10 }}>
          <StatCard label="TLB Hits"       value={fmt(s.tlbHits)}       icon={<Zap size={15} />}      color="var(--accent-green)" />
          <StatCard label="TLB Misses"     value={fmt(s.tlbMisses)}     icon={<Activity size={15} />} color="var(--accent-orange)" />
          <StatCard label="TLB Hit Rate"   value={fmt(s.tlbHitRate, 4)} icon={<Activity size={15} />} color="var(--accent-blue)" />
          <StatCard label="Page Walks"     value={fmt(s.pageWalks)}     icon={<Activity size={15} />} color="var(--accent-purple)" />
          <StatCard label="Page Faults"    value={fmt(s.pageFaults)}    icon={<Activity size={15} />} color="var(--accent-orange)" />
          <StatCard label="Swap Outs"      value={fmt(s.swapOuts)}      icon={<HardDrive size={15}/>} color="var(--accent-yellow)" />
          <StatCard label="Swap Ins"       value={fmt(s.swapIns)}       icon={<HardDrive size={15}/>} color="var(--accent-blue)" />
          <StatCard label="Transl. Penalty" value={fmt(s.translationPenalty)} icon={<Clock size={15} />} color="var(--accent-purple)" sub="cycles" />
        </div>
      </section>

      {/* Cache */}
      <section>
        <SectionHeader icon={<Layers size={13} />} title="Cache Statistics" color="var(--accent-blue)" />
        <div style={{ border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
          <table className="mem-table">
            <thead>
              <tr><th>Level</th><th>Hits</th><th>Misses</th><th>Miss Rate</th></tr>
            </thead>
            <tbody>
              <CacheRow name="L1I" hits={s.l1iHits} misses={s.l1iMisses} />
              <CacheRow name="L1D" hits={s.l1dHits} misses={s.l1dMisses} />
              <CacheRow name="L2"  hits={s.l2Hits}  misses={s.l2Misses} />
            </tbody>
          </table>
        </div>
      </section>

      {/* Raw output */}
      <details>
        <summary style={{ fontSize: 11, color: 'var(--text-muted)', cursor: 'pointer', userSelect: 'none', marginBottom: 8 }}>
          Raw output
        </summary>
        <div className="terminal" style={{ borderRadius: 6, fontSize: 12 }}>{raw}</div>
      </details>
    </div>
  );
}

// ── Drop Zone ─────────────────────────────────────────────────────────────────

function DropZone({ onFile }: { onFile: (f: File) => void }) {
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) onFile(f);
  }, [onFile]);

  return (
    <div
      onDragOver={e => { e.preventDefault(); setDragging(true); }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      style={{
        border: `2px dashed ${dragging ? 'var(--accent-cyan)' : 'var(--border)'}`,
        borderRadius: 12,
        padding: '40px 32px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        background: dragging ? 'rgba(0,245,212,0.04)' : 'var(--bg-elevated)',
        boxShadow: dragging ? 'var(--glow-cyan)' : 'none',
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".trace"
        style={{ display: 'none' }}
        onChange={e => { const f = e.target.files?.[0]; if (f) onFile(f); }}
      />
      <div style={{
        width: 52, height: 52, borderRadius: 12,
        background: 'rgba(0,245,212,0.1)',
        border: '1px solid rgba(0,245,212,0.3)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <Upload size={24} color="var(--accent-cyan)" />
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
          Drop a .trace file here
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
          or click to browse — any <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>.trace</code> file
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function TraceReplayPage() {
  const [runState, setRunState] = useState<RunState>('idle');
  const [resultRaw, setResultRaw] = useState('');
  const [fileName, setFileName] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [elapsed, setElapsed] = useState<number | null>(null);
  const [availableTraces, setAvailableTraces] = useState<string[]>([]);
  const [loadingTraces, setLoadingTraces] = useState(false);

  // Load list of available trace files on mount
  useEffect(() => {
    setLoadingTraces(true);
    listTraces()
      .then(setAvailableTraces)
      .catch(() => setAvailableTraces([]))
      .finally(() => setLoadingTraces(false));
  }, []);

  const handleFile = useCallback(async (file: File) => {
    setRunState('running');
    setFileName(file.name);
    setErrorMsg('');
    setResultRaw('');
    const t0 = Date.now();
    try {
      const result = await runTrace(file);
      setElapsed(Date.now() - t0);
      if (result.ok && result.content) {
        setResultRaw(result.content);
        setRunState('success');
      } else {
        setErrorMsg(result.error ?? 'Unknown error');
        setRunState('error');
      }
    } catch (err) {
      setElapsed(Date.now() - t0);
      setErrorMsg(err instanceof Error ? err.message : String(err));
      setRunState('error');
    }
  }, []);

  // Run one of the pre-existing trace files from phase3_traces/
  const handlePresetTrace = useCallback(async (traceName: string) => {
    // Fetch the trace file from a special endpoint by name
    setRunState('running');
    setFileName(traceName);
    setErrorMsg('');
    setResultRaw('');
    const t0 = Date.now();
    try {
      // Fetch the file bytes via our proxy, then pass as a File object
      const apiBase = import.meta.env.VITE_API_URL
        ? `${import.meta.env.VITE_API_URL}/api`
        : '/api';
      const resp = await fetch(`${apiBase}/trace-file?name=${encodeURIComponent(traceName)}`);
      if (!resp.ok) throw new Error(`Failed to fetch ${traceName}`);
      const blob = await resp.blob();
      const file = new File([blob], traceName);
      const result = await runTrace(file);
      setElapsed(Date.now() - t0);
      if (result.ok && result.content) {
        setResultRaw(result.content);
        setRunState('success');
      } else {
        setErrorMsg(result.error ?? 'Unknown error');
        setRunState('error');
      }
    } catch (err) {
      setElapsed(Date.now() - t0);
      setErrorMsg(err instanceof Error ? err.message : String(err));
      setRunState('error');
    }
  }, []);

  return (
    <div style={{ height: '100vh', overflow: 'auto', background: 'var(--bg-base)' }}>

      {/* Header */}
      <div style={{
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border)',
        padding: '14px 24px',
        position: 'sticky', top: 0, zIndex: 10,
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{
          width: 32, height: 32, borderRadius: 8,
          background: 'rgba(188,140,255,0.15)',
          border: '1px solid rgba(188,140,255,0.4)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Activity size={16} color="var(--accent-purple)" />
        </div>
        <div>
          <h1 style={{ fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
            Trace Replay
          </h1>
          <p style={{ fontSize: 11, color: 'var(--text-muted)', margin: 0 }}>
            Upload a <code style={{ fontFamily: 'var(--font-mono)' }}>.trace</code> file to run cache &amp; virtual memory analysis
          </p>
        </div>

        {/* Status indicator */}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
          {runState === 'running' && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--accent-cyan)', fontSize: 12 }}>
              <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} />
              Simulating {fileName}…
            </div>
          )}
          {runState === 'success' && elapsed != null && (
            <div className="animate-fade-in" style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--accent-green)', fontSize: 12 }}>
              <CheckCircle2 size={14} />
              Done in {(elapsed / 1000).toFixed(1)}s — {fileName}
            </div>
          )}
          {runState === 'error' && (
            <div className="animate-fade-in" style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--accent-orange)', fontSize: 12 }}>
              <XCircle size={14} />
              Error
            </div>
          )}
        </div>
      </div>

      <div style={{ padding: 24, display: 'flex', gap: 24, alignItems: 'flex-start', maxWidth: 1200 }}>

        {/* LEFT: Upload + presets */}
        <div style={{ width: 280, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>

          <DropZone onFile={handleFile} />

          {runState === 'running' && (
            <div style={{ padding: '12px 16px', background: 'rgba(0,245,212,0.05)', border: '1px solid rgba(0,245,212,0.2)', borderRadius: 8 }}>
              <div className="progress-bar" style={{ marginBottom: 8 }} />
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                Running trace simulation — trace files are large, this may take 30–120 seconds…
              </div>
            </div>
          )}

          {runState === 'error' && errorMsg && (
            <div style={{ padding: '12px 16px', background: 'rgba(255,123,114,0.08)', border: '1px solid rgba(255,123,114,0.3)', borderRadius: 8, fontSize: 12, color: 'var(--accent-orange)' }}>
              <XCircle size={12} style={{ display: 'inline', marginRight: 6 }} />
              {errorMsg}
            </div>
          )}

          {/* Available traces */}
          <div className="panel" style={{ padding: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
              <FileText size={13} color="var(--accent-purple)" />
              <span style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Phase 3 Traces
              </span>
              {availableTraces.length > 0 && (
                <span className="badge badge-cyan" style={{ marginLeft: 'auto' }}>
                  {availableTraces.length}
                </span>
              )}
            </div>

            {loadingTraces ? (
              <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Loading…</div>
            ) : availableTraces.length === 0 ? (
              <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.6 }}>
                No trace files found in <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>phase3_traces/</code>.
                Upload a file manually above.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <div style={{ fontSize: 10, color: 'var(--text-muted)', marginBottom: 4 }}>
                  ⚠️ Running a preset trace requires fetching ~12MB — use file upload for faster results.
                </div>
                {availableTraces.map(name => (
                  <button
                    key={name}
                    onClick={() => handlePresetTrace(name)}
                    disabled={runState === 'running'}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 8,
                      padding: '7px 10px', borderRadius: 6,
                      border: '1px solid var(--border)',
                      background: fileName === name && runState === 'success' ? 'rgba(0,245,212,0.06)' : 'transparent',
                      cursor: runState === 'running' ? 'not-allowed' : 'pointer',
                      opacity: runState === 'running' ? 0.5 : 1,
                      textAlign: 'left', width: '100%',
                      transition: 'all 0.15s ease',
                    }}
                    onMouseEnter={e => { if (runState !== 'running') (e.currentTarget as HTMLElement).style.background = 'var(--bg-elevated)'; }}
                    onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = fileName === name && runState === 'success' ? 'rgba(0,245,212,0.06)' : 'transparent'; }}
                  >
                    {fileName === name && runState === 'running' ? (
                      <Loader2 size={12} color="var(--accent-cyan)" style={{ animation: 'spin 1s linear infinite', flexShrink: 0 }} />
                    ) : fileName === name && runState === 'success' ? (
                      <CheckCircle2 size={12} color="var(--accent-green)" style={{ flexShrink: 0 }} />
                    ) : (
                      <Play size={12} color="var(--accent-purple)" fill="var(--accent-purple)" style={{ flexShrink: 0 }} />
                    )}
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-primary)' }}>{name}</span>
                    <span style={{ fontSize: 10, color: 'var(--text-muted)', marginLeft: 'auto' }}>~12MB</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT: Results */}
        <div style={{ flex: 1 }}>
          {runState === 'idle' && (
            <div style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              height: 400, gap: 14, color: 'var(--text-muted)',
            }}>
              <div style={{
                width: 64, height: 64, borderRadius: 16,
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Activity size={28} strokeWidth={1} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 4 }}>
                  No trace loaded yet
                </div>
                <div style={{ fontSize: 12 }}>
                  Drop a <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>.trace</code> file or choose one from the preset list
                </div>
              </div>
            </div>
          )}

          {runState === 'running' && (
            <div style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              height: 400, gap: 16,
            }}>
              <div style={{
                width: 64, height: 64, borderRadius: '50%',
                border: '3px solid var(--bg-elevated)',
                borderTop: '3px solid var(--accent-cyan)',
                animation: 'spin 1s linear infinite',
              }} />
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                  Simulating {fileName}
                </div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  Running TLB, page table, and cache simulation…
                </div>
              </div>
            </div>
          )}

          {runState === 'success' && resultRaw && (
            <div className="animate-fade-in">
              <TraceStatsDisplay raw={resultRaw} fileName={fileName} />
            </div>
          )}
        </div>
      </div>

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
