import { useMemo } from 'react';
import { Database, Hash } from 'lucide-react';

interface ConsolePanelProps {
  content: string;
}

interface MemRow { addr: string; val: string }

function parseConsole(raw: string) {
  const lines = raw.split(/\r?\n/);
  const headerLines: string[] = [];
  const memRows: MemRow[] = [];
  let inMemDump = false;

  for (const line of lines) {
    if (line.startsWith('=== Memory Dump')) {
      inMemDump = true;
      continue;
    }
    if (inMemDump) {
      // e.g. "0x0400: 1"
      const m = line.match(/^(0x[0-9A-Fa-f]+):\s*(.+)$/);
      if (m) {
        memRows.push({ addr: m[1], val: m[2].trim() });
      }
    } else {
      if (line.trim()) headerLines.push(line);
    }
  }

  return { headerLines, memRows };
}

export default function ConsolePanel({ content }: ConsolePanelProps) {
  const { headerLines, memRows } = useMemo(() => parseConsole(content), [content]);

  if (!content.trim()) {
    return (
      <EmptyState message="Run the simulation to see console output" />
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, padding: 16, overflow: 'auto', height: '100%' }}>
      {/* Header section */}
      {headerLines.length > 0 && (
        <div>
          <div className="terminal-header">
            <div className="terminal-dot" style={{ background: '#ff5f57' }} />
            <div className="terminal-dot" style={{ background: '#febc2e' }} />
            <div className="terminal-dot" style={{ background: '#28c840' }} />
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-muted)', marginLeft: 4 }}>console.txt</span>
          </div>
          <div className="terminal" style={{ borderRadius: '0 0 6px 6px', borderTop: 'none' }}>
            {headerLines.map((l, i) => (
              <div key={i}>
                {l.startsWith('===') ? (
                  <span style={{ color: 'var(--accent-cyan)' }}>{l}</span>
                ) : l.startsWith('Loading:') ? (
                  <span>
                    <span style={{ color: 'var(--text-secondary)' }}>Loading: </span>
                    <span style={{ color: 'var(--accent-yellow)' }}>{l.replace('Loading: ', '')}</span>
                  </span>
                ) : l.startsWith('Compiled') ? (
                  <span style={{ color: 'var(--accent-green)' }}>{l}</span>
                ) : (
                  <span style={{ color: '#c9d1d9' }}>{l}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Memory Dump Table */}
      {memRows.length > 0 && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
            <Database size={14} color="var(--accent-cyan)" />
            <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Memory Dump</span>
            <span className="badge badge-cyan">{memRows.length} words</span>
          </div>
          <div style={{ border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
            <table className="mem-table">
              <thead>
                <tr>
                  <th style={{ width: 140 }}><Hash size={10} style={{ display: 'inline', marginRight: 4 }} />Address</th>
                  <th>Value (dec)</th>
                  <th>Hex</th>
                </tr>
              </thead>
              <tbody>
                {memRows.map((row, i) => {
                  const numVal = parseInt(row.val, 10);
                  const hexVal = isNaN(numVal) ? '—' : '0x' + numVal.toString(16).toUpperCase().padStart(8, '0');
                  return (
                    <tr key={i}>
                      <td className="addr">{row.addr}</td>
                      <td className="val">{row.val}</td>
                      <td style={{ color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>{hexVal}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-muted)' }}>
      <Database size={32} strokeWidth={1} />
      <p style={{ fontSize: 13 }}>{message}</p>
    </div>
  );
}
