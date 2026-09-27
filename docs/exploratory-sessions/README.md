# Exploratory session reports

Session reports produced by the `exploratory-tester` subagent land here, one
file per session: `<YYYY-MM-DD-HHmm>-<slug>.md`.

Charters that drive these sessions live in `docs/exploratory-charters/` -
see `TEMPLATE.md` there for the format, and `multi-currency-payments.md` for
a worked example.

To run a session: invoke the `exploratory-tester` subagent with a charter
(inline, or a path to one of the files in `docs/exploratory-charters/`) as
the prompt. The agent explores the *running* app - start it first
(`scripts/start-all.ps1` / `start-all.sh`) - forms its own test ideas within
the charter's scope, and writes its report here when done.
