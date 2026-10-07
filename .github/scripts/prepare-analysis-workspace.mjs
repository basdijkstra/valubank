// Builds the directory the LLM analysis runs in, from an allowlist: only the files the
// analysis needs are copied, and every copied file is pseudonymized (redact.mjs).
// Claude runs inside this directory, outside the repository checkout, so it cannot
// reach anything that is not copied here (.git, traces, screenshots, config files...).
//
// Also writes, to <publishDir>:
// - failures.json: the redacted copy (the raw one is never published)
// - manifest.json: every file that went into the workspace, with its source, SHA-256,
//   size and redaction counts. The redaction mapping itself is never written anywhere.
//
// Usage: node prepare-analysis-workspace.mjs <rawFailuresJson> <repoRoot> <workspaceDir> <publishDir>
// See docs/llm-data-policy.md for why each kind of data is (not) included.

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { Redactor } from './redact.mjs';

const [failuresFile, repoRoot, workspaceDir, publishDir] = process.argv.slice(2);
if (!failuresFile || !repoRoot || !workspaceDir || !publishDir) {
  console.error('Usage: node prepare-analysis-workspace.mjs <rawFailuresJson> <repoRoot> <workspaceDir> <publishDir>');
  process.exit(2);
}

// Source code and documentation the analysis may read (decision: sending application
// code is acceptable). The spec files of the failing tests are added below.
const SOURCE_ALLOWLIST = [
  'CLAUDE.md',
  'README.md',
  'docs',
  'e2e/playwright.config.ts',
  'e2e/tests/pages',
  'e2e/tests/helpers',
  'frontend/src',
  ...fs.readdirSync(path.join(repoRoot, 'services')).map((service) => `services/${service}/src/main/java`)
];
const TEXT_EXTENSIONS = new Set(['.md', '.ts', '.js', '.jsx', '.tsx', '.java', '.css', '.html', '.json']);
// Documented in the manifest, so it is visible what was deliberately left out.
const NEVER_INCLUDED = [
  'Playwright traces, screenshots and videos (network data, cookies, localStorage)',
  'The Playwright HTML report and the full results.json',
  'Complete service logs (only time windows around failing attempts)',
  'Configuration files (src/main/resources, .env, settings)',
  'Git history and credentials (.git)'
];
// Log lines this long before and after a failing attempt are included.
const LOG_MARGIN_MS = 5000;
const MAX_LOG_LINES_PER_FILE = 2000;
const STARTUP_LOG_LINES = 200;
const LOG_TIMESTAMP = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2}))/;

const redactor = new Redactor();
const manifestFiles = [];
const excluded = [];

const toPosix = (p) => p.replace(/\\/g, '/');

function writeRedacted(workspacePath, content, source, kind) {
  const { text, counts } = redactor.redact(content, kind);
  const target = path.join(workspaceDir, workspacePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, text);
  manifestFiles.push({
    path: toPosix(workspacePath),
    source: toPosix(source),
    kind,
    bytes: Buffer.byteLength(text),
    sha256: crypto.createHash('sha256').update(text).digest('hex'),
    redactions: counts
  });
  return text;
}

function filesUnder(relativePath) {
  const absolute = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolute)) return [];
  if (fs.statSync(absolute).isFile()) return [relativePath];
  return fs.readdirSync(absolute, { recursive: true })
    .map((entry) => path.join(relativePath, entry))
    .filter((entry) => fs.statSync(path.join(repoRoot, entry)).isFile());
}

function copySource(relativePath) {
  for (const file of filesUnder(relativePath)) {
    if (!TEXT_EXTENSIONS.has(path.extname(file))) {
      excluded.push({ source: toPosix(file), reason: 'not a text source file' });
      continue;
    }
    writeRedacted(file, fs.readFileSync(path.join(repoRoot, file), 'utf8'), file, 'source');
  }
}

// ---------- log fragments ----------

function mergeWindows(windows) {
  const sorted = [...windows].sort((a, b) => a.from - b.from);
  const merged = [];
  for (const window of sorted) {
    const last = merged.at(-1);
    if (last && window.from <= last.to) last.to = Math.max(last.to, window.to);
    else merged.push({ ...window });
  }
  return merged;
}

function logFragment(logFile, windows) {
  const lines = fs.readFileSync(logFile, 'utf8').split(/\r?\n/);
  let current = null;
  let sawTimestamp = false;
  const stamped = lines.map((line) => {
    const match = line.match(LOG_TIMESTAMP);
    if (match) {
      current = Date.parse(match[1]);
      sawTimestamp = true;
    }
    // Lines without a timestamp (stack traces) belong to the line before them.
    return { line, time: current };
  });
  if (!sawTimestamp) return null;

  const output = [];
  for (const window of windows) {
    const selected = stamped.filter(({ time }) => time !== null && time >= window.from && time <= window.to);
    if (selected.length === 0) continue;
    output.push(`--- log lines from ${new Date(window.from).toISOString()} to ${new Date(window.to).toISOString()} ---`);
    output.push(...selected.map(({ line }) => line));
  }
  if (output.length > MAX_LOG_LINES_PER_FILE) {
    return [...output.slice(0, MAX_LOG_LINES_PER_FILE), `--- truncated after ${MAX_LOG_LINES_PER_FILE} lines ---`].join('\n');
  }
  return output.join('\n');
}

