// Turns the Claude analysis (<dir>/claude-output.json) and the redacted failures
// (<dir>/failures.json) into <dir>/findings.json and <dir>/report.md. The workflow adds
// the report to the job summary only after the secret scan of the output has passed.
//
// The status per JDK version comes from failures.json, not from the model, so that
// part of the report is always correct, even when the analysis is missing or invalid.
// Everything the model wrote is escaped: text in logs or test output could otherwise
// make the model put links or markup into the published report.
//
// Usage: node render-e2e-analysis.mjs <dir>
// Env:   ANALYSIS_OUTCOME   - outcome of the analysis step (success, failure or skipped)
//        INPUT_SCAN_OUTCOME - outcome of the secret scan of the analysis input
//        HAS_ANTHROPIC_KEY  - 'true' when the API key is available
// Exits non-zero when the analysis ran but produced no valid result.

import fs from 'node:fs';
import path from 'node:path';

const [dir = 'failure-analysis'] = process.argv.slice(2);
const outcome = process.env.ANALYSIS_OUTCOME || 'skipped';
const inputScanOutcome = process.env.INPUT_SCAN_OUTCOME || 'success';
const hasAnthropicKey = process.env.HAS_ANTHROPIC_KEY !== 'false';

const VERDICTS = ['test-problem', 'application-problem', 'environment', 'flaky', 'inconclusive'];
const CONFIDENCES = ['high', 'medium', 'low'];
const STATUS_LABELS = { expected: 'passed', unexpected: '❌ failed', flaky: '⚠️ flaky', skipped: 'skipped', 'not run': '—' };

const failures = JSON.parse(fs.readFileSync(path.join(dir, 'failures.json'), 'utf8'));
const manifestPath = path.join(dir, 'manifest.json');
const manifest = fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) : null;
const expectedIds = [
  ...failures.tests.map((test) => test.id),
  ...failures.legsWithoutResults.map((leg) => `JDK${leg.jdk}`)
];

