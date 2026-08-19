import { useMemo } from 'react';
import { Activity, Cpu, Layers, Zap, TrendingUp, TrendingDown } from 'lucide-react';

interface StatsPanelProps {
  content: string;
}

interface ParsedStats {
  cycles: number | null;
  stalls: number | null;
  branchFlushes: number | null;
  instructionsRetired: number | null;
  ipc: number | null;
  l1dSize: string;
  memLatency: string;
  forwarding: string;
  l1iHits: number | null;
  l1iMisses: number | null;
  l1dHits: number | null;
  l1dMisses: number | null;
  l2Hits: number | null;
  l2Misses: number | null;
}

function parseNum(line: string, key: string): number | null {
  const r = new RegExp(key + '\\s*:\\s*([\\d.]+)');
  const m = line.match(r);
  return m ? parseFloat(m[1]) : null;
}

function parseStats(raw: string): ParsedStats {
  const lines = raw.split(/\r?\n/);
  const get = (key: string): number | null => {
    for (const l of lines) {
      const v = parseNum(l, key);
      if (v !== null) return v;
    }
    return null;
  };

  const getString = (key: string): string => {
    for (const l of lines) {
      const r = new RegExp(key + '\\s*:\\s*(.+)$');
      const m = l.match(r);
      if (m) return m[1].trim();
    }
    return '—';
  };

  // parse cache stats from lines like: "L1I  : 0 hits, 0 misses, miss rate 0.000"
  const parseCacheLine = (prefix: string): { hits: number | null; misses: number | null } => {
    for (const l of lines) {
      if (l.startsWith(prefix)) {
        const m = l.match(/(\d+)\s+hits,\s*(\d+)\s+misses/);
        if (m) return { hits: parseInt(m[1]), misses: parseInt(m[2]) };
      }
    }
    return { hits: null, misses: null };
  };

  const l1i = parseCacheLine('L1I');
  const l1d = parseCacheLine('L1D');
  const l2  = parseCacheLine('L2 ');

  return {
    cycles:               get('Cycles'),
    stalls:               get('Stalls'),
    branchFlushes:        get('Branch Flushes'),
    instructionsRetired:  get('Instructions Retired'),
    ipc:                  get('IPC'),
    l1dSize:              getString('L1D'),
    memLatency:           getString('Memory Latency'),
    forwarding:           getString('Forwarding'),
    l1iHits:  l1i.hits,  l1iMisses:  l1i.misses,
    l1dHits:  l1d.hits,  l1dMisses:  l1d.misses,
    l2Hits:   l2.hits,   l2Misses:   l2.misses,
  };
}

function fmt(n: number | null, dec = 0): string {
  if (n === null) return '—';
  return dec > 0 ? n.toFixed(dec) : n.toLocaleString();
}

function missRate(hits: number | null, misses: number | null): string {
  if (hits === null || misses === null) return '—';
  const total = hits + misses;
  if (total === 0) return '0.000';
  return (misses / total).toFixed(3);
}

interface StatCardProps {
  label: string;
  value: string;
  sub?: string;
  icon: React.ReactNode;
  accent?: string;
  trend?: 'up' | 'down' | null;
}

function StatCard({ label, value, sub, icon, accent = 'var(--accent-cyan)', trend }: StatCardProps) {
  return (
    <div className="stat-card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
          {label}
        </div>
        <div style={{ color: accent, opacity: 0.8 }}>{icon}</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
        <div style={{ fontSize: 28, fontWeight: 700, color: accent, fontFamily: 'var(--font-mono)', lineHeight: 1 }}>
          {value}
        </div>
        {trend && (
          trend === 'up'
            ? <TrendingUp size={14} color="var(--accent-green)" />
            : <TrendingDown size={14} color="var(--accent-orange)" />
        )}
      </div>
      {sub && <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{sub}</div>}
    </div>
  );
}

