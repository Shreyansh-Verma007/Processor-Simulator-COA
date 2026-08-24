import { useMemo, useState } from 'react';
import { Database, Hash, Search, Eye } from 'lucide-react';

interface MemoryPanelProps {
  content: string; // raw console.txt content
}

interface MemRow { addr: string; val: string; addrNum: number }

type ViewMode = 'dec' | 'hex' | 'bin';

function parseMemory(raw: string): MemRow[] {
  const rows: MemRow[] = [];
  let inMemDump = false;
  for (const line of raw.split(/\r?\n/)) {
    if (line.startsWith('=== Memory Dump')) { inMemDump = true; continue; }
    if (inMemDump) {
      const m = line.match(/^(0x[0-9A-Fa-f]+):\s*(.+)$/);
      if (m) {
        rows.push({ addr: m[1], val: m[2].trim(), addrNum: parseInt(m[1], 16) });
      }
    }
  }
  return rows;
}

function formatVal(rawVal: string, mode: ViewMode): string {
  const n = parseInt(rawVal, 10);
  if (isNaN(n)) return rawVal;
  if (mode === 'hex') return '0x' + n.toString(16).toUpperCase().padStart(8, '0');
  if (mode === 'bin') return n.toString(2).padStart(32, '0');
  return n.toLocaleString();
}

function valColor(rawVal: string): string {
  const n = parseInt(rawVal, 10);
  if (isNaN(n)) return 'var(--text-muted)';
  if (n === 0) return 'var(--text-muted)';
  if (n < 0)   return 'var(--accent-orange)';
  return 'var(--accent-green)';
}

