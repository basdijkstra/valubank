// Tests for the redaction that runs before anything is sent to the LLM.
// Run: node --test .github/scripts/redact.test.mjs
//
// The fake secrets are assembled at runtime, so this file itself contains nothing
// that a secret scanner (or GitHub push protection) would flag.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Redactor } from './redact.mjs';

const fake = (...parts) => parts.join('');
const redactData = (text, redactor = new Redactor()) => redactor.redact(text, 'data');
const redactSource = (text, redactor = new Redactor()) => redactor.redact(text, 'source');

// ---------- IBANs ----------

test('seeded IBANs stay visible, also written with spaces or in lowercase', () => {
  const text = "fill('NL01VALU0000000003'); fill('nl01 valu 0000 0000 03'); fill('  NL01 VALU 0000 0000 01  ')";
  assert.deepEqual(redactSource(text), { text, counts: {} });
});

test('other IBANs get a placeholder with their length', () => {
  const { text, counts } = redactSource("reviewPayment('NL01ABCD0000001', 'Bob')");
  assert.equal(text, "reviewPayment('<IBAN_1 len=15>', 'Bob')");
  assert.deepEqual(counts, { IBAN: 1 });
});

test('the same IBAN gets the same placeholder in every file of a run', () => {
  const redactor = new Redactor();
  const source = redactSource("const target = 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJ';", redactor).text;
  const data = redactData('Expected: "nl01 abcd efgh ij01 2345 6789 abcd efgh ij"', redactor).text;
  assert.match(source, /<IBAN_1 len=34>/);
  assert.match(data, /<IBAN_1 len=34>/);
});

test('a string longer than an IBAN is left alone, not partly redacted', () => {
  const text = "iban: 'NL01ABCDEFGHIJ0123456789ABCDEFGHIJK'";
  assert.equal(redactSource(text).text, text);
});

test('strings that are not IBAN-shaped are left alone', () => {
  for (const text of ['1L01VALU0000000003', 'NLA1VALU0000000003', 'NL01VALU-000000003', 'NL01ABCD000001', 'AB12 REST MORE TEXT HERE']) {
    assert.equal(redactSource(text).text, text, text);
  }
});

test('an IBAN inside an error message is redacted', () => {
  const { text } = redactData('Please enter a valid IBAN (e.g. NL91ABNA0417164300).');
  assert.equal(text, 'Please enter a valid IBAN (e.g. <IBAN_1 len=18>).');
});

// ---------- secrets and personal data ----------

test('email addresses are redacted', () => {
  assert.equal(redactData('mail sent to Jane.Doe@example.com').text, 'mail sent to <EMAIL_1>');
});

test('JWTs, bearer tokens and API keys are redacted', () => {
  const jwt = fake('eyJ', 'hbGciOiJIUzI1NiJ9', '.', 'eyJzdWIiOiIxMjM0NTY3ODkwIn0', '.', 'dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U');
  const anthropicKey = fake('sk-', 'ant-', 'api03-', 'A'.repeat(40));
  const githubToken = fake('gh', 'p_', 'B'.repeat(36));
  const { text, counts } = redactData(`token ${jwt}\nAuthorization: Bearer abcdef0123456789\nkey ${anthropicKey} ${githubToken}`);
  assert.doesNotMatch(text, /eyJ|abcdef0123456789|sk-ant-|ghp_/);
  assert.equal(counts.JWT, 1);
  assert.equal(counts.TOKEN, 1);
  assert.equal(counts.API_KEY, 2);
});

test('cookie headers and credentials in URLs are redacted', () => {
  const { text } = redactData('Cookie: JSESSIONID=1234abcd\nGET https://deploy:s3cr3t-pass@internal.example/api');
  assert.equal(text, 'Cookie: <SECRET_1>\nGET https://<USER_1>:<SECRET_2>@internal.example/api');
});

test('password values are redacted in data files, except the seeded logins', () => {
  const { text } = redactData('{"username":"carol","password":"Winter2026!"} textbox "Password": password123');
  assert.equal(text, '{"username":"carol","password":"<SECRET_1>"} textbox "Password": password123');
});

test('password-like code in source files is not redacted', () => {
  const text = 'String password = request.getPassword();';
  assert.equal(redactSource(text).text, text);
});

// ---------- the mapping never leaves the redactor ----------

test('the result contains placeholders and counts only, not the original values', () => {
  const result = redactData('pay NL01VALU0000000099 and mail bob@example.com');
  assert.doesNotMatch(JSON.stringify(result), /NL01VALU0000000099|bob@example\.com/);
});
