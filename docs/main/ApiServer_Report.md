# ApiServer.java Report

**Package:** `(default)` (grouped with main)
**File:** `src/ApiServer.java`

## Overview
`ApiServer` provides a lightweight HTTP API server for the RISC-V Pipeline Simulator. It uses Java's built-in `HttpServer` to expose endpoints for the web interface, avoiding external dependencies.

## Key Endpoints
- `GET /api/status`: Returns the current simulator status (`idle` or `running`).
- `GET /api/asm`: Returns the currently loaded assembly text.
- `POST /api/run`: Runs the pipeline simulation and returns the JSON result.
- `GET /api/console`, `GET /api/output`, `GET /api/swap`: Returns output text contents.
- `POST /api/trace`: Uploads and runs a `.trace` file for Phase 3 evaluation.
- Also manages step-by-step execution states using `step.StepSession`.

## Dependencies
- Built on `com.sun.net.httpserver.HttpServer`.
- Uses `step.StepSession` for interactive debugging sessions over HTTP.
