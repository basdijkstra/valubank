#!/usr/bin/env bash
# Stops whatever is listening on ValuBank's service ports. Safe to run any
# time - only touches these five ports, regardless of how the process was
# started (start-all.sh, an IDE, manually, ...).

PORTS=(8081 8082 8083 8084 5173)
NAMES=(accounts-service payments-service fraud-service interest-rate-service frontend)

stop_port() {
    local port="$1"
    local name="$2"

    if command -v lsof >/dev/null 2>&1; then
        local pids
        pids=$(lsof -ti tcp:"$port" 2>/dev/null)
        if [ -z "$pids" ]; then
            echo "$name (:$port): nothing listening"
            return
        fi
        for pid in $pids; do
            echo "$name (:$port): stopping PID $pid"
            kill -9 "$pid" 2>/dev/null
        done
    elif command -v netstat >/dev/null 2>&1; then
        # Windows / Git Bash: no lsof, parse netstat -ano instead.
        local pids
        pids=$(netstat -ano 2>/dev/null | grep -E "LISTENING" | grep -E ":$port[[:space:]]" | awk '{print $NF}' | sort -u)
        if [ -z "$pids" ]; then
            echo "$name (:$port): nothing listening"
            return
        fi
        for pid in $pids; do
            echo "$name (:$port): stopping PID $pid"
            taskkill //PID "$pid" //F >/dev/null 2>&1
        done
    else
        echo "$name (:$port): no lsof or netstat available, cannot check"
    fi
}

for i in "${!PORTS[@]}"; do
    stop_port "${PORTS[$i]}" "${NAMES[$i]}"
done

echo ""
echo "Done. Leftover terminal windows (if any) can be closed manually."
