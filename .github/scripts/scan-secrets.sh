#!/usr/bin/env bash
# Scans a directory for secrets with gitleaks and fails when it finds any.
# Used twice in the analysis job: on the analysis input (before anything goes to the
# LLM) and on the report (before it is published). The report file only contains
# rule ids and locations: --redact keeps the secrets themselves out of it and out of the log.
#
# Usage: scan-secrets.sh <directory> <reportFile>
# Exit:  0 = no findings, 1 = findings or scanner error

set -uo pipefail

dir="$1"
report="$2"

gitleaks dir "$dir" --no-banner --redact --report-format json --report-path "$report" --exit-code 2
code=$?

case "$code" in
  0)
    echo "No secrets found in $dir."
    exit 0
    ;;
  2)
    echo "::error::Secret scanner found findings in $dir. Nothing from it may be sent or published; see $(basename "$report")."
    node -e '
      const findings = require(process.argv[1]);
      for (const f of findings) console.log(`  ${f.RuleID}: ${f.File}:${f.StartLine}`);
    ' "$(realpath "$report")"
    exit 1
    ;;
  *)
    echo "::error::Secret scanner failed (exit code $code). Treating this as a finding: fail closed."
    exit 1
    ;;
esac
