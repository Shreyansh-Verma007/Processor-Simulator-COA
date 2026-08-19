import PipelineDiagram from '../components/PipelineDiagram';
import { Cpu, Layers, GitBranch, HardDrive, Zap } from 'lucide-react';

export default function ArchitecturePage() {
  return (
    <div style={{ height: '100vh', overflow: 'auto', background: 'var(--bg-base)' }}>
      {/* Header */}
      <div style={{
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border)',
        padding: '16px 24px',
        position: 'sticky',
        top: 0,
        zIndex: 10,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Cpu size={18} color="var(--accent-cyan)" />
          <div>
            <h1 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
              Architecture Overview
            </h1>
            <p style={{ fontSize: 11, color: 'var(--text-muted)', margin: 0 }}>
              RISC-V RV32I Pipeline with Cache & Virtual Memory
            </p>
          </div>
        </div>
      </div>

      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 1200 }}>

        {/* Pipeline diagram */}
        <section className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ borderBottom: '1px solid var(--border)', padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Zap size={14} color="var(--accent-cyan)" />
            <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Pipeline Stages</span>
          </div>
          <PipelineDiagram />
        </section>

        {/* Instruction set table */}
        <section className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ borderBottom: '1px solid var(--border)', padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Layers size={14} color="var(--accent-purple)" />
            <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Supported Instructions</span>
          </div>
          <div style={{ overflow: 'auto', padding: 16 }}>
            <table className="mem-table" style={{ tableLayout: 'auto' }}>
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Instructions</th>
                  <th>Latency</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {[
                  { type: 'R-Type', instr: 'ADD, SUB, SLL, SRL, XOR, OR, AND', lat: '1 cycle', note: 'Integer arithmetic & logic', color: 'var(--accent-cyan)' },
                  { type: 'R-Type', instr: 'MUL', lat: '3 cycles', note: 'Multi-cycle multiply', color: 'var(--accent-cyan)' },
                  { type: 'R-Type', instr: 'DIV', lat: '4 cycles', note: 'Multi-cycle divide', color: 'var(--accent-cyan)' },
                  { type: 'I-Type', instr: 'ADDI, LI, LW, LB', lat: '1 cycle', note: 'Immediate & load operations', color: 'var(--accent-blue)' },
                  { type: 'S-Type', instr: 'SW, SB', lat: '1 cycle', note: 'Store to memory', color: 'var(--accent-yellow)' },
                  { type: 'B-Type', instr: 'BEQ, BNE, BLT, BGE', lat: '1 cycle', note: 'Branches resolved in EX stage', color: 'var(--accent-purple)' },
                  { type: 'J-Type', instr: 'JAL', lat: '1 cycle', note: 'Jump and link', color: 'var(--accent-green)' },
                  { type: 'Pseudo', instr: 'LA, MV, NOP', lat: '1 cycle', note: 'Pseudo-instructions (assembler expands)', color: 'var(--accent-orange)' },
                  { type: 'System', instr: 'ECALL, HALT', lat: '1 cycle', note: 'System calls & program termination', color: 'var(--text-muted)' },
                ].map(({ type, instr, lat, note, color }) => (
                  <tr key={instr}>
                    <td><span className="badge" style={{ background: `${color}18`, color }}>{type}</span></td>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-primary)' }}>{instr}</td>
                    <td style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-yellow)' }}>{lat}</td>
                    <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>{note}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* Hazard & Cache info */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>

          <section className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ borderBottom: '1px solid var(--border)', padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
              <GitBranch size={14} color="var(--accent-orange)" />
              <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Hazard Handling</span>
            </div>
            <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[
                { title: 'RAW Hazard Detection', desc: 'Checks if EX/MEM/WB stage has a result needed by the current ID stage instruction.' },
                { title: 'Data Forwarding', desc: 'Forwards results from EX→EX and MEM→EX paths without stalling when possible.' },
                { title: 'Load-Use Stall', desc: 'LW followed by dependent instruction: inserts 1 bubble (stall cycle).' },
                { title: 'Branch Flush', desc: 'On taken branch detected in EX: flushes IF and ID pipeline registers (2-cycle penalty).' },
              ].map(({ title, desc }) => (
                <div key={title} style={{ borderLeft: '2px solid var(--accent-orange)', paddingLeft: 12 }}>
                  <div style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-primary)', marginBottom: 3 }}>{title}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{desc}</div>
                </div>
              ))}
            </div>
          </section>

          <section className="glass-card" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ borderBottom: '1px solid var(--border)', padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
              <HardDrive size={14} color="var(--accent-blue)" />
              <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>Memory Subsystem</span>
            </div>
            <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[
                { title: 'L1D Cache', detail: '4 KB, 64B blocks, 1-way (direct-mapped), 1-cycle hit', color: 'var(--accent-cyan)' },
                { title: 'Main Memory', detail: '10-cycle latency on cache miss', color: 'var(--accent-blue)' },
                { title: 'DTLB', detail: '16 entries, LRU replacement, 1-cycle hit, 10-cycle miss (page walk)', color: 'var(--accent-purple)' },
                { title: 'Swap Space', detail: 'Physical: 256 KB (64 frames × 4KB). Virtual: 4GB address space.', color: 'var(--accent-yellow)' },
                { title: 'Page Fault', detail: '50-cycle penalty. LRU page evicted to swap if physical memory full.', color: 'var(--accent-orange)' },
              ].map(({ title, detail, color }) => (
                <div key={title} style={{ borderLeft: `2px solid ${color}`, paddingLeft: 12 }}>
                  <div style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-primary)', marginBottom: 3 }}>{title}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>{detail}</div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
