export function formatBalance(balance, currency) {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(balance)
  } catch {
    return `${balance} ${currency}`
  }
}
