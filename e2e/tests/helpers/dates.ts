/**
 * Local date, offset by a number of days, as YYYY-MM-DD: the format of the
 * date input and of the Payments Service API. "Today" is the local date of the
 * machine running the tests, which is also the machine running the services.
 */
export function isoDate(offsetDays: number): string {
    const date = new Date();
    date.setDate(date.getDate() + offsetDays);
    return toIsoDate(date);
}

/**
 * A YYYY-MM-DD date as the frontend displays it (formatDate in
 * ScheduledPayments.jsx). Matches only when the browser runs with the
 * 'en-US' locale.
 */
export function displayDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('en-US');
}

/** The next 29 February that is still in the future, as YYYY-MM-DD. */
export function nextLeapDay(): string {
    const today = new Date();
    let year = today.getFullYear();
    while (!isLeapYear(year) || new Date(year, 1, 29) <= today) {
        year++;
    }
    return `${year}-02-29`;
}

function isLeapYear(year: number): boolean {
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function toIsoDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}
