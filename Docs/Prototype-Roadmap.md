# Prototype Roadmap — Deliverable 3

**Baseline:** `Docs/Updated Spec.docx` (the submitted spec). Every "spec says" below refers to it. Rubric: `Docs/Prototype-Rubric.md`.

Scope of the prototype: the MVP path only (deposit → bet advice → bet → deal → hit/stand → dealer plays → settle → record → repeat until broke or quit). Spec §7 put card counting in the MVP, so `CardCounter` is in scope, but its model has to be written down first (see Step 0, D6).

Keep a running **change log** from Step 0 onward: one line per deviation from the spec, with *why* and *origin* (group discussion / GenAI / own reflection). Step 6 turns it into the Reflection.

---

## Step 0 — Settle the spec's internal contradictions

Code can't be written until these are decided. Each decision is a change-log line.

| # | Contradiction in the spec | Recommended decision |
|---|---|---|
| D1 | Class table: `GameHistory` holds `LinkedChain<String>`; method table: `record(RoundResult)` / `List<RoundResult> all()`; a separate `Round` class also exists. | One record type: `RoundResult` (record: `int round, Outcome outcome, int bet, int netChange`). `GameHistory` holds `LinkedChain<RoundResult>`. `Round` is dropped; its round logic goes in `Game`. |
| D2 | `CardHistory` has methods and tests but no class-table row; the class diagram puts a `List<Card>` in `Game`. | Build `CardHistory` with `LinkedChain<Card> seen`, as the method table and tests say. The scope's "card-history tracker" needs it. |
| D3 | Spec says `LinkedChain.add(null)` throws `NullPointerException`; the starter throws `IllegalArgumentException`. | Leave `LinkedChain` unchanged. App classes (`Hand.add`, `GameHistory.record`, `CardHistory.record`) call `Objects.requireNonNull` first, so they throw NPE as the spec says. |
| D4 | Spec says `toArray` makes a new array if the given one is too small; the starter throws `IllegalArgumentException`. `all()` returns `List`. | `all()` calls `toArray(new X[chain.size()])` and wraps the result in `Arrays.asList`. The array is always the right size, so the IAE never fires. |
| D5 | `ConsoleUI` and `Game.run` tests need scripted input and captured output, but `ConsoleUI` lists only `Scanner in`. `Game` has no seeded `Random`. | `ConsoleUI(Scanner in, PrintStream out)`; `Game(ConsoleUI ui, Random rng)`. |
| D6 | Spec §7 puts card counting in the MVP, but the spec only names "Bayesian probability model" and "Kelly criterion" and never defines them. Field `int recommendedBet` vs. method `suggestedUnits` also disagree. | The model is now written down in `blackjack-expected-return-model.md` (rows 1–13 plus a worked example). `CardCounter` implements it: `observe(Card)`, `expectedReturn()` (row 10), `variance()` (row 12), `suggestedBet(int bankroll)` (row 13). The worked example's numbers (E = −0.0193, σ² = 0.974 for a fresh single deck) are the normal-case tests. |
| D7 | Spec's test plan tests `LinkedChain`; the rubric asks for tests of *app* classes only. | Drop `LinkedChain` tests (starter code, unchanged). |
| D8 | Public methods that `Game` alone calls each cost two tests. | Anything not called from outside its class is `private`. The public list is the one in Step 4. |

**Done when:** the team agrees on D1–D8 and the change log has a line for each.

---

## Step 1 — One-command build and test

Layout: `src/` (app + `LinkedChain.java`), `test/`, `lib/junit-platform-console-standalone-1.10.2.jar`. Java 17 (records allowed).

```
javac -d out -cp "lib/*" src/*.java test/*.java
java -jar lib/junit-platform-console-standalone-1.10.2.jar execute -cp out --scan-classpath
java -cp out Game
```

If the course requires Eclipse with its built-in JUnit, use that and write the Eclipse steps in the README instead.

**Done when:** a placeholder test runs green with the command above. The README "How to run" section gets these lines verbatim.
**Closes:** *JUnit — tests execute*; README "how to compile and run".

---

## Step 2 — Model and storage classes

