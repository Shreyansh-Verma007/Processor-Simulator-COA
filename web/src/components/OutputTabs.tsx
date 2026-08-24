import { Terminal, BarChart2, HardDrive, FileText, Database, Settings } from 'lucide-react';

interface OutputTabsProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  hasData: boolean;
  configModified?: boolean;
}

const TABS = [
  { id: 'console',  label: 'Console',  icon: Terminal  },
  { id: 'stats',    label: 'Stats',    icon: BarChart2 },
  { id: 'memory',   label: 'Memory',   icon: Database  },
  { id: 'swap',     label: 'Swap',     icon: HardDrive },
  { id: 'raw',      label: 'Raw',      icon: FileText  },
  { id: 'config',   label: 'Config',   icon: Settings  },
];

export default function OutputTabs({ activeTab, onTabChange, hasData, configModified }: OutputTabsProps) {
  return (
    <div className="tab-bar" style={{ gap: 2 }}>
      {TABS.map(({ id, label, icon: Icon }) => (
        <button
          key={id}
          className={`tab ${activeTab === id ? 'active' : ''}`}
          onClick={() => onTabChange(id)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', fontFamily: 'var(--font-ui)' }}
        >
          <Icon size={13} />
          {label}
          {/* Green dot for data-backed tabs when simulation has run */}
          {hasData && (id === 'console' || id === 'stats' || id === 'memory' || id === 'swap') && (
            <span
              style={{
                width: 6, height: 6,
                borderRadius: '50%',
                background: 'var(--accent-green)',
                display: 'inline-block',
                boxShadow: '0 0 4px rgba(57,255,20,0.6)',
              }}
            />
          )}
          {/* Yellow dot for config tab when settings have been modified */}
          {id === 'config' && configModified && (
            <span
              style={{
                width: 6, height: 6,
                borderRadius: '50%',
                background: 'var(--accent-yellow)',
                display: 'inline-block',
                boxShadow: '0 0 4px rgba(227,179,65,0.6)',
              }}
            />
          )}
        </button>
      ))}
    </div>
  );
}