export default function MemoryPanel({ content }: MemoryPanelProps) {
  const allRows = useMemo(() => parseMemory(content), [content]);
  const [search, setSearch] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('dec');
  const [hideZero, setHideZero] = useState(false);

  if (!content.trim()) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-muted)' }}>
        <Database size={32} strokeWidth={1} />
        <p style={{ fontSize: 13 }}>Run the simulation to see the memory dump</p>
        <p style={{ fontSize: 11, color: 'var(--text-muted)', maxWidth: 300, textAlign: 'center' }}>
          The memory dump appears only when your program has a <code style={{ fontFamily: 'var(--font-mono)', background: 'rgba(255,255,255,0.05)', padding: '0 4px', borderRadius: 3 }}>.data</code> section
        </p>
      </div>
    );
  }

  if (allRows.length === 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-muted)' }}>
        <Database size={32} strokeWidth={1} />
        <p style={{ fontSize: 13 }}>No memory dump in this simulation</p>
        <p style={{ fontSize: 11, color: 'var(--text-muted)', maxWidth: 300, textAlign: 'center' }}>
          Add a <code style={{ fontFamily: 'var(--font-mono)', background: 'rgba(255,255,255,0.05)', padding: '0 4px', borderRadius: 3 }}>.data</code> section to your assembly to see a memory dump here
        </p>
      </div>
    );
  }

  const filtered = allRows.filter(r => {
    if (hideZero && parseInt(r.val, 10) === 0) return false;
    if (search) {
      const s = search.toLowerCase();
      return r.addr.toLowerCase().includes(s) || r.val.includes(s);
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>

      {/* Toolbar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '10px 16px',
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border)',
        flexShrink: 0,
        flexWrap: 'wrap',
      }}>
        {/* Title + badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Database size={14} color="var(--accent-cyan)" />
          <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Memory Dump</span>
          <span className="badge badge-cyan">{allRows.length} words</span>
          {filtered.length !== allRows.length && (
            <span className="badge badge-yellow">{filtered.length} shown</span>
          )}
        </div>

        <div style={{ flex: 1 }} />

        {/* Hide-zero toggle */}
        <button
          onClick={() => setHideZero(h => !h)}
          style={{
            display: 'flex', alignItems: 'center', gap: 5,
            padding: '4px 10px', borderRadius: 4, fontSize: 11, cursor: 'pointer',
            border: `1px solid ${hideZero ? 'var(--accent-purple)' : 'var(--border)'}`,
            background: hideZero ? 'rgba(188,140,255,0.1)' : 'transparent',
            color: hideZero ? 'var(--accent-purple)' : 'var(--text-muted)',
            transition: 'all 0.2s ease',
          }}
        >
          <Eye size={11} />
          Hide zeros
        </button>

        {/* View mode toggle */}
        <div style={{ display: 'flex', border: '1px solid var(--border)', borderRadius: 4, overflow: 'hidden' }}>
          {(['dec', 'hex', 'bin'] as ViewMode[]).map(m => (
            <button
              key={m}
              onClick={() => setViewMode(m)}
              style={{
                padding: '4px 10px', fontSize: 11, cursor: 'pointer', border: 'none',
                background: viewMode === m ? 'var(--accent-cyan)' : 'transparent',
                color: viewMode === m ? '#0d1117' : 'var(--text-muted)',
                fontFamily: 'var(--font-mono)',
                fontWeight: viewMode === m ? 700 : 400,
                transition: 'all 0.15s ease',
              }}
            >
              {m.toUpperCase()}
            </button>
          ))}
        </div>

        {/* Search */}
        <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
          <Search size={11} style={{ position: 'absolute', left: 8, color: 'var(--text-muted)', pointerEvents: 'none' }} />
          <input
            placeholder="Filter address or value…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{
              paddingLeft: 26, paddingRight: 10, paddingTop: 4, paddingBottom: 4,
              borderRadius: 4, border: '1px solid var(--border)',
              background: 'var(--bg-elevated)', color: 'var(--text-primary)',
              fontFamily: 'var(--font-mono)', fontSize: 11, width: 180,
              outline: 'none',
              transition: 'border-color 0.2s',
            }}
            onFocus={e => (e.target.style.borderColor = 'var(--accent-cyan)')}
            onBlur={e => (e.target.style.borderColor = 'var(--border)')}
          />
        </div>
      </div>

      {/* Table */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {filtered.length === 0 ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)', fontSize: 13 }}>
            No rows match your filter
          </div>
        ) : (
          <table className="mem-table" style={{ position: 'relative' }}>
            <thead style={{ position: 'sticky', top: 0, zIndex: 1 }}>
              <tr>
                <th style={{ width: 60 }}>#</th>
                <th style={{ width: 140 }}><Hash size={10} style={{ display: 'inline', marginRight: 4 }} />Address</th>
                <th style={{ width: 100 }}>Addr (dec)</th>
                <th>Value ({viewMode.toUpperCase()})</th>
                {viewMode !== 'hex' && <th>Hex</th>}
                {viewMode !== 'bin' && <th style={{ width: 80 }}>ASCII</th>}
              </tr>
            </thead>
            <tbody>
              {filtered.map((row, i) => {
                const numVal = parseInt(row.val, 10);
                const hexVal = isNaN(numVal) ? '—' : '0x' + numVal.toString(16).toUpperCase().padStart(8, '0');
                // Try to extract printable ASCII from the 4 bytes of the word
                const ascii = isNaN(numVal) ? '—' : Array.from({ length: 4 }, (_, b) => {
                  const byte = (numVal >>> (b * 8)) & 0xff;
                  return byte >= 32 && byte < 127 ? String.fromCharCode(byte) : '.';
                }).join('');

                return (
                  <tr key={i} style={{ animation: 'fade-in 0.15s ease-out both', animationDelay: `${Math.min(i * 0.01, 0.3)}s` }}>
                    <td style={{ color: 'var(--text-muted)', fontSize: 11 }}>{i + 1}</td>
                    <td className="addr">{row.addr}</td>
                    <td style={{ color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>{row.addrNum}</td>
                    <td style={{ color: valColor(row.val), fontFamily: 'var(--font-mono)', wordBreak: 'break-all' }}>
                      {formatVal(row.val, viewMode)}
                    </td>
                    {viewMode !== 'hex' && (
                      <td style={{ color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>{hexVal}</td>
                    )}
                    {viewMode !== 'bin' && (
                      <td style={{ color: 'var(--accent-yellow)', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                        {ascii}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
