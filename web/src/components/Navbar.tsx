import { Cpu, Code2, BookOpen, Info, Zap, GitBranch } from 'lucide-react';

interface NavbarProps {
  activePage: string;
  onNavigate: (page: string) => void;
  backendOnline: boolean;
}

const navItems = [
  { id: 'simulator',    label: 'Simulator',     icon: Code2       },
  { id: 'trace',        label: 'Trace Replay',  icon: GitBranch   },
  { id: 'architecture', label: 'Architecture',  icon: Cpu         },
  { id: 'about',        label: 'About',         icon: Info        },
];

export default function Navbar({ activePage, onNavigate, backendOnline }: NavbarProps) {
  return (
    <aside
      style={{
        width: 220,
        minWidth: 220,
        background: 'var(--bg-surface)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        height: '100vh',
        position: 'sticky',
        top: 0,
        userSelect: 'none',
      }}
    >
      {/* Logo */}
      <div style={{ padding: '20px 16px 16px', borderBottom: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 8,
              background: 'linear-gradient(135deg, rgba(0,245,212,0.2), rgba(88,166,255,0.2))',
              border: '1px solid rgba(0,245,212,0.4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Zap size={18} color="var(--accent-cyan)" />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-primary)', lineHeight: 1.2 }}>
              RISC-V
            </div>
            <div style={{ fontWeight: 400, fontSize: 11, color: 'var(--text-secondary)', lineHeight: 1.2 }}>
              Pipeline Simulator
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav style={{ padding: '12px 8px', flex: 1 }}>
        <div style={{ fontSize: 10, fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.1em', textTransform: 'uppercase', padding: '0 8px', marginBottom: 6 }}>
          Navigation
        </div>
        {navItems.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            className={`nav-item ${activePage === id ? 'active' : ''}`}
            style={{ width: '100%', background: 'none', border: '1px solid transparent', marginBottom: 2, cursor: 'pointer', textAlign: 'left' }}
            onClick={() => onNavigate(id)}
          >
            <Icon size={15} />
            <span>{label}</span>
          </button>
        ))}
      </nav>

      {/* Backend status */}
      <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: backendOnline ? 'var(--accent-green)' : 'var(--accent-orange)',
              boxShadow: backendOnline ? '0 0 6px rgba(57,255,20,0.6)' : '0 0 6px rgba(255,123,114,0.6)',
            }}
          />
          <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
            {backendOnline ? 'Backend online' : 'Backend offline'}
          </span>
        </div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
          Java API · port 8080
        </div>
      </div>

      {/* Footer */}
      <div style={{ padding: '10px 16px', borderTop: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 6 }}>
        <BookOpen size={12} color="var(--text-muted)" />
        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>COA — IIT Phase 3</span>
      </div>
    </aside>
  );
}
