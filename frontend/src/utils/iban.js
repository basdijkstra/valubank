// Structural IBAN format validation: 2-letter country code, 2 check digits,
// then an alphanumeric BBAN, 15-34 characters overall (the real-world IBAN
// length range). Deliberately does not verify the ISO 7064 mod-97 checksum -
// ValuBank's own seeded IBANs (e.g. NL01VALU0000000001) are fictional and
// wouldn't pass a real checksum, so checking it here would reject legitimate
// in-app payments.
const IBAN_PATTERN = /^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$/

export function isValidIban(rawIban) {
  return IBAN_PATTERN.test(normalize(rawIban))
}

function normalize(rawIban) {
  return (rawIban || '').replace(/\s+/g, '').toUpperCase()
}
