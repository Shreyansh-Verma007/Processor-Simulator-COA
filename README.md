# 🚀 RISC-V Pipeline Simulator

**Phase 1 – Cycle-Accurate Implementation**

A cycle-accurate 5-stage in-order RISC-V pipeline simulator written in Java, designed for modularity and future extensibility to superscalar and out-of-order execution.

---

## 🧠 Overview

This simulator models a classic **5-stage RISC-V pipeline** with accurate cycle tracking, hazard detection, and configurable execution latencies. The architecture is modular and extensible, allowing future upgrades to superscalar and out-of-order execution without structural redesign.

---

## 📜 Supported Instructions

| Type | Instructions |
|------|-------------|
| 🧮 Arithmetic | `ADD`, `SUB` |
| 💾 Memory | `LW`, `SW` |
| 🌿 Branch | `BNE` |
| 🔀 Jump | `JAL` |

---

## ✨ Features

### 🏗️ Core Pipeline
- **5-Stage Pipeline**: IF → ID → EX → MEM → WB  
- **Cycle-Accurate Simulation**: Every stage executes per clock cycle  
- **Flush-on-Taken Branches**: Correct control hazard handling  

### ⚙️ Execution Control
- **Configurable Latencies**: Per-instruction latency defined via `Config`  
- **Forwarding Support**: Optional data forwarding toggle  
- **RAW Hazard Detection**: Automatic stall insertion  

### 💾 Memory & State
- **4KB Memory**: Byte-addressable with word-aligned access  
- **Register File**: 32 general-purpose registers (x0–x31)  
- **x0 Enforcement**: Register zero remains immutable  

### 📊 Performance Metrics
- **Cycle Count**
- **Committed Instructions**
- **Stall Count**
- **IPC (Instructions Per Cycle)**

---

## 📂 Project Structure

    src/
    ├── common/          Instruction definitions, Config
    ├── compiler/        
    ├── core/            Memory, RegisterFile, Stats
    ├── pipeline/
    │   ├── stages/      IF, ID, EX, MEM, WB
    │   └── registers/   IF_ID, ID_EX, EX_MEM, MEM_WB
    └── hazard/          HazardUnit, ForwardingUnit

---

## 🛠️ Build & Run

### 📦 Compile

    javac -d out src/**/*.java

### ▶️ Run

    java -cp out core.Processor <assembly_file>

---

## ⚙️ Configuration

Latencies and forwarding behavior can be modified in `Config.java`:

    latencies.put(Opcode.ADD, 1);
    latencies.put(Opcode.MUL, 2);
    forwardingEnabled = true;

---

## 🎯 Design Goals

- Clean separation between **core processor**, **pipeline stages**, and **hazard logic**
- Modular architecture supporting future:
  - Superscalar width extension
  - Register renaming
  - Reorder Buffer (ROB)
  - Out-of-Order execution
- Clear cycle-by-cycle traceability for academic evaluation

---

<div align="center">

**RISC-V Pipeline Simulator – Phase 1**

*Accurate. Modular. Extensible.*

</div>
