import Editor from '@monaco-editor/react';
import type * as Monaco from 'monaco-editor';

interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

// Register RISC-V assembly language on first mount
function registerRiscvLanguage(monaco: typeof Monaco) {
  const id = 'riscv-asm';
  if (monaco.languages.getLanguages().some(l => l.id === id)) return;

  monaco.languages.register({ id });

  monaco.languages.setMonarchTokensProvider(id, {
    keywords: [
      'ADD','SUB','MUL','DIV','SLL','SRL','XOR','OR','AND',
      'ADDI','LW','LB','SW','SB','LI','LA','MV','NOP',
      'BEQ','BNE','BLT','BGE','JAL','ECALL','HALT',
      'add','sub','mul','div','sll','srl','xor','or','and',
      'addi','lw','lb','sw','sb','li','la','mv','nop',
      'beq','bne','blt','bge','jal','ecall','halt',
    ],
    directives: ['.data','.text','.word','.half','.byte','.space','.zero',
                 '.ascii','.asciiz','.string','.align','.globl','.global'],
    registers: [
      'x0','x1','x2','x3','x4','x5','x6','x7','x8','x9',
      'x10','x11','x12','x13','x14','x15','x16','x17','x18','x19',
      'x20','x21','x22','x23','x24','x25','x26','x27','x28','x29','x30','x31',
      'zero','ra','sp','gp','tp',
      'a0','a1','a2','a3','a4','a5','a6','a7',
      't0','t1','t2','t3','t4','t5','t6',
      's0','s1','s2','s3','s4','s5','s6','s7','s8','s9','s10','s11',
      'fp',
    ],
    tokenizer: {
      root: [
        [/#.*$/, 'comment'],
        [/\/\/.*$/, 'comment'],
        [/\.[a-zA-Z]+/, {
          cases: {
            '@directives': 'keyword.directive',
            '@default': 'identifier',
          },
        }],
        [/[a-zA-Z_][a-zA-Z0-9_]*:/, 'type.identifier'],
        [/[a-zA-Z_][a-zA-Z0-9_]*/, {
          cases: {
            '@keywords': 'keyword',
            '@registers': 'variable',
            '@default': 'identifier',
          },
        }],
        [/0x[0-9a-fA-F]+/, 'number.hex'],
        [/-?\d+/, 'number'],
        [/"[^"]*"/, 'string'],
        [/[(),]/, 'delimiter'],
      ],
    },
  });

  monaco.editor.defineTheme('riscv-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'keyword',          foreground: '00f5d4', fontStyle: 'bold' },
      { token: 'keyword.directive',foreground: 'bc8cff' },
      { token: 'variable',         foreground: '58a6ff' },
      { token: 'type.identifier',  foreground: 'e3b341' },
      { token: 'comment',          foreground: '484f58', fontStyle: 'italic' },
      { token: 'number',           foreground: 'a8ff78' },
      { token: 'number.hex',       foreground: 'a8ff78' },
      { token: 'string',           foreground: 'ff9a76' },
      { token: 'delimiter',        foreground: '8b949e' },
    ],
    colors: {
      'editor.background':          '#0a0e14',
      'editor.foreground':          '#e6edf3',
      'editor.lineHighlightBackground': '#161b22',
      'editorLineNumber.foreground':'#484f58',
      'editorLineNumber.activeForeground': '#8b949e',
      'editor.selectionBackground': '#2d333b',
      'editorCursor.foreground':    '#00f5d4',
      'editorIndentGuide.background': '#21262d',
    },
  });
}

export default function CodeEditor({ value, onChange, readOnly = false }: CodeEditorProps) {
  return (
    <div style={{ width: '100%', height: '100%', overflow: 'hidden', borderRadius: 8, border: '1px solid var(--border)' }}>
      <Editor
        height="100%"
        language="riscv-asm"
        theme="riscv-dark"
        value={value}
        options={{
          readOnly,
          fontSize: 13,
          fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
          fontLigatures: true,
          lineNumbers: 'on',
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          automaticLayout: true,
          tabSize: 4,
          renderWhitespace: 'selection',
          smoothScrolling: true,
          cursorBlinking: 'smooth',
          padding: { top: 12, bottom: 12 },
          wordWrap: 'off',
          glyphMargin: false,
          folding: true,
          renderLineHighlight: 'line',
          overviewRulerLanes: 0,
          scrollbar: {
            verticalScrollbarSize: 6,
            horizontalScrollbarSize: 6,
          },
        }}
        onChange={(val) => onChange(val ?? '')}
        beforeMount={(monaco) => registerRiscvLanguage(monaco)}
      />
    </div>
  );
}
