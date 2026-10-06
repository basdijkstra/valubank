// Collects failed and flaky Playwright tests from the E2E results of every JDK version
// (artifacts e2e-results-jdk<N>, downloaded into one directory) and writes them to
// <outDir>/failures.json: the input for the Claude analysis and the report.
//
// Usage: node collect-e2e-failures.mjs <artifactsDir> <outDir>
// Sets the step output needs_analysis=true when there is anything to analyze.

import fs from 'node:fs';
import path from 'node:path';

const [artifactsDir = 'artifacts', outDir = 'failure-analysis'] = process.argv.slice(2);

// Tests beyond this number are listed in the report, but not analyzed in detail.
const MAX_ANALYZED_TESTS = 15;
const MAX_ERROR_LENGTH = 1500;
const LEG_PREFIX = 'e2e-results-jdk';
const RESULTS_FILE = path.join('e2e', 'results', 'results.json');

function readLegs() {
  if (!fs.existsSync(artifactsDir)) {
    return [];
  }
  return fs.readdirSync(artifactsDir)
    .filter((name) => name.startsWith(LEG_PREFIX))
    .map((name) => {
      const dir = path.join(artifactsDir, name);
      const resultsPath = path.join(dir, RESULTS_FILE);
      const report = fs.existsSync(resultsPath) ? JSON.parse(fs.readFileSync(resultsPath, 'utf8')) : null;
      return { jdk: name.slice(LEG_PREFIX.length), dir, report };
    })
    .sort((a, b) => Number(a.jdk) - Number(b.jdk));
}

function* specsOf(suite, titlePath) {
  for (const spec of suite.specs ?? []) {
    yield { spec, titlePath: [...titlePath, spec.title] };
  }
  for (const child of suite.suites ?? []) {
    yield* specsOf(child, [...titlePath, child.title]);
  }
}

function stripAnsi(text) {
  return text.replace(/\u001b\[[0-9;]*m/g, '');
}

function truncate(text) {
  return text.length > MAX_ERROR_LENGTH ? `${text.slice(0, MAX_ERROR_LENGTH)}\n[truncated]` : text;
}

// Attachment paths are absolute paths on the runner that ran the tests; map them into the artifact.
function artifactPath(legDir, attachmentPath) {
  const normalised = attachmentPath.replace(/\\/g, '/');
  const index = normalised.indexOf('/e2e/test-results/');
  if (index === -1) {
    return null;
  }
  const local = path.join(legDir, normalised.slice(index + 1));
  return fs.existsSync(local) ? local.replace(/\\/g, '/') : null;
}

function failureDetails(leg, test) {
  return {
    status: test.status,
    attempts: (test.results ?? []).map((result) => ({
      retry: result.retry,
      status: result.status,
      durationMs: result.duration,
      error: (result.errors ?? []).length > 0
        ? truncate(stripAnsi(result.errors.map((error) => error.message ?? error.value ?? '').join('\n')))
        : null,
      errorContext: (result.attachments ?? [])
        .filter((attachment) => attachment.name === 'error-context' && attachment.path)
        .map((attachment) => artifactPath(leg.dir, attachment.path))
        .filter(Boolean)
    }))
  };
}

const legs = readLegs();
const tests = new Map();

for (const leg of legs.filter((leg) => leg.report)) {
  for (const fileSuite of leg.report.suites ?? []) {
    for (const { spec, titlePath } of specsOf(fileSuite, [])) {
      for (const test of spec.tests ?? []) {
        const key = `${spec.file}::${test.projectName}::${titlePath.join(' › ')}`;
        if (!tests.has(key)) {
          tests.set(key, {
            title: titlePath.join(' › '),
            file: `e2e/tests/${spec.file}`,
            line: spec.line,
            project: test.projectName,
            statusByJdk: {},
            failures: {}
          });
        }
        const entry = tests.get(key);
        entry.statusByJdk[leg.jdk] = test.status;
        if (test.status === 'unexpected' || test.status === 'flaky') {
          entry.failures[leg.jdk] = failureDetails(leg, test);
        }
      }
    }
  }
}

// Tests missing from a leg didn't run there (for example: the run was interrupted).
for (const entry of tests.values()) {
  for (const leg of legs) {
    entry.statusByJdk[leg.jdk] ??= 'not run';
  }
}

const problemTests = [...tests.values()]
  .filter((entry) => Object.keys(entry.failures).length > 0)
  // Failures on every JDK first, then the ones that differ between JDKs.
  .sort((a, b) => Object.keys(b.failures).length - Object.keys(a.failures).length || a.file.localeCompare(b.file) || a.line - b.line)
  .map((entry, index) => ({ id: `T${index + 1}`, ...entry }));

const analyzed = problemTests.slice(0, MAX_ANALYZED_TESTS);
const notAnalyzed = problemTests.slice(MAX_ANALYZED_TESTS)
  .map(({ id, title, file, line, statusByJdk }) => ({ id, title, file, line, statusByJdk }));

const legsWithoutResults = legs.filter((leg) => !leg.report).map((leg) => ({
  jdk: leg.jdk,
  logs: fs.existsSync(path.join(leg.dir, 'logs'))
    ? fs.readdirSync(path.join(leg.dir, 'logs')).map((name) => `${leg.dir}/logs/${name}`.replace(/\\/g, '/'))
    : []
}));

const failures = {
  generatedAt: new Date().toISOString(),
  legs: legs.map((leg) => ({
    jdk: leg.jdk,
    artifactDir: leg.dir.replace(/\\/g, '/'),
    hasResults: Boolean(leg.report),
    stats: leg.report?.stats ?? null
  })),
  legsWithoutResults,
  tests: analyzed,
  notAnalyzed
};

fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'failures.json'), JSON.stringify(failures, null, 2));

const needsAnalysis = problemTests.length > 0 || legsWithoutResults.length > 0;
const message = legs.length === 0
  ? 'No E2E results found for any JDK version.'
  : needsAnalysis
    ? `${problemTests.length} failed or flaky E2E test(s), ${legsWithoutResults.length} JDK version(s) without E2E results.`
    : `No failed or flaky E2E tests on JDK ${legs.map((leg) => leg.jdk).join(', ')}.`;

console.log(message);
if (process.env.GITHUB_OUTPUT) {
  fs.appendFileSync(process.env.GITHUB_OUTPUT, `needs_analysis=${needsAnalysis}\n`);
}
if (process.env.GITHUB_STEP_SUMMARY && !needsAnalysis) {
  fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `## E2E failure analysis\n\n${message}\n`);
}