export default function StatsPanel({ content }: StatsPanelProps) {
  const stats = useMemo(() => parseStats(content), [content]);

  if (!content.trim()) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-muted)' }}>
        <Activity size={32} strokeWidth={1} />
        <p style={{ fontSize: 13 }}>Run the simulation to see performance stats</p>
      </div>
    );
  }

  const stallPct = (stats.cycles && stats.stalls)
    ? ((stats.stalls / stats.cycles) * 100).toFixed(1) + '% of cycles'
    : undefined;

  return (
    <div style={{ padding: 16, overflow: 'auto', height: '100%', display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* Section: Execution */}
      <section>
        <SectionHeader icon={<Cpu size={14} />} title="Execution Statistics" />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
          <StatCard label="Total Cycles"     value={fmt(stats.cycles)}              icon={<Activity size={16} />} accent="var(--accent-cyan)" />
          <StatCard label="Stalls"            value={fmt(stats.stalls)}              icon={<Activity size={16} />} sub={stallPct} accent="var(--accent-orange)" />
          <StatCard label="Branch Flushes"   value={fmt(stats.branchFlushes)}       icon={<Activity size={16} />} accent="var(--accent-purple)" />
          <StatCard label="Instr. Retired"   value={fmt(stats.instructionsRetired)} icon={<Zap size={16} />}      accent="var(--accent-green)" />
          <StatCard label="IPC"              value={fmt(stats.ipc, 3)}              icon={<TrendingUp size={16}/>} sub="Instructions per cycle" accent="var(--accent-blue)" />
        </div>
      </section>

      {/* Section: Cache Config */}
      <section>
        <SectionHeader icon={<Layers size={14} />} title="Cache Configuration" />
        <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)', borderRadius: 8, padding: 14, fontFamily: 'var(--font-mono)', fontSize: 12 }}>
          <ConfigRow label="L1D Cache"     value={stats.l1dSize} />
          <ConfigRow label="Memory Latency" value={stats.memLatency} />
          <ConfigRow label="Forwarding"    value={stats.forwarding}
            valueColor={stats.forwarding === 'enabled' ? 'var(--accent-green)' : 'var(--accent-orange)'} />
        </div>
      </section>

      {/* Section: Cache Stats */}
      <section>
        <SectionHeader icon={<Layers size={14} />} title="Cache Statistics" />
        <div style={{ border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
          <table className="mem-table">
            <thead>
              <tr>
                <th>Cache</th>
                <th>Hits</th>
                <th>Misses</th>
                <th>Miss Rate</th>
              </tr>
            </thead>
            <tbody>
              <CacheRow name="L1I" hits={stats.l1iHits} misses={stats.l1iMisses} />
              <CacheRow name="L1D" hits={stats.l1dHits} misses={stats.l1dMisses} />
              <CacheRow name="L2"  hits={stats.l2Hits}  misses={stats.l2Misses} />
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function SectionHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10, color: 'var(--text-secondary)' }}>
      {icon}
      <span style={{ fontWeight: 600, fontSize: 12, letterSpacing: '0.05em', textTransform: 'uppercase' }}>{title}</span>
    </div>
  );
}

function ConfigRow({ label, value, valueColor }: { label: string; value: string; valueColor?: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '5px 0', borderBottom: '1px solid var(--border-subtle)' }}>
      <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
      <span style={{ color: valueColor ?? 'var(--text-primary)' }}>{value}</span>
    </div>
  );
}

function CacheRow({ name, hits, misses }: { name: string; hits: number | null; misses: number | null }) {
  const mr = missRate(hits, misses);
  const mrNum = parseFloat(mr);
  const mrColor = isNaN(mrNum) ? 'var(--text-muted)' :
    mrNum === 0 ? 'var(--accent-green)' :
    mrNum < 0.05 ? 'var(--accent-yellow)' : 'var(--accent-orange)';

  return (
    <tr>
      <td style={{ fontWeight: 600 }}>{name}</td>
      <td className="val">{hits ?? '—'}</td>
      <td style={{ color: 'var(--accent-orange)' }}>{misses ?? '—'}</td>
      <td style={{ color: mrColor }}>{mr}</td>
    </tr>
  );
}
