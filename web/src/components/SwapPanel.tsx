import { useMemo } from 'react';
import { HardDrive, ArrowUpDown, ArrowDownToLine, ArrowUpFromLine } from 'lucide-react';

interface SwapPanelProps {
  content: string;
}

interface VpnEntry { vpn: string; words: number }

interface SwapData {
  swapOuts: number | null;
  swapIns: number | null;
  resident: number | null;
  vpnEntries: VpnEntry[];
}

function parseSwap(raw: string): SwapData {
  const lines = raw.split(/\r?\n/);
  let swapOuts: number | null = null;
  let swapIns: number | null = null;
  let resident: number | null = null;
  const vpnEntries: VpnEntry[] = [];

  for (const line of lines) {
    const triml = line.trim();
    if (triml.match(/Swap Outs/i)) {
      const m = triml.match(/:\s*(\d+)/);
      if (m) swapOuts = parseInt(m[1]);
    } else if (triml.match(/Swap Ins/i)) {
      const m = triml.match(/:\s*(\d+)/);
      if (m) swapIns = parseInt(m[1]);
    } else if (triml.match(/Resident in swap/i)) {
      const m = triml.match(/:\s*(\d+)/);
      if (m) resident = parseInt(m[1]);
    } else if (triml.match(/^VPN\s+\d+/i)) {
      const m = triml.match(/VPN\s+(\d+):\s*(\d+)\s+words/i);
      if (m) vpnEntries.push({ vpn: m[1], words: parseInt(m[2]) });
    }
  }

  return { swapOuts, swapIns, resident, vpnEntries };
}

function fmt(n: number | null): string {
  return n === null ? '—' : n.toLocaleString();
}

export default function SwapPanel({ content }: SwapPanelProps) {
  const { swapOuts, swapIns, resident, vpnEntries } = useMemo(() => parseSwap(content), [content]);

  if (!content.trim()) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-muted)' }}>
        <HardDrive size={32} strokeWidth={1} />
        <p style={{ fontSize: 13 }}>Run the simulation to see swap space data</p>
      </div>
    );
  }

  return (
    <div style={{ padding: 16, overflow: 'auto', height: '100%', display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* Summary cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
        <SummaryCard
          label="Swap Outs (writes)"
          value={fmt(swapOuts)}
          icon={<ArrowUpFromLine size={18} />}
          accent="var(--accent-orange)"
          sub="Pages evicted to disk"
        />
        <SummaryCard
          label="Swap Ins (reads)"
          value={fmt(swapIns)}
          icon={<ArrowDownToLine size={18} />}
          accent="var(--accent-blue)"
          sub="Pages loaded from disk"
        />
        <SummaryCard
          label="Resident in Swap"
          value={fmt(resident)}
          icon={<HardDrive size={18} />}
          accent="var(--accent-purple)"
          sub="Pages currently on disk"
        />
      </div>

      {/* Swap Balance */}
      {swapOuts !== null && swapIns !== null && (
        <div style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border)', borderRadius: 8, padding: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
            <ArrowUpDown size={14} color="var(--accent-cyan)" />
            <span style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Swap Balance
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>Swap Out Pressure</div>
              <div style={{ height: 8, background: 'var(--bg-panel)', borderRadius: 4, overflow: 'hidden' }}>
                <div style={{
                  height: '100%',
                  width: swapOuts + swapIns === 0 ? '0%' : `${(swapOuts / (swapOuts + swapIns)) * 100}%`,
                  background: 'linear-gradient(90deg, var(--accent-orange), var(--accent-purple))',
                  borderRadius: 4,
                  transition: 'width 0.5s ease',
                }} />
              </div>
            </div>
            <span style={{ fontSize: 12, fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)', minWidth: 60, textAlign: 'right' }}>
              {swapOuts + swapIns === 0 ? '0' : ((swapOuts / (swapOuts + swapIns)) * 100).toFixed(1)}% out
            </span>
          </div>
        </div>
      )}

      {/* VPN Table */}
      {vpnEntries.length > 0 && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
            <HardDrive size={14} color="var(--accent-cyan)" />
            <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Pages in Swap Space</span>
            <span className="badge badge-purple" style={{ background: 'rgba(188,140,255,0.15)', color: 'var(--accent-purple)' }}>
              {vpnEntries.length} pages
            </span>
          </div>
          <div style={{ border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
            <table className="mem-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Virtual Page Number (VPN)</th>
                  <th>Size (words)</th>
                  <th>Size (bytes)</th>
                </tr>
              </thead>
              <tbody>
                {vpnEntries.map((e, i) => (
                  <tr key={i}>
                    <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>{i + 1}</td>
                    <td className="addr">{e.vpn}</td>
                    <td className="val">{e.words.toLocaleString()}</td>
                    <td style={{ color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
                      {(e.words * 4).toLocaleString()} B
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Raw view */}
      <details style={{ cursor: 'pointer' }}>
        <summary style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 8, userSelect: 'none' }}>
          Raw swap.txt
        </summary>
        <div className="terminal" style={{ borderRadius: 6, fontSize: 12 }}>
          {content}
        </div>
      </details>
    </div>
  );
}

function SummaryCard({ label, value, icon, accent, sub }: {
  label: string; value: string; icon: React.ReactNode; accent: string; sub?: string;
}) {
  return (
    <div className="stat-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {label}
        </span>
        <span style={{ color: accent, opacity: 0.8 }}>{icon}</span>
      </div>
      <div style={{ fontSize: 28, fontWeight: 700, color: accent, fontFamily: 'var(--font-mono)', lineHeight: 1.2, marginTop: 4 }}>
        {value}
      </div>
      {sub && <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>{sub}</div>}
    </div>
  );
}
