import { BookOpen, Users, Code2, Layers, Cpu, HardDrive, Zap } from 'lucide-react';

const PHASES = [
  {
    phase: 'Phase 1',
    title: 'Basic Pipeline',
    color: 'var(--accent-cyan)',
    desc: 'Implemented the 5-stage RISC-V pipeline (IF/ID/EX/MEM/WB) with hazard detection and data forwarding. Supported R-Type, I-Type, Branch, and Jump instructions.',
    items: ['5-stage in-order pipeline', 'RAW hazard detection unit', 'Data forwarding paths', 'Branch flush on mis-predict'],
  },
  {
    phase: 'Phase 2',
    title: 'Cache Hierarchy',
    color: 'var(--accent-blue)',
    desc: 'Added a configurable L1D cache (4KB, 64B blocks, direct-mapped, LRU). Integrated cache miss penalties into the cycle count. Stats printed to output.txt.',
    items: ['L1D cache simulation', 'Cache hit/miss tracking', 'Configurable block size & associativity', 'Miss penalty integration'],
  },
  {
    phase: 'Phase 3',
    title: 'Virtual Memory & Swap',
    color: 'var(--accent-purple)',
    desc: 'Added a 16-entry DTLB, full page table walk, page fault handling, and a swap space simulation. Also added trace replay mode for batch cache analysis.',
    items: ['DTLB (16 entries, LRU)', 'Page table walk (10 cycles)', 'Page fault handler (50 cycles)', 'Swap in/out tracking', 'Trace replay mode', 'Batch trace analysis'],
  },
];

const FEATURES = [
  { icon: <Cpu size={18} />,      color: 'var(--accent-cyan)',   title: 'RISC-V RV32I',     desc: 'Supports R/I/S/B/J-type instructions including MUL, DIV, LW, SW, BEQ, JAL, HALT' },
  { icon: <Zap size={18} />,      color: 'var(--accent-green)',  title: 'Data Forwarding',   desc: 'EX→EX and MEM→EX forwarding eliminates most pipeline stalls' },
  { icon: <Layers size={18} />,   color: 'var(--accent-blue)',   title: 'Cache Simulation',  desc: 'L1D with configurable size, associativity, block size, and replacement policy' },
  { icon: <HardDrive size={18} />,color: 'var(--accent-purple)', title: 'Virtual Memory',    desc: '4GB virtual space, 256KB physical, DTLB, page table walk, swap space' },
  { icon: <Code2 size={18} />,    color: 'var(--accent-yellow)', title: 'Assembler',         desc: 'Built-in compiler supporting labels, .data section, pseudo-instructions' },
  { icon: <BookOpen size={18} />, color: 'var(--accent-orange)', title: 'Trace Replay',      desc: 'Batch trace mode for comparative cache performance analysis' },
];