| File | Holds a LinkedChain? | Notes |
|---|---|---|
| `Rank.java`, `Suit.java`, `Card.java` (record) | — | `Rank` carries point value; ace 11. |
| `Outcome.java`, `Action.java`, `RoundResult.java` (record) | — | D1. |
| `Shoe.java` | `LinkedChain<Card>` | Build in `ArrayList`, `Collections.shuffle(list, rng)`, `add` each to the chain. `deal()` = `cards.remove()`, throws `IllegalStateException("shoe is empty")` on `null`. `CUT_CARD = 32`. |
| `CardHistory.java` | `LinkedChain<Card>` | D2. |
| `GameHistory.java` | `LinkedChain<RoundResult>` | `wins/losses/winRate` iterate `all()`. Empty → 0, 0, 0.0. |
| `Hand.java` | — (`List<Card>`, per spec) | Ace 11→1 demotion in `total()`. |
| `Bankroll.java` | — | Exception messages exactly as spec §5. |
| `CardCounter.java` | `LinkedChain<Card> seen` (per spec) | Implements the D6 addendum. `observe` rejects null with NPE. Reset with the shoe. |

Rule for every class: touch the chain only through `add`, `remove()`, `size`, `toArray`. No new methods on `LinkedChain`; no game logic in it.

**Done when:** everything compiles; `grep -n "LinkedChain" src/*.java` shows only fields and public-method calls.
**Closes:** *Reused — two element types (`Card`, `RoundResult`) through one generic class*; *Public API — only methods / logic in app classes*; *Slice — LinkedChain does the storage*.

---

## Step 3 — `ConsoleUI` and `Game` (the MVP path)

`ConsoleUI`: `promptInt`, `promptAction`, `promptYesNo`, `show`, each re-prompting on bad text with the spec's messages.
`Game.run()`: deposit (catch `IllegalArgumentException` from `Bankroll`, show, re-prompt) → loop: `needsShuffle()` → new `Shoe`/`CardHistory`/`CardCounter` → show `suggestedUnits` advice, capped at the balance → bet (catch, re-prompt) → deal 2+2 (every dealt card goes `shoe.deal()` → `cardHistory.record` → `counter.observe`) → blackjack check → player hit/stand → dealer hits while `< 17` → `settle(player, dealer, bet)` (public so the payout table is testable without seeds) → `history.record` → `isBroke()` ends with "You are out of money." + history; else `promptYesNo`. `main` builds `new Game(new ConsoleUI(new Scanner(System.in), System.out), new Random())`.

**Done when:** `java -cp out Game` plays several rounds to quit and to broke without a stack trace.
**Closes:** *Slice — compiles, runs on the MVP path*.

---

## Step 4 — JUnit tests: one normal + one bad-input per public app method

Write them from spec §6, adjusted by D3/D4/D7. Seed `Random(42)` wherever a shoe is built.

