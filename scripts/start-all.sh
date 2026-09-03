#!/usr/bin/env bash
# Starts every ValuBank service plus the frontend as background processes.
# Logs go to ./logs/<service>.log. Ctrl+C stops everything started by this script.

set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

PIDS=()

cleanup() {
    echo ""
    echo "Stopping all ValuBank processes..."
    for pid in "${PIDS[@]}"; do
        kill "$pid" 2>/dev/null || true
    done
}
trap cleanup EXIT INT TERM

start_service() {
    local name="$1"
    local path="$2"
    echo "Starting $name..."
    (cd "$path" && mvn spring-boot:run) > "$LOG_DIR/$name.log" 2>&1 &
    PIDS+=($!)
}

start_service "interest-rate-service" "$ROOT/services/interest-rate-service"
start_service "fraud-service" "$ROOT/services/fraud-service"
start_service "accounts-service" "$ROOT/services/accounts-service"
start_service "payments-service" "$ROOT/services/payments-service"

echo "Starting frontend..."
(
    cd "$ROOT/frontend"
    if [ ! -d node_modules ]; then npm install; fi
    npm run dev
) > "$LOG_DIR/frontend.log" 2>&1 &
PIDS+=($!)

echo ""
echo "All services starting (logs in $LOG_DIR):"
echo "  Interest Rate Service : http://localhost:8084"
echo "  Fraud Service          : http://localhost:8083"
echo "  Accounts Service       : http://localhost:8081"
echo "  Payments Service       : http://localhost:8082"
echo "  Frontend               : http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop all services."
wait
