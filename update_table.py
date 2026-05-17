import os
import re

files_to_update = ['README.md', 'RISCV Pipeline Simulator.md', 'phase3_report.md']
output_dir = 'traces_output'

def parse_val(text, key):
    match = re.search(fr'{key}\s*:\s*([\d\.]+)', text)
    if match:
        return match.group(1)
    return "0"

new_rows = []
for i in range(1, 11):
    trace_name = f'trace{i:02d}'
    out_file = os.path.join(output_dir, f'{trace_name}_output.txt')
    if not os.path.exists(out_file):
        print(f"Warning: {out_file} not found. Using placeholder.")
        row = f"| `{trace_name}` | - | - | - | - | - | - | - | - | - | - | - |\n"
        new_rows.append((trace_name, row))
        continue
    
    with open(out_file, 'r', encoding='utf-8') as f:
        content = f.read()
        
    total_cycles = parse_val(content, 'Total Cycles')
    instr = parse_val(content, 'Instructions Retired')
    ipc = parse_val(content, 'IPC')
    stalls = parse_val(content, 'Stalls')
    tlb_hits = parse_val(content, 'TLB Hits')
    tlb_misses = parse_val(content, 'TLB Misses')
    page_walks = parse_val(content, 'Page Walks')
    page_faults = parse_val(content, 'Page Faults')
    evictions = parse_val(content, 'Page Evictions')
    dirty_evic = parse_val(content, 'Dirty Evictions')
    translation_penalty = parse_val(content, 'Translation Penalty Cycles')
    
    row = f"| `{trace_name}` | {total_cycles} | {instr} | {ipc} | {stalls} | {tlb_hits} | {tlb_misses} | {page_walks} | {page_faults} | {evictions} | {dirty_evic} | {translation_penalty} |\n"
    new_rows.append((trace_name, row))

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    start_idx = -1
    for i, line in enumerate(lines):
        if "| Trace | Total Cycles | Instr Retired" in line:
            start_idx = i
            break

    if start_idx != -1:
        data_start_idx = start_idx + 2
        data_end_idx = data_start_idx
        while data_end_idx < len(lines) and lines[data_end_idx].strip().startswith('| `trace'):
            data_end_idx += 1
        
        table_lines = lines[:data_start_idx]
        for _, row in new_rows:
            table_lines.append(row)
        table_lines.extend(lines[data_end_idx:])
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(table_lines)
        print(f"{filepath} table updated successfully.")
    else:
        print(f"Could not find table in {filepath}")
