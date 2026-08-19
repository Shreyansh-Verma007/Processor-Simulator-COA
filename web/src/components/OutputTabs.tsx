import { Terminal, BarChart2, HardDrive, FileText } from 'lucide-react';

interface OutputTabsProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  hasData: boolean;
}

const TABS = [
  { id: 'console', label: 'Console', icon: Terminal,   badge: 'Memory' },
  { id: 'stats',   label: 'Stats',   icon: BarChart2,  badge: 'Output'  },
  { id: 'swap',    label: 'Swap',    icon: HardDrive,  badge: 'VM'     },
  { id: 'raw',     label: 'Raw',     icon: FileText,   badge: 'Logs'   },
];

export default function OutputTabs({ activeTab, onTabChange, hasData }: OutputTabsProps) {
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
          {hasData && id !== 'raw' && (
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
        </button>
      ))}
    </div>
  );
}
