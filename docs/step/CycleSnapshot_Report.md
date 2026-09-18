# CycleSnapshot.java Report

**Package:** `step`
**File:** `src/step/CycleSnapshot.java`

## Overview
`CycleSnapshot` captures the full state of the pipeline after a single clock cycle. It is designed to be easily serialized and sent to the frontend web UI for visualization during step-by-step debugging.

## Key Fields
- **Cycle Info**: Tracks `cycle`, `done`, `stall`, `flush`, and `hazardType`.
- **Pipeline Stages**: An array of `StageState` capturing the instruction label, PC, and status for the IF, ID, EX, MEM, and WB stages.
- **Registers**: The state of all 32 general-purpose registers during that cycle.
- **Stats**: Running totals for cycles, stalls, flushes, and instructions retired.

## JSON Serialization
Provides a manual `toJson()` method to construct a JSON string representation, ensuring zero external dependencies on libraries like Jackson or GSON.