function logFiles(artifactDir) {
  const dir = path.join(artifactDir, 'logs');
  return fs.existsSync(dir) ? fs.readdirSync(dir).filter((name) => name.endsWith('.log')).map((name) => path.join(dir, name)) : [];
}

// ---------- build the workspace ----------

fs.rmSync(workspaceDir, { recursive: true, force: true });
fs.mkdirSync(workspaceDir, { recursive: true });

const failures = JSON.parse(fs.readFileSync(failuresFile, 'utf8'));
const artifactDirByJdk = new Map(failures.legs.map((leg) => [leg.jdk, leg.artifactDir]));
const workspaceDirFor = (jdk) => `ci-results/jdk${jdk}`;

for (const entry of SOURCE_ALLOWLIST) copySource(entry);
for (const specFile of new Set(failures.tests.map((test) => test.file))) copySource(specFile);

// Error contexts of the failing attempts, and the log time windows around those attempts.
const windowsByJdk = new Map();
for (const test of failures.tests) {
  for (const [jdk, failure] of Object.entries(test.failures)) {
    for (const attempt of failure.attempts) {
      attempt.errorContext = attempt.errorContext.map((artifactPath) => {
        const relative = toPosix(artifactPath).split('/e2e/test-results/')[1];
        const workspacePath = `${workspaceDirFor(jdk)}/test-results/${relative}`;
        writeRedacted(workspacePath, fs.readFileSync(artifactPath, 'utf8'), artifactPath, 'data');
        return workspacePath;
      });
      if (attempt.startTime) {
        const from = Date.parse(attempt.startTime) - LOG_MARGIN_MS;
        const to = Date.parse(attempt.startTime) + (attempt.durationMs ?? 0) + LOG_MARGIN_MS;
        windowsByJdk.set(jdk, [...(windowsByJdk.get(jdk) ?? []), { from, to }]);
      }
    }
  }
}

for (const leg of failures.legs) {
  leg.artifactDir = workspaceDirFor(leg.jdk);
  leg.logFragments = [];
  const windows = mergeWindows(windowsByJdk.get(leg.jdk) ?? []);
  if (windows.length === 0) continue;
  for (const logFile of logFiles(artifactDirByJdk.get(leg.jdk))) {
    const fragment = logFragment(logFile, windows);
    if (fragment === null) {
      excluded.push({ source: toPosix(logFile), reason: 'log has no timestamps, so no time window can be cut out' });
      continue;
    }
    if (fragment === '') continue;
    const workspacePath = `${workspaceDirFor(leg.jdk)}/logs/${path.basename(logFile)}`;
    writeRedacted(workspacePath, fragment, logFile, 'data');
    leg.logFragments.push(workspacePath);
  }
}

// JDK versions without E2E results: the end of each log usually shows why the services did not start.
for (const leg of failures.legsWithoutResults) {
  leg.logs = leg.logs.map((logFile) => {
    const tail = fs.readFileSync(logFile, 'utf8').split(/\r?\n/).slice(-STARTUP_LOG_LINES).join('\n');
    const workspacePath = `${workspaceDirFor(leg.jdk)}/logs/${path.basename(logFile)}`;
    writeRedacted(workspacePath, `--- last ${STARTUP_LOG_LINES} lines ---\n${tail}`, logFile, 'data');
    return workspacePath;
  });
}

const redactedFailures = writeRedacted('failures.json', JSON.stringify(failures, null, 2), failuresFile, 'data');

// ---------- manifest ----------

const totals = { files: manifestFiles.length, bytes: 0, redactions: {} };
for (const file of manifestFiles) {
  totals.bytes += file.bytes;
  for (const [category, count] of Object.entries(file.redactions)) {
    totals.redactions[category] = (totals.redactions[category] ?? 0) + count;
  }
}
const manifest = {
  generatedAt: new Date().toISOString(),
  description: 'Every file the LLM analysis could read, after redaction. Placeholders like <IBAN_1 len=15> replace '
    + 'sensitive values consistently within this run; the mapping to the original values is not stored.',
  totals,
  neverIncluded: NEVER_INCLUDED,
  excluded,
  files: manifestFiles.sort((a, b) => a.path.localeCompare(b.path))
};

fs.mkdirSync(publishDir, { recursive: true });
fs.writeFileSync(path.join(publishDir, 'failures.json'), redactedFailures);
fs.writeFileSync(path.join(publishDir, 'manifest.json'), JSON.stringify(manifest, null, 2));

const summary = Object.entries(totals.redactions).map(([category, count]) => `${count} ${category}`).join(', ') || 'nothing';
console.log(`Analysis workspace: ${totals.files} files, ${totals.bytes} bytes. Redacted: ${summary}.`);
