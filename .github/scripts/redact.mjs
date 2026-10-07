// Pseudonymizes sensitive values before files are handed to an LLM.
//
// One Redactor is used for a whole analysis run, so the same value gets the same
// placeholder in every file (a test, its error context and a log line stay connected).
// The mapping back to the original values only lives in memory: it is never written
// to disk or published. What is published are the counts per category.
//
// What is (not) redacted follows the classification in docs/llm-data-policy.md:
// - Seeded accounts and logins are synthetic and stay visible (SEEDED_IBANS, SEEDED_SECRETS).
// - Every other IBAN-shaped value, email address, token, key and credential is replaced.

// Synthetic seed data (accounts-service DataSeeder). Compared after normalizing.
export const SEEDED_IBANS = new Set([
  'NL01VALU0000000001',
  'NL01VALU0000000002',
  'NL01VALU0000000003',
  'NL01VALU0000000004'
]);
export const SEEDED_SECRETS = new Set(['password123', 'admin123']);

// Same structural check as the application (frontend/src/utils/iban.js).
const IBAN_SHAPE = /^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$/;
// Candidates in free text: written as one word or in groups of four. The boundaries make
// sure a longer string is never partly redacted (no "<IBAN_1 len=34>K").
const IBAN_CANDIDATE = /(?<![A-Za-z0-9])[A-Za-z]{2}[0-9]{2}(?: ?[A-Za-z0-9]{4}){2,7}(?: ?[A-Za-z0-9]{1,3})?(?![A-Za-z0-9])/g;
// Real IBANs contain digits after the check digits; this keeps words like "AB12 RESTMORETEXT" out.
const MIN_BBAN_DIGITS = 6;

const EMAIL = /(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?![A-Za-z0-9-])/g;
const JWT = /\beyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}/g;
const API_KEY = /\b(?:sk-ant-[A-Za-z0-9_-]{10,}|gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[A-Z0-9]{16})\b/g;
const BEARER = /\b(Bearer\s+)([A-Za-z0-9._~+/-]{8,}=*)/g;
const AUTH_HEADER = /\b((?:Authorization|Proxy-Authorization|Cookie|Set-Cookie)["']?\s*[:=]\s*["']?)([^"'\r\n]+)/gi;
const URL_CREDENTIALS = /\b([a-z][a-z0-9+.-]*:\/\/)([^\s/:@]+):([^\s/@]+)@/gi;
// key=value / "key": "value" pairs. Data files only: in source code this would hit
// expressions like `password = request.getPassword()`.
const SECRET_PAIR = /\b((?:password|passwd|pwd|secret|api[_-]?key|access[_-]?token|token)["']?\s*[:=]\s*["']?)([^"'\s,;}&]+)/gi;

export class Redactor {
  #placeholders = new Map();
  #counters = new Map();

  #placeholder(category, original, describe = () => '') {
    const key = `${category}:${original}`;
    if (!this.#placeholders.has(key)) {
      const next = (this.#counters.get(category) ?? 0) + 1;
      this.#counters.set(category, next);
      this.#placeholders.set(key, `<${category}_${next}${describe()}>`);
    }
    return this.#placeholders.get(key);
  }

  /**
   * @param {string} text
   * @param {'data' | 'source'} kind - data: logs, error contexts, test results; source: code and docs
   * @returns {{ text: string, counts: Record<string, number> }}
   */
  redact(text, kind) {
    const counts = {};
    const count = (category) => { counts[category] = (counts[category] ?? 0) + 1; };

    let result = text
      .replace(JWT, (match) => { count('JWT'); return this.#placeholder('JWT', match); })
      .replace(API_KEY, (match) => { count('API_KEY'); return this.#placeholder('API_KEY', match); })
      .replace(BEARER, (_, prefix, token) => { count('TOKEN'); return prefix + this.#placeholder('TOKEN', token); })
      .replace(AUTH_HEADER, (match, prefix, value) => {
        if (value.startsWith('Bearer <TOKEN_')) return match;
        count('SECRET');
        return prefix + this.#placeholder('SECRET', value.trim());
      })
      .replace(URL_CREDENTIALS, (_, scheme, user, password) => {
        count('CREDENTIALS');
        return `${scheme}${this.#placeholder('USER', user)}:${this.#placeholder('SECRET', password)}@`;
      });

    if (kind === 'data') {
      result = result.replace(SECRET_PAIR, (match, prefix, value) => {
        if (SEEDED_SECRETS.has(value) || value.startsWith('<')) return match;
        count('SECRET');
        return prefix + this.#placeholder('SECRET', value);
      });
    }

    result = result
      .replace(EMAIL, (match) => { count('EMAIL'); return this.#placeholder('EMAIL', match.toLowerCase()); })
      .replace(IBAN_CANDIDATE, (match) => {
        const iban = match.replace(/ /g, '').toUpperCase();
        if (!IBAN_SHAPE.test(iban) || SEEDED_IBANS.has(iban)) return match;
        if ((iban.slice(4).match(/[0-9]/g) ?? []).length < MIN_BBAN_DIGITS) return match;
        count('IBAN');
        return this.#placeholder('IBAN', iban, () => ` len=${iban.length}`);
      });

    return { text: result, counts };
  }
}
