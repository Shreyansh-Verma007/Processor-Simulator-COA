interface PipelineDiagramProps {
  isRunning?: boolean;
}

const STAGES = [
  {
    id: 'IF',
    name: 'Instruction Fetch',
    abbr: 'IF',
    color: 'var(--accent-cyan)',
    glow: 'rgba(0,245,212,0.2)',
    desc: 'Fetches instruction from memory using the Program Counter (PC). Supports L1I cache lookup.',
    details: ['PC → Memory', 'L1I Cache hit/miss', 'Increment PC → PC+4'],
  },
  {
    id: 'ID',
    name: 'Instruction Decode',
    abbr: 'ID',
    color: 'var(--accent-blue)',
    glow: 'rgba(88,166,255,0.2)',
    desc: 'Decodes the instruction, reads register file, generates control signals and immediate values.',
    details: ['Register read (rs1, rs2)', 'Opcode decode', 'Immediate generation'],
  },
  {
    id: 'EX',
    name: 'Execute',
    abbr: 'EX',
    color: 'var(--accent-purple)',
    glow: 'rgba(188,140,255,0.2)',
    desc: 'ALU performs arithmetic/logic. Branch target computed. Forwarding unit resolves data hazards.',
    details: ['ALU operation', 'Branch resolution', 'Data forwarding'],
  },
  {
    id: 'MEM',
    name: 'Memory Access',
    abbr: 'MEM',
    color: 'var(--accent-yellow)',
    glow: 'rgba(227,179,65,0.2)',
    desc: 'Loads or stores data via L1D cache. TLB + page table walk for virtual memory translation.',
    details: ['L1D Cache read/write', 'TLB lookup', 'Page table walk'],
  },
  {
    id: 'WB',
    name: 'Write Back',
    abbr: 'WB',
    color: 'var(--accent-green)',
    glow: 'rgba(57,255,20,0.2)',
    desc: 'Writes computation result back to the register file. Instruction is considered retired.',
    details: ['Result → Reg File', 'Instruction retired', 'IPC updated'],
  },
];

function ArrowRight({ animated }: { animated?: boolean }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '0 4px', flexShrink: 0 }}>
      <svg width="32" height="20" viewBox="0 0 32 20">
        <defs>
          <linearGradient id="arrowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="rgba(0,245,212,0.3)" />
            <stop offset="100%" stopColor="rgba(88,166,255,0.6)" />
          </linearGradient>
        </defs>
        <line x1="2" y1="10" x2="26" y2="10"
          stroke="url(#arrowGrad)" strokeWidth="2"
          strokeDasharray={animated ? '4 2' : undefined}
        >
          {animated && (
            <animate attributeName="stroke-dashoffset" from="0" to="-12" dur="0.8s" repeatCount="indefinite" />
          )}
        </line>
        <polygon points="26,6 32,10 26,14" fill="rgba(88,166,255,0.6)" />
      </svg>
    </div>
  );
}

export default function PipelineDiagram({ isRunning = false }: PipelineDiagramProps) {
  return (
    <div style={{ padding: 16, overflow: 'auto', height: '100%', display: 'flex', flexDirection: 'column', gap: 24 }}>

      {/* Title */}
      <div>
        <h2 style={{ fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 4 }}>
          5-Stage RISC-V Pipeline
        </h2>
        <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>
          Classic in-order pipeline with hazard detection, data forwarding, and cache hierarchy
        </p>
      </div>

      {/* Stage flow */}
      <div style={{ display: 'flex', alignItems: 'stretch', overflowX: 'auto', gap: 0, paddingBottom: 8 }}>
        {STAGES.map((stage, i) => (
          <div key={stage.id} style={{ display: 'flex', alignItems: 'center' }}>
            <div
              className="stage-box"
              style={{
                minWidth: 140,
                border: `1px solid ${stage.color}40`,
                background: `linear-gradient(135deg, ${stage.glow}, var(--bg-elevated))`,
              }}
            >
              {/* Stage abbreviation badge */}
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 36,
                height: 36,
                borderRadius: 8,
                background: `${stage.color}20`,
                border: `1px solid ${stage.color}60`,
                marginBottom: 10,
              }}>
                <span style={{
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 800,
                  fontSize: 13,
                  color: stage.color,
                }}>
                  {stage.abbr}
                </span>
              </div>

              <div style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-primary)', marginBottom: 4 }}>
                {stage.name}
              </div>

              <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.5 }}>
                {stage.desc}
              </div>

              {/* Detail bullets */}
              <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 3 }}>
                {stage.details.map((d, j) => (
                  <div key={j} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ width: 4, height: 4, borderRadius: '50%', background: stage.color, flexShrink: 0 }} />
                    <span style={{ fontSize: 10, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>{d}</span>
                  </div>
                ))}
              </div>
            </div>

            {i < STAGES.length - 1 && <ArrowRight animated={isRunning} />}
          </div>
        ))}
      </div>

      {/* Hazard info */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
        {[
          {
            title: 'Data Hazards',
            color: 'var(--accent-orange)',
            desc: 'RAW hazards detected by the Hazard Detection Unit. Resolved via data forwarding from EX/MEM/WB stages, or stalling if necessary.',
          },
          {
            title: 'Control Hazards',
            color: 'var(--accent-purple)',
            desc: 'Branch outcomes resolved in EX stage. Mis-predicted instructions flushed (Branch Flushes counter). Assume not-taken by default.',
          },
          {
            title: 'Cache Hierarchy',
            color: 'var(--accent-blue)',
            desc: 'L1D (4KB, direct-mapped, 64B blocks). On miss, 10-cycle main memory penalty. L1I disabled; instruction fetches go direct to memory.',
          },
          {
            title: 'Virtual Memory',
            color: 'var(--accent-cyan)',
            desc: '16-entry DTLB with LRU replacement. TLB miss triggers page table walk (10 cycles). Page faults add 50-cycle penalty + swap I/O.',
          },
        ].map(({ title, color, desc }) => (
          <div key={title} style={{
            background: 'var(--bg-elevated)',
            border: `1px solid ${color}30`,
            borderRadius: 8,
            padding: 14,
          }}>
            <div style={{ fontWeight: 600, fontSize: 12, color, marginBottom: 6 }}>{title}</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.6 }}>{desc}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
