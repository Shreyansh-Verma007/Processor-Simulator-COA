# ── Stage 1: Compile ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy all Java source files
COPY src/ ./src/

# Compile everything into /app/out
RUN find src -name "*.java" > sources.txt && \
    mkdir -p out && \
    javac -d out @sources.txt

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes from builder
COPY --from=builder /app/out ./out

# Copy runtime assets (input.asm, phase3_traces/, existing output files)
COPY input.asm ./
COPY phase3_traces/ ./phase3_traces/

# Heroku dynamically assigns PORT — ApiServer reads it from env
# Default is 8080 for local Docker runs
EXPOSE 8080

# Start the API server
CMD ["java", "-cp", "out", "Main", "--server"]
