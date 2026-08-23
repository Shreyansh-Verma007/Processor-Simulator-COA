import { useEffect, useState } from 'react';
import { Save, RefreshCw, AlertTriangle, Wifi, WifiOff } from 'lucide-react';
import CodeEditor from '../components/CodeEditor';
import RunButton from '../components/RunButton';
import OutputTabs from '../components/OutputTabs';
import ConsolePanel from '../components/ConsolePanel';
import StatsPanel from '../components/StatsPanel';
import SwapPanel from '../components/SwapPanel';
import PipelineDiagram from '../components/PipelineDiagram';
import { useSimulator } from '../hooks/useSimulator';

const EXAMPLES: { label: string; code: string }[] = [
  {
    label: 'Simple Add',
    code: `# Simple Add Example
# Computes x3 = x1 + x2

.text
LI x1, 10       # x1 = 10
LI x2, 25       # x2 = 25
ADD x3, x1, x2  # x3 = x1 + x2 = 35
ECALL           # dump registers
HALT
`,
  },
  {
    label: 'Load & Store',
    code: `# Load & Store Example
# Stores a value to memory, loads it back, and doubles it

.data
value: .word 42

.text
LI x1, 42       # x1 = 42
SW x1, 0(x0)    # store 42 at address 0
LW x2, 0(x0)    # load it back into x2
ADD x3, x2, x2  # x3 = x2 + x2 = 84 (double)
ECALL
HALT
`,
  },
  {
    label: 'Bubble Sort',
    code: `# Bubble Sort
# Sorts an array [5, 3, 8, 1, 4] in ascending order

.data
arr: .word 5, 3, 8, 1, 4

.text
    LI x10, 5        # n = 5
    LI x9, 0         # base address = 0

outer:
    LI x6, 0         # i = 0
    ADDI x7, x10, -1 # limit = n - 1

inner:
    BGE x6, x7, next_outer   # if i >= limit, go to outer
    SLL x5, x6, 2            # byte offset = i * 4
    ADD x4, x9, x5           # addr_a = base + offset
    ADDI x3, x4, 4           # addr_b = addr_a + 4
    LW x1, 0(x4)             # x1 = arr[i]
    LW x2, 0(x3)             # x2 = arr[i+1]
    BLT x1, x2, no_swap      # if arr[i] < arr[i+1], skip
    SW x2, 0(x4)             # arr[i] = arr[i+1]
    SW x1, 0(x3)             # arr[i+1] = arr[i]
no_swap:
    ADDI x6, x6, 1           # i++
    BLT x6, x7, inner        # if i < limit, repeat inner
next_outer:
    ADDI x7, x7, -1          # limit--
    BLT x0, x7, outer        # if limit > 0, repeat outer
    ECALL
    HALT
`,
  },
];

const DEFAULT_CODE = EXAMPLES[0].code;