export default function AboutPage() {
  return (
    <div style={{ height: '100vh', overflow: 'auto', background: 'var(--bg-base)' }}>

      {/* Hero */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(0,245,212,0.05) 0%, rgba(88,166,255,0.05) 50%, rgba(188,140,255,0.05) 100%)',
        borderBottom: '1px solid var(--border)',
        padding: '40px 32px',
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* Background decoration */}
        <div style={{
          position: 'absolute', top: 0, right: 0, width: 300, height: 300,
          background: 'radial-gradient(circle, rgba(0,245,212,0.08) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />

        <div style={{ maxWidth: 800, position: 'relative' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <div style={{
              width: 48, height: 48,
              borderRadius: 12,
              background: 'linear-gradient(135deg, rgba(0,245,212,0.2), rgba(88,166,255,0.2))',
              border: '1px solid rgba(0,245,212,0.4)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Zap size={24} color="var(--accent-cyan)" />
            </div>
            <div>
              <h1 style={{ fontSize: 22, fontWeight: 800, color: 'var(--text-primary)', margin: 0 }}>
                RISC-V Pipeline Simulator
              </h1>
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                Computer Organization & Architecture · IIT · Semester 5
              </div>
            </div>
          </div>

          <p style={{ fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.8, marginBottom: 16, maxWidth: 660 }}>
            A fully-featured RISC-V RV32I processor simulator built in Java, implementing a 5-stage
            in-order pipeline with hazard detection, data forwarding, an L1D cache hierarchy, and a
            complete virtual memory subsystem including TLB, page table walks, and swap space.
          </p>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {['RISC-V', 'Pipeline', 'Cache', 'Virtual Memory', 'Trace Replay', 'Java', 'React', 'TypeScript'].map(tag => (
              <span key={tag} className="badge badge-cyan">{tag}</span>
            ))}
          </div>
        </div>
      </div>

      <div style={{ padding: 32, display: 'flex', flexDirection: 'column', gap: 32, maxWidth: 1100 }}>

        {/* Features grid */}
        <section>
          <h2 style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Zap size={14} color="var(--accent-cyan)" />
            Key Features
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 14 }}>
            {FEATURES.map(({ icon, color, title, desc }) => (
              <div key={title} className="glass-card" style={{ padding: 16, display: 'flex', gap: 12, transition: 'all 0.2s' }}>
                <div style={{
                  width: 36, height: 36, borderRadius: 8,
                  background: `${color}18`,
                  border: `1px solid ${color}40`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  color,
                }}>
                  {icon}
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)', marginBottom: 4 }}>{title}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.6 }}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Project phases timeline */}
        <section>
          <h2 style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <BookOpen size={14} color="var(--accent-cyan)" />
            Project Phases
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            {PHASES.map((phase, i) => (
              <div key={phase.phase} style={{ display: 'flex', gap: 20 }}>
                {/* Timeline line */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 40, flexShrink: 0 }}>
                  <div style={{
                    width: 28, height: 28,
                    borderRadius: '50%',
                    background: `${phase.color}20`,
                    border: `2px solid ${phase.color}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1,
                    flexShrink: 0,
                  }}>
                    <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: 10, color: phase.color }}>
                      P{i + 1}
                    </span>
                  </div>
                  {i < PHASES.length - 1 && (
                    <div style={{ width: 2, flex: 1, background: `${phase.color}30`, minHeight: 24 }} />
                  )}
                </div>

                {/* Content */}
                <div style={{ paddingBottom: i < PHASES.length - 1 ? 24 : 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                    <span className="badge" style={{ background: `${phase.color}18`, color: phase.color }}>{phase.phase}</span>
                    <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text-primary)' }}>{phase.title}</span>
                  </div>
                  <p style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: 10 }}>
                    {phase.desc}
                  </p>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    {phase.items.map(item => (
                      <span key={item} style={{
                        padding: '2px 8px',
                        borderRadius: 4,
                        background: 'var(--bg-elevated)',
                        border: `1px solid ${phase.color}30`,
                        fontSize: 11,
                        color: 'var(--text-secondary)',
                        fontFamily: 'var(--font-mono)',
                      }}>
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* How to use */}
        <section className="glass-card" style={{ padding: 20 }}>
          <h2 style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Code2 size={14} color="var(--accent-cyan)" />
            How to Use
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {[
              { step: '1', text: 'Compile the Java backend:', code: 'javac -d out src/**/*.java src/*.java' },
              { step: '2', text: 'Start the API server:', code: 'java -cp out Main --server' },
              { step: '3', text: 'Start the frontend dev server:', code: 'cd web && npm run dev' },
              { step: '4', text: 'Open the simulator in your browser:', code: 'http://localhost:5173' },
            ].map(({ step, text, code }) => (
              <div key={step} style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <div style={{
                  width: 24, height: 24, borderRadius: '50%',
                  background: 'rgba(0,245,212,0.15)',
                  border: '1px solid rgba(0,245,212,0.4)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 11, fontWeight: 700, color: 'var(--accent-cyan)',
                  flexShrink: 0,
                }}>
                  {step}
                </div>
                <div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>{text}</div>
                  <code style={{
                    display: 'block',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 12,
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    borderRadius: 4,
                    padding: '6px 10px',
                    color: 'var(--accent-green)',
                  }}>
                    {code}
                  </code>
                </div>
              </div>
            ))}
          </div>
        </section>

      </div>
    </div>
  );
}
