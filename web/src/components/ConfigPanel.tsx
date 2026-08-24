import { useState } from 'react';
import { Settings, Layers, Cpu, Clock, RotateCcw, CheckCircle2, Info } from 'lucide-react';

export interface SimConfig {
  forwardingEnabled: boolean;
  l1dSizeKb: number;        // KB
  l1dBlockBytes: number;    // bytes
  l1dAssoc: number;         // ways
  l1dLatency: number;       // cycles
  l1iEnabled: boolean;
  l2Enabled: boolean;
  l2SizeKb: number;
  l2Assoc: number;
  memoryLatency: number;    // cycles
  mulLatency: number;
  divLatency: number;
}

export const DEFAULT_CONFIG: SimConfig = {
  forwardingEnabled: true,
  l1dSizeKb:     4,
  l1dBlockBytes: 64,
  l1dAssoc:      1,
  l1dLatency:    1,
  l1iEnabled:    false,
  l2Enabled:     false,
  l2SizeKb:      16,
  l2Assoc:       4,
  memoryLatency: 10,
  mulLatency:    3,
  divLatency:    4,
};

interface ConfigPanelProps {
  config: SimConfig;
  onChange: (cfg: SimConfig) => void;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function Section({ icon, title, children }: { icon: React.ReactNode; title: string; children: React.ReactNode }) {
  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text-secondary)' }}>
        {icon}
        <span style={{ fontWeight: 600, fontSize: 12, letterSpacing: '0.06em', textTransform: 'uppercase' }}>{title}</span>
      </div>
      <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
        {children}
      </div>
    </section>
  );
}

function Row({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  const [showHint, setShowHint] = useState(false);
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 14px', borderBottom: '1px solid var(--border-subtle)',
      gap: 12,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, minWidth: 0 }}>
        <span style={{ fontSize: 13, color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>{label}</span>
        {hint && (
          <div style={{ position: 'relative', display: 'inline-flex' }}>
            <Info
              size={12}
              color="var(--text-muted)"
              style={{ cursor: 'pointer', flexShrink: 0 }}
              onMouseEnter={() => setShowHint(true)}
              onMouseLeave={() => setShowHint(false)}
            />
            {showHint && (
              <div style={{
                position: 'absolute', bottom: '100%', left: '50%', transform: 'translateX(-50%)',
                background: 'var(--bg-panel)', border: '1px solid var(--border)',
                borderRadius: 6, padding: '6px 10px', fontSize: 11, color: 'var(--text-primary)',
                whiteSpace: 'nowrap', zIndex: 10, boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
                marginBottom: 4,
              }}>
                {hint}
              </div>
            )}
          </div>
        )}
      </div>
      <div style={{ flexShrink: 0 }}>{children}</div>
    </div>
  );
}

function Toggle({ value, onChange }: { value: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      onClick={() => onChange(!value)}
      style={{
        width: 44, height: 24, borderRadius: 12, border: 'none', cursor: 'pointer',
        background: value ? 'var(--accent-cyan)' : 'var(--bg-panel)',
        position: 'relative', transition: 'background 0.2s ease',
        boxShadow: value ? '0 0 8px rgba(0,245,212,0.4)' : 'none',
      }}
      title={value ? 'Disable' : 'Enable'}
    >
      <span style={{
        position: 'absolute', top: 3, left: value ? 23 : 3,
        width: 18, height: 18, borderRadius: '50%',
        background: value ? '#0d1117' : 'var(--text-muted)',
        transition: 'left 0.2s ease',
      }} />
    </button>
  );
}

function NumberInput({ value, onChange, min, max, step = 1, unit }: {
  value: number; onChange: (v: number) => void;
  min: number; max: number; step?: number; unit?: string;
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <input
        type="number"
        value={value}
        min={min}
        max={max}
        step={step}
        onChange={e => {
          const v = Number(e.target.value);
          if (!isNaN(v) && v >= min && v <= max) onChange(v);
        }}
        style={{
          width: 72, padding: '4px 8px', textAlign: 'right',
          fontFamily: 'var(--font-mono)', fontSize: 13,
          background: 'var(--bg-panel)', border: '1px solid var(--border)',
          borderRadius: 4, color: 'var(--accent-cyan)',
          outline: 'none', transition: 'border-color 0.2s',
        }}
        onFocus={e => (e.target.style.borderColor = 'var(--accent-cyan)')}
        onBlur={e => (e.target.style.borderColor = 'var(--border)')}
      />
      {unit && <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{unit}</span>}
    </div>
  );
}

