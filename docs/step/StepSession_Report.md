# StepSession.java Report

**Package:** `step`
**File:** `src/step/StepSession.java`

## Overview
`StepSession` holds the full mutable pipeline state between interactive step requests. Unlike `PipelineController`, which runs a program continuously to completion, `StepSession` advances the pipeline exactly one clock cycle per request and pauses.

## Key Functionality
- **State Management**: Maintains instances of all pipeline stages (`IF`, `ID`, `EX`, `MEM`, `WB`), hazard units, and pipeline registers.
- **Single Step Execution**: Provides a `step()` method that ticks the stages in reverse order and returns a `CycleSnapshot`.
- **Dynamic Simulation**: Handles cache stalls, branch flushes, and data hazards dynamically cycle-by-cycle, precisely mirroring `PipelineController`'s logic.

## Dependencies
- Uses `core.Memory`, `core.RegisterFile`, `core.Stats`, and `cache.CacheHierarchy`.
- Interacts with all classes in `pipeline_stages` and `pipeline_registers`.