function readClaudeOutput() {
  const file = path.join(dir, 'claude-output.json');
  if (!fs.existsSync(file) || fs.statSync(file).size === 0) {
    return { problems: ['Claude produced no output.'] };
  }
  let output;
  try {
    output = JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (error) {
    return { problems: [`Claude output is not valid JSON: ${error.message}`] };
  }
  const meta = { costUsd: output.total_cost_usd ?? null, turns: output.num_turns ?? null, durationMs: output.duration_ms ?? null };
  if (output.is_error) {
    return { meta, problems: [`Claude ended with an error (${output.subtype ?? 'unknown'}): ${output.result ?? ''}`] };
  }
  let analysis = output.structured_output;
  if (!analysis && typeof output.result === 'string') {
    try {
      analysis = JSON.parse(output.result);
    } catch {
      return { meta, problems: ['Claude returned text instead of the structured analysis.'] };
    }
  }
  return { meta, analysis, problems: validate(analysis) };
}

function validate(analysis) {
  const problems = [];
  if (!analysis || typeof analysis.summary !== 'string' || !Array.isArray(analysis.findings)) {
    return ['The analysis does not have a summary and a findings list.'];
  }
  for (const finding of analysis.findings) {
    const label = finding.testId ?? '(no testId)';
    if (!expectedIds.includes(finding.testId)) problems.push(`${label}: unknown test id.`);
    if (!VERDICTS.includes(finding.verdict)) problems.push(`${label}: invalid verdict "${finding.verdict}".`);
    if (!CONFIDENCES.includes(finding.confidence)) problems.push(`${label}: invalid confidence "${finding.confidence}".`);
    if (!Array.isArray(finding.evidence) || finding.evidence.length === 0) problems.push(`${label}: no evidence.`);
  }
  return problems;
}

// Markdown-escapes text that comes from the model or from test data.
function text(value) {
  return String(value ?? '')
    .replace(/[\\`*_{}[\]()#!|<>]/g, '\\$&')
    // Keeps GitHub from turning URLs into links.
    .replace(/:\/\//g, ':&#47;&#47;');
}

function code(value) {
  return `\`${String(value ?? '').replace(/`/g, "'").replace(/\r?\n/g, ' ')}\``;
}

function cell(value) {
  return text(value).replace(/\r?\n/g, ' ');
}

function skipReason() {
  if (inputScanOutcome === 'failure') {
    return 'Not run: the secret scanner found findings in the analysis input, so nothing was sent to the LLM. '
      + '`secret-scan-input.json` in the artifact `e2e-secret-scans` lists the rule ids and locations.';
  }
  if (inputScanOutcome !== 'success') return 'Not run: the analysis input could not be prepared or scanned.';
  if (!hasAnthropicKey) return 'Not run: no `ANTHROPIC_API_KEY` is available to this build (for example, a pull request from a fork).';
  return 'Not run.';
}

function render({ analysis, meta, problems }) {
  const jdks = failures.legs.map((leg) => leg.jdk);
  const runUrl = process.env.GITHUB_RUN_ID
    ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
    : null;
  const lines = [];

  lines.push('# E2E failure analysis', '');
  if (process.env.GITHUB_SHA) {
    lines.push(`Commit \`${process.env.GITHUB_SHA.slice(0, 7)}\` on \`${process.env.GITHUB_REF_NAME}\`${runUrl ? `, [run ${process.env.GITHUB_RUN_ID}](${runUrl})` : ''}.`, '');
  }
  lines.push('> Generated by an LLM. The verdicts are advisory: check the evidence before acting on them.', '');
  if (manifest) {
    const redacted = Object.entries(manifest.totals.redactions).map(([category, count]) => `${count} ${category}`).join(', ') || 'none';
    lines.push('Sensitive values are pseudonymized, in this report and in everything the LLM could read '
      + `(placeholders such as \`<IBAN_1 len=15>\`). The LLM could read ${manifest.totals.files} files; `
      + `redacted: ${redacted}. \`manifest.json\` lists every file.`, '');
  }

  lines.push('## Results per JDK version', '');
  lines.push('| JDK | Passed | Failed | Flaky | Skipped |', '|---|---|---|---|---|');
  for (const leg of failures.legs) {
    const stats = leg.stats;
    lines.push(stats
      ? `| ${leg.jdk} | ${stats.expected} | ${stats.unexpected} | ${stats.flaky} | ${stats.skipped} |`
      : `| ${leg.jdk} | no E2E results | | | |`);
  }
  lines.push('');

  const allTests = [...failures.tests, ...failures.notAnalyzed];
  if (allTests.length > 0) {
    lines.push('## Failed and flaky tests per JDK version', '');
    lines.push(`| Id | Test | ${jdks.join(' | ')} |`, `|---|---|${jdks.map(() => '---').join('|')}|`);
    for (const test of allTests) {
      lines.push(`| ${test.id} | ${cell(test.title)} | ${jdks.map((jdk) => STATUS_LABELS[test.statusByJdk[jdk]] ?? test.statusByJdk[jdk]).join(' | ')} |`);
    }
    lines.push('');
  }

  if (outcome === 'skipped') {
    lines.push('## Analysis', '', skipReason(), '');
  } else if (!analysis || problems.length > 0) {
    lines.push('## Analysis', '', 'The analysis did not produce a valid result:', '', ...problems.map((problem) => `- ${text(problem)}`), '');
  }

  if (analysis?.findings) {
    const byId = new Map(analysis.findings.map((finding) => [finding.testId, finding]));
    const titles = new Map([
      ...failures.tests.map((test) => [test.id, test.title]),
      ...failures.legsWithoutResults.map((leg) => [`JDK${leg.jdk}`, `JDK ${leg.jdk}: no E2E results`])
    ]);

    lines.push('## Summary', '', text(analysis.summary), '');
    lines.push('| Id | Test | Verdict | Confidence |', '|---|---|---|---|');
    for (const id of expectedIds) {
      const finding = byId.get(id);
      lines.push(`| ${id} | ${cell(titles.get(id))} | ${finding ? cell(finding.verdict) : '**missing**'} | ${finding ? cell(finding.confidence) : ''} |`);
    }
    lines.push('');

    for (const id of expectedIds) {
      const finding = byId.get(id);
      if (!finding) continue;
      const test = failures.tests.find((candidate) => candidate.id === id);
      lines.push(`### ${id}: ${text(titles.get(id))}`, '');
      if (test) lines.push(`${code(`${test.file}:${test.line}`)} (${text(test.project)})`, '');
      lines.push(`**Verdict:** ${text(finding.verdict)} (confidence: ${text(finding.confidence)})`, '');
      lines.push(text(finding.explanation), '');
      lines.push(`**Across JDK versions:** ${text(finding.jdkDifference)}`, '');
      lines.push('**Evidence:**', '');
      for (const item of finding.evidence ?? []) {
        lines.push(`- ${code(item.location)}: ${text(item.detail)}`);
      }
      lines.push('', `**Suggested fix (not applied):** ${text(finding.suggestedFix)}`, '');
    }
  }

  if (failures.notAnalyzed.length > 0) {
    lines.push('## Not analyzed', '', `${failures.notAnalyzed.length} more failed or flaky test(s) exceeded the analysis limit; see the table above.`, '');
  }

  if (meta) {
    const parts = [
      meta.costUsd != null ? `cost $${meta.costUsd.toFixed(2)}` : null,
      meta.turns != null ? `${meta.turns} turns` : null,
      meta.durationMs != null ? `${Math.round(meta.durationMs / 1000)} s` : null
    ].filter(Boolean);
    if (parts.length > 0) lines.push('---', '', `Analysis: ${parts.join(', ')}.`, '');
  }

  return lines.join('\n');
}

const result = outcome === 'skipped' ? { problems: [] } : readClaudeOutput();
const report = render(result);

fs.writeFileSync(path.join(dir, 'report.md'), report);
if (result.analysis) {
  fs.writeFileSync(path.join(dir, 'findings.json'), JSON.stringify({ ...result.analysis, meta: result.meta ?? null }, null, 2));
}
console.log(`Report written to ${path.join(dir, 'report.md')}.`);

const missing = result.analysis?.findings
  ? expectedIds.filter((id) => !result.analysis.findings.some((finding) => finding.testId === id))
  : [];
if (outcome !== 'skipped' && (!result.analysis || result.problems.length > 0 || missing.length > 0)) {
  console.error(`::error::E2E failure analysis incomplete: ${[...result.problems, ...missing.map((id) => `${id}: no finding.`)].join(' ')}`);
  process.exit(1);
}