| Test file | Public methods covered |
|---|---|
| `CardTest`, `RoundResultTest` | `Rank.value`, `Rank.label`, the `Card` and `RoundResult` null checks, both `toString`s |
| `ShoeTest` | constructor, `deal` (157th deal throws — *removing what isn't there*), `cardsRemaining`, `needsShuffle` |
| `HandTest` | `add`, `total`, `isBust`, `isBlackjack`, `toString` |
| `BankrollTest` | constructor, `placeBet`, `credit`, `balance`, `isBroke` |
| `GameHistoryTest` | `record`, `all`, `wins`, `losses`, `winRate` (empty history → 0/0/0.0 — *empty collection*) |
| `CardHistoryTest` | `record`, `all` (empty → empty list) |
| `CardCounterTest` | constructor, `observe` (null → NPE; a 5th ace from one deck → ISE), `expectedReturn` and `variance` (fresh single deck matches the worked example; 31 unseen → ISE), `suggestedBet` (losing deck → minimum; ten-rich deck → floor(E/σ² × bankroll); bankroll 0 → IAE) |
| `ConsoleUITest` | `promptInt`, `promptAction`, `promptYesNo`, `show` (input from a `String`, output to a `ByteArrayOutputStream`) |
| `GameTest` | constructor; `run`: seed 1 scripted round ends at $110; deposit `-50` and bet `500` each re-asked once; seed 2 goes broke with no play-again prompt. `settle(Hand, Hand)` → `Outcome` and `winnings(Outcome, int)` with hand-built hands: $5 blackjack returns $12; null hand → NPE; bet 0 → IAE. `main` (via `System.setIn`): scripted session; `abc` then `100` at the deposit prompt |

`Rank`'s point value is a `public final int` field, so it needs no tests. Record accessors and enum `values()` are compiler-generated and excluded.

**Done when:** the Step 1 command reports all green, and every hand-written method in `grep -n "public" src/*.java` (skip `LinkedChain.java`) is ticked against a normal and a bad-input test. Any test that is skipped or failing is listed for "What doesn't work yet".
**Closes:** *JUnit — normal + bad per method, tests pass*; *JUnit — Exceeds* (empty collection, removing what isn't there).

---

## Step 5 — Bad-input walkthrough against the built program

Run each case from the scope in `java -cp out Game` and confirm the response and the catching class match spec §5.

| Scope case | Caught in | Expected response |
|---|---|---|
| Deposit ≤ 0 | `Bankroll(int)` → `Game.run` | "Deposit must be greater than zero", re-prompt |
| Bet ≤ 0 | `Bankroll.placeBet` → `Game.run` | "Bet must be greater than zero", re-prompt |
| Bet > balance | `Bankroll.placeBet` → `Game.run` | message naming balance, re-prompt |
| Non-numeric number | `ConsoleUI.promptInt` | "Please enter a whole number.", re-prompt |
| Not hit/stand | `ConsoleUI.promptAction` | "Type hit or stand.", re-prompt |
| Hit with no cards left | `Game.run` via `needsShuffle()` | new shoe before the round; `Shoe.deal()` throws only if the cut-card maths is wrong |
| Balance reaches 0 | `Game.run` via `isBroke()` | "You are out of money." + history, session ends |

**Done when:** every row behaves as listed with no crash. A row that doesn't goes into "What doesn't work yet".
**Closes:** *Bad Input — no crash, caught where spec said*; *Bad Input — Exceeds* (named response per case).

---

## Step 6 — README.md

| Section | Content | Closes |
|---|---|---|
| How to run | The three commands from Step 1. | Submit requirement |
| Big-O | One line per public app method: built cost vs. spec estimate, match or not. | *Public API — Exceeds* (oral defence) |
| **Reflection** | Q1 spec changes: the change log (D1–D8 plus anything found while building), each with why and origin. Q2 LinkedChain: used unchanged; note D3/D4. Q3 unknowns: shuffling a chain → solved by shuffling an `ArrayList` before loading; card-counting maths → what the D6 addendum settled and what it still approximates. Q4 what doesn't work: failures from Steps 4–5, including `CardCounter` if it missed the deadline. | *Reflection — Meets + both Exceeds boxes* |
| **Next Steps** | Ordered list of remaining MVP fixes, each with an owner. Each remaining stretch goal (ASCII art, splitting, cheating dealer, multiplayer) marked committed or dropped. New tests named for each committed one, e.g. ASCII art → a `Hand` rendering method with a normal hand and an empty hand. | *Next Steps — Meets + both Exceeds boxes* |
| Attribution | One row per class **and** per test file (15 source files including `LinkedChain`, 10 test files): who, and how (wrote / revised / reviewed / tested / GenAI-assisted). GenAI note. | Rubric point 8 |

**Done when:** all six sections exist, every `.java` file under `src/` and `test/` has an attribution row, and every Next Steps item has a name.

---

## Coverage check

| Rubric checkbox | Step |
|---|---|
| App classes compile | 2, 3 |
| Runs on the MVP path | 3 |
| LinkedChain does the storage | 2 |
| ≥ 2 element types stored and retrieved | 2 (`Card`, `RoundResult`) |
| Same generic class, no type-specific copies | 2 |
| Only calls its methods | 2 |
| Logic in app classes | 2, 3 |
| Tests execute | 1 |
| Normal + bad-input per public app method | 4 (D8 sets the method list) |
| Tests pass | 4 |
| No crash on scope bad-input cases | 5 |
| Caught where spec said | 5 |
| Reflection section, changes + why, other questions | 0, 6 |
| Next Steps section, MVP remaining, other questions | 6 |
| Attribution table + GenAI note | 6 |
| Exceeds (included): empty collection, removing absent, named response per case, Big-O, reflection specifics + origins, ordered/owned next steps, stretch goals marked | 4, 5, 6 |
| Exceeds (opt-in): stretch goal working → ASCII card art, the smallest of the four remaining stretch goals | — |
| Exceeds (opt-in): > 2 element types → also keep the spec's `LinkedChain<String>` of round summary lines in `GameHistory` | — |