export default function SimulatorPage() {
  const sim = useSimulator();
  const [activeTab, setActiveTab] = useState('console');
  const [editorWidth, setEditorWidth] = useState(50); // percent

  useEffect(() => {
    // Initialise editor with a local default — no server fetch needed
    sim.setAsmCode(DEFAULT_CODE);
    sim.checkBackend().then(online => {
      if (online) sim.loadOutputFiles();
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const hasData = !!(sim.consoleContent || sim.outputContent || sim.swapContent);

  const handleRun = () => {
    sim.run(sim.asmCode);
  };

  const handleExampleSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const ex = EXAMPLES.find(x => x.label === e.target.value);
    if (ex) sim.setAsmCode(ex.code);
    e.target.value = '';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>

      {/* Top bar */}
      <header style={{
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border)',
        padding: '10px 20px',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        flexShrink: 0,
      }}>
        <div style={{ flex: 1 }}>
          <h1 style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
            Pipeline Simulator
          </h1>
          <p style={{ fontSize: 11, color: 'var(--text-muted)', margin: 0 }}>
            Edit assembly code and run the RISC-V pipeline simulation
          </p>
        </div>

        {/* Backend status pill */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: '4px 10px',
          borderRadius: 20,
          background: sim.backendOnline ? 'rgba(57,255,20,0.08)' : 'rgba(255,123,114,0.08)',
          border: `1px solid ${sim.backendOnline ? 'rgba(57,255,20,0.3)' : 'rgba(255,123,114,0.3)'}`,
          fontSize: 11,
          color: sim.backendOnline ? 'var(--accent-green)' : 'var(--accent-orange)',
        }}>
          {sim.backendOnline ? <Wifi size={11} /> : <WifiOff size={11} />}
          {sim.backendOnline ? 'API Connected' : 'API Offline'}
        </div>

        {/* Reload outputs */}
        <button
          className="btn btn-ghost"
          style={{ fontSize: 12, padding: '6px 12px' }}
          onClick={sim.loadOutputFiles}
          disabled={!sim.backendOnline}
          title="Reload output files"
        >
          <RefreshCw size={13} />
          Reload
        </button>

        <RunButton
          status={sim.status}
          onRun={handleRun}
          disabled={!sim.backendOnline}
          durationMs={sim.runDurationMs}
        />
      </header>

      {/* Offline warning */}
      {!sim.backendOnline && (
        <div style={{
          background: 'rgba(255,123,114,0.1)',
          borderBottom: '1px solid rgba(255,123,114,0.3)',
          padding: '8px 20px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          fontSize: 12,
          color: 'var(--accent-orange)',
          flexShrink: 0,
        }}>
          <AlertTriangle size={13} />
          Backend API is offline. Start the Java server with:{' '}
          <code style={{ fontFamily: 'var(--font-mono)', background: 'rgba(255,123,114,0.15)', padding: '1px 6px', borderRadius: 3 }}>
            java Main --server
          </code>
          {' '}from the project root.
        </div>
      )}

      {/* Error banner */}
      {sim.status === 'error' && sim.errorMessage && (
        <div style={{
          background: 'rgba(255,123,114,0.08)',
          borderBottom: '1px solid rgba(255,123,114,0.25)',
          padding: '8px 20px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          fontSize: 12,
          color: 'var(--accent-orange)',
          flexShrink: 0,
        }}>
          <AlertTriangle size={13} />
          Simulation error: {sim.errorMessage}
        </div>
      )}

      {/* Main split pane */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

        {/* LEFT: Code editor */}
        <div style={{ width: `${editorWidth}%`, height: '100%', display: 'flex', flexDirection: 'column', borderRight: '1px solid var(--border)', overflow: 'hidden' }}>
          <div style={{
            padding: '8px 14px',
            background: 'var(--bg-surface)',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            flexShrink: 0,
          }}>
            <div className="terminal-dot" style={{ background: '#ff5f57' }} />
            <div className="terminal-dot" style={{ background: '#febc2e' }} />
            <div className="terminal-dot" style={{ background: '#28c840' }} />
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-muted)', marginLeft: 4 }}>
              input.asm
            </span>
            <div style={{ flex: 1 }} />

            {/* Examples dropdown */}
            <select
              onChange={handleExampleSelect}
              defaultValue=""
              style={{
                fontSize: 11,
                padding: '4px 8px',
                borderRadius: 4,
                border: '1px solid var(--border)',
                background: 'var(--bg-surface)',
                color: 'var(--text-muted)',
                cursor: 'pointer',
              }}
            >
              <option value="" disabled>Examples ▾</option>
              {EXAMPLES.map(ex => (
                <option key={ex.label} value={ex.label}>{ex.label}</option>
              ))}
            </select>

            <button
              className="btn btn-ghost"
              style={{ fontSize: 11, padding: '4px 8px' }}
              onClick={() => sim.run(sim.asmCode)}
              disabled={!sim.backendOnline || sim.status === 'running'}
              title="Run simulation"
            >
              <Save size={11} />
              Run
            </button>
          </div>
          <div style={{ flex: 1, height: 0, overflow: 'hidden' }}>
            <CodeEditor
              value={sim.asmCode}
              onChange={sim.setAsmCode}
              readOnly={sim.status === 'running'}
            />
          </div>
        </div>

        {/* Resize handle */}
        <div
          style={{
            width: 4,
            background: 'transparent',
            cursor: 'col-resize',
            transition: 'background 0.2s',
            flexShrink: 0,
          }}
          onMouseEnter={e => (e.currentTarget.style.background = 'var(--accent-cyan)')}
          onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
          onMouseDown={(e) => {
            e.preventDefault();
            const startX = e.clientX;
            const startW = editorWidth;
            const container = e.currentTarget.parentElement!;
            const onMove = (mv: MouseEvent) => {
              const dx = mv.clientX - startX;
              const totalW = container.offsetWidth;
              const newPct = Math.min(75, Math.max(25, startW + (dx / totalW) * 100));
              setEditorWidth(newPct);
            };
            const onUp = () => {
              window.removeEventListener('mousemove', onMove);
              window.removeEventListener('mouseup', onUp);
            };
            window.addEventListener('mousemove', onMove);
            window.addEventListener('mouseup', onUp);
          }}
        />

        {/* RIGHT: Output panels */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <OutputTabs
            activeTab={activeTab}
            onTabChange={setActiveTab}
            hasData={hasData}
          />
          <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>
            {activeTab === 'console' && <ConsolePanel content={sim.consoleContent} />}
            {activeTab === 'stats'   && <StatsPanel   content={sim.outputContent} />}
            {activeTab === 'swap'    && <SwapPanel    content={sim.swapContent} />}
            {activeTab === 'raw'     && (
              <RawView
                consoleContent={sim.consoleContent}
                outputContent={sim.outputContent}
                swapContent={sim.swapContent}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function RawView({ consoleContent, outputContent, swapContent }: {
  consoleContent: string;
  outputContent: string;
  swapContent: string;
}) {
  const [active, setActive] = useState<'console' | 'output' | 'swap'>('console');
  const content = active === 'console' ? consoleContent : active === 'output' ? outputContent : swapContent;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', gap: 4, padding: '8px 12px', borderBottom: '1px solid var(--border)', background: 'var(--bg-surface)' }}>
        {(['console', 'output', 'swap'] as const).map(f => (
          <button
            key={f}
            onClick={() => setActive(f)}
            style={{
              padding: '3px 10px',
              borderRadius: 4,
              border: '1px solid',
              borderColor: active === f ? 'var(--accent-cyan)' : 'var(--border)',
              background: active === f ? 'rgba(0,245,212,0.08)' : 'transparent',
              color: active === f ? 'var(--accent-cyan)' : 'var(--text-muted)',
              fontFamily: 'var(--font-mono)',
              fontSize: 11,
              cursor: 'pointer',
            }}
          >
            {f}.txt
          </button>
        ))}
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        <div className="terminal" style={{ height: '100%', borderRadius: 6 }}>
          {content || '(no content — run the simulation first)'}
        </div>
      </div>
    </div>
  );
}