function SelectInput({ value, onChange, options }: {
  value: number; onChange: (v: number) => void;
  options: { label: string; value: number }[];
}) {
  return (
    <select
      value={value}
      onChange={e => onChange(Number(e.target.value))}
      style={{
        padding: '4px 8px', borderRadius: 4, border: '1px solid var(--border)',
        background: 'var(--bg-panel)', color: 'var(--accent-cyan)',
        fontFamily: 'var(--font-mono)', fontSize: 13, cursor: 'pointer', outline: 'none',
      }}
    >
      {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────

export default function ConfigPanel({ config, onChange }: ConfigPanelProps) {
  const set = <K extends keyof SimConfig>(key: K, val: SimConfig[K]) =>
    onChange({ ...config, [key]: val });

  const isDefault = JSON.stringify(config) === JSON.stringify(DEFAULT_CONFIG);

  return (
    <div style={{ height: '100%', overflow: 'auto', padding: 16, display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Settings size={16} color="var(--accent-cyan)" />
          <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)' }}>Simulation Configuration</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {isDefault ? (
            <span style={{ fontSize: 11, color: 'var(--accent-green)', display: 'flex', alignItems: 'center', gap: 4 }}>
              <CheckCircle2 size={11} /> Defaults
            </span>
          ) : (
            <span className="badge badge-yellow">Modified</span>
          )}
          <button
            className="btn btn-ghost"
            style={{ fontSize: 11, padding: '4px 10px', gap: 5 }}
            onClick={() => onChange({ ...DEFAULT_CONFIG })}
            title="Reset to defaults"
          >
            <RotateCcw size={11} />
            Reset
          </button>
        </div>
      </div>

      {/* Note */}
      <div style={{
        background: 'rgba(88,166,255,0.06)', border: '1px solid rgba(88,166,255,0.2)',
        borderRadius: 8, padding: '10px 14px', fontSize: 12, color: 'var(--accent-blue)',
        display: 'flex', gap: 8, alignItems: 'flex-start',
      }}>
        <Info size={14} style={{ flexShrink: 0, marginTop: 1 }} />
        <span>
          Changes take effect on the <strong>next Run</strong>. The config is sent to the Java backend with each simulation request.
        </span>
      </div>

      {/* ── Pipeline ── */}
      <Section icon={<Cpu size={14} />} title="Pipeline">
        <Row label="Data Forwarding" hint="Enables EX/MEM→EX bypass paths. Disabling forces stalls on every RAW hazard.">
          <Toggle value={config.forwardingEnabled} onChange={v => set('forwardingEnabled', v)} />
        </Row>
        <Row label="MUL Latency" hint="Extra cycles the MUL instruction occupies the EX stage.">
          <NumberInput value={config.mulLatency} onChange={v => set('mulLatency', v)} min={1} max={20} unit="cycles" />
        </Row>
        <Row label="DIV Latency" hint="Extra cycles the DIV instruction occupies the EX stage.">
          <NumberInput value={config.divLatency} onChange={v => set('divLatency', v)} min={1} max={40} unit="cycles" />
        </Row>
      </Section>

      {/* ── L1D Cache ── */}
      <Section icon={<Layers size={14} />} title="L1D Cache (Data)">
        <Row label="Cache Size" hint="Total capacity of the L1 data cache.">
          <SelectInput
            value={config.l1dSizeKb}
            onChange={v => set('l1dSizeKb', v)}
            options={[1, 2, 4, 8, 16, 32].map(kb => ({ value: kb, label: `${kb} KB` }))}
          />
        </Row>
        <Row label="Block Size" hint="Size of each cache line. Larger blocks improve spatial locality.">
          <SelectInput
            value={config.l1dBlockBytes}
            onChange={v => set('l1dBlockBytes', v)}
            options={[16, 32, 64, 128].map(b => ({ value: b, label: `${b} B` }))}
          />
        </Row>
        <Row label="Associativity" hint="Number of ways per set. 1 = direct-mapped. Higher reduces conflict misses.">
          <SelectInput
            value={config.l1dAssoc}
            onChange={v => set('l1dAssoc', v)}
            options={[1, 2, 4, 8].map(w => ({ value: w, label: w === 1 ? '1-way (Direct)' : `${w}-way` }))}
          />
        </Row>
        <Row label="Hit Latency" hint="Cycles to serve a cache hit.">
          <NumberInput value={config.l1dLatency} onChange={v => set('l1dLatency', v)} min={1} max={10} unit="cycles" />
        </Row>
      </Section>

      {/* ── L1I Cache ── */}
      <Section icon={<Layers size={14} />} title="L1I Cache (Instruction)">
        <Row label="Enable L1I" hint="When disabled, instruction fetches go directly to main memory (adds memory latency per fetch).">
          <Toggle value={config.l1iEnabled} onChange={v => set('l1iEnabled', v)} />
        </Row>
        {config.l1iEnabled && (
          <Row label="Block Size" hint="Instruction cache line size (always direct-mapped, 4KB).">
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-muted)' }}>4 KB / 64 B blocks / 1-way</span>
          </Row>
        )}
      </Section>

      {/* ── L2 Cache ── */}
      <Section icon={<Layers size={14} />} title="L2 Cache (Unified)">
        <Row label="Enable L2" hint="Unified L2 sits between L1 and main memory. Reduces main memory accesses.">
          <Toggle value={config.l2Enabled} onChange={v => set('l2Enabled', v)} />
        </Row>
        {config.l2Enabled && (
          <>
            <Row label="Cache Size" hint="Total capacity of the L2 cache.">
              <SelectInput
                value={config.l2SizeKb}
                onChange={v => set('l2SizeKb', v)}
                options={[8, 16, 32, 64, 128, 256].map(kb => ({ value: kb, label: `${kb} KB` }))}
              />
            </Row>
            <Row label="Associativity" hint="Number of ways per set in L2.">
              <SelectInput
                value={config.l2Assoc}
                onChange={v => set('l2Assoc', v)}
                options={[1, 2, 4, 8, 16].map(w => ({ value: w, label: w === 1 ? '1-way (Direct)' : `${w}-way` }))}
              />
            </Row>
          </>
        )}
      </Section>

      {/* ── Memory ── */}
      <Section icon={<Clock size={14} />} title="Main Memory">
        <Row label="Access Latency" hint="Penalty cycles on an L1 cache miss (or every fetch if caches are disabled).">
          <NumberInput value={config.memoryLatency} onChange={v => set('memoryLatency', v)} min={1} max={200} unit="cycles" />
        </Row>
        <Row label="Physical RAM" hint="Fixed at 128 KB (pipeline mode) or 256 KB (trace mode). Not configurable.">
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-muted)' }}>128 KB (pipeline)</span>
        </Row>
      </Section>

      {/* ── Summary ── */}
      <div style={{
        background: 'var(--bg-elevated)', border: '1px solid var(--border)',
        borderRadius: 8, padding: 14, fontFamily: 'var(--font-mono)', fontSize: 11,
        color: 'var(--text-muted)', lineHeight: 1.8,
      }}>
        <span style={{ color: 'var(--accent-cyan)', fontWeight: 600 }}>Effective config: </span>
        Forwarding={config.forwardingEnabled ? 'ON' : 'OFF'} ·{' '}
        L1D={config.l1dSizeKb}KB/{config.l1dBlockBytes}B/{config.l1dAssoc}-way ·{' '}
        L1I={config.l1iEnabled ? 'ON' : 'OFF'} ·{' '}
        L2={config.l2Enabled ? `${config.l2SizeKb}KB/${config.l2Assoc}-way` : 'OFF'} ·{' '}
        MEM={config.memoryLatency}cy ·{' '}
        MUL={config.mulLatency}cy · DIV={config.divLatency}cy
      </div>

    </div>
  );
}
