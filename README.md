# Blackjack on a LinkedChain

Virginia Tech CS 2114 Project 2, Deliverable 3 — Prototype + Tests.

A console Blackjack game: deposit whole dollars, place a bet, hit or stand, and play against a dealer who stands on all 17s. Wins pay 1:1, natural blackjack pays 3:2 (fractional dollars truncated), ties return the bet, and losses forfeit it. The session ends on quit, exhausted input, or a zero balance. Three decks form the shoe. Dealt cards and completed rounds can be reviewed in the final history. The existing card counter offers expected-return and approximate Kelly betting advice before each round.

The submitted D2 baseline is [Updated Spec.docx](Docs/Updated%20Spec.docx). Earlier versions are `Docs/Spec.docx` and `Docs/Spec.md`; they describe a different Hi-Lo counter and contain starter-API contradictions. The rubric is [Prototype-Rubric.md](Docs/Prototype-Rubric.md). Specifications and diagrams remain unchanged as historical design documents; the deviations below describe the implementation.

## How to Compile and Run

Use **JDK 17** and run commands from the repository root. No Maven, Gradle, downloads, or additional libraries are needed. JUnit Jupiter 5 is supplied by `lib/junit-platform-console-standalone-1.10.2.jar`.

Compile the application and run it:

```sh
javac --release 17 -d out src/*.java
java -cp out Game
```

Compile the application and all JUnit tests, then execute the complete suite:

```sh
javac --release 17 -d out -cp "lib/*" src/*.java test/*.java
java -jar lib/junit-platform-console-standalone-1.10.2.jar execute -cp out --scan-classpath --fail-if-no-tests --details=summary
```

Check `java -version` and `javac -version` both report 17. `--release 17` also prevents accidentally using newer Java APIs. `out/` is ignored by Git. The commands above work in a POSIX shell or Windows cmd; in PowerShell, use cmd or enumerate the Java filenames if wildcard expansion is unsupported by your javac launcher.

In Eclipse, select JavaSE-17, add `src` and `test` as source folders, and add the supplied JUnit jar to the build path. Run `Game` as a Java application or the `test` folder as JUnit tests. Eclipse execution has not been independently verified in this session.

Enter a positive deposit and bet, then `hit`/`h` or `stand`/`s`, followed by `y`/`n` to continue or quit. Natural blackjacks and a player total of 21 finish without an action prompt. A dealer natural also settles immediately. Closing input during the player's turn cancels that unfinished round and returns its bet; it is not added to round history.

## Storage and Architecture

| Class | Responsibility | Storage |
|---|---|---|
| `Card`, `Rank`, `Suit` | Immutable card identity, rank points, suit | Record and enums |
| `Action`, `Outcome`, `RoundResult` | Choices and completed-round data | Enums and record |
| `Shoe` | Build, shuffle, and deal cards; cut card at 32 | `LinkedChain<Card>` |
| `Hand` | Hold and score a hand, including multiple aces | `List<Card>`, as specified in D2 |
| `Bankroll` | Validate deposits, bets and credits; hold balance | `int` whole dollars |
| `CardHistory` | Record and retrieve dealt cards | `LinkedChain<Card>` |
| `GameHistory` | Record/retrieve rounds; compute wins/losses/rate | `LinkedChain<RoundResult>` |
| `CardCounter` | Record observations, calculate return/variance/bet | `LinkedChain<Card>` plus calculation caches |
| `ConsoleUI` | Parse keyboard input and print messages | Injected `Scanner` and `PrintStream` |
| `Game` | Run rounds, dealer turns, payouts, and session history | Owns the above objects |

The same unbounded `LinkedChain<T>` stores **two element types**, `Card` and `RoundResult`. `Shoe.deal()` retrieves cards via `remove()`; both histories retrieve entries via `toArray(new ElementType[size()])`. Duplicate cards and duplicate results are retained. `ShoeTest`, `CardHistoryTest`, `GameHistoryTest`, and `LinkedChainTest.sameGenericClassStoresAndRetrievesRoundResults` demonstrate actual storage and retrieval. No third type or duplicate chain implementation was added for rubric points. D2’s generic-contract paragraph also mentions `Scanner` entries; the actual scope needs cards and round history, so scanners remain UI dependencies rather than stored entries.

Application classes call only public chain methods (`add`, `remove()`, `size`, `isEmpty`, `toArray`). They never access nodes or internal state. Shuffling, scoring, counting and payouts stay outside `LinkedChain`. Temporary lists for shuffling and hands preserve the existing architecture; the shoe and histories use chain storage.

## Validation and Public-Method Test Audit

| Input or boundary | Validation owner and response | Test evidence |
|---|---|---|
| Nonpositive deposit | `Bankroll(int)` throws `IllegalArgumentException`; `Game` prints message and re-prompts | `BankrollTest.constructorRejectsZeroAndNegativeDeposit`, `GameTest.runRepromptsBadDepositAndBadBet` |
| Nonpositive bet or insufficient funds | `Bankroll.placeBet` throws `IllegalArgumentException` without deduction; `Game` re-prompts | `placeBetRejectsBadAmounts`, `runRepromptsZeroAndNegativeBets`, `runRepromptsBadDepositAndBadBet` |
| Unrepresentable payout/balance | `Bankroll.placeBet` checks maximum possible payout before deduction; `credit` and `Game.winnings` reject overflow with `IllegalArgumentException` | `placeBetRejectsUnrepresentablePayoutBeforeDeduction`, `creditRejectsOverflowWithoutChangingBalance`, `winningsRejectsNegativeAndOverflowingBets`, `runRejectsBetThatCouldOverflowAndAllowsRetry` |
| Letters, blank, decimal, out-of-int-range text | `ConsoleUI.promptInt` prints whole-number message and re-prompts | `promptIntRepromptsUntilNumber`, `promptIntRejectsIntegerOverflow` |
| Invalid hit/stand or yes/no | Corresponding `ConsoleUI` method prints its correction and re-prompts | `promptActionRepromptsUntilHitOrStand`, `promptYesNoRepromptsUntilAnswer` |
| End of input | Prompts throw an `IllegalStateException` subtype; `Game.run` handles EOF specifically, refunds an unfinished bet, and exits | `allPromptsReportEndOfInput`, `mainExitsCleanlyWhenInputEnds`, `runRefundsUnfinishedRoundOnInputEnd` |
| Shoe exhaustion | `Game` replaces shoe and card trackers below 32 cards before dealing; direct empty `Shoe.deal` throws `IllegalStateException` | `needsShuffleTurnsTrueBelowCutCard`, `dealFromEmptyShoeThrows`, `runReshufflesAndResetsCardTrackersButKeepsRoundHistory` |
| Balance zero | `Game` prints out-of-money message and history without another round prompt | `runStopsWhenBroke` |
| Null card/result/hand/dependency | Existing application boundary checks throw `NullPointerException` before mutation | Constructor and `*RejectsNull*` tests |
| Null display/prompt text | Existing `PrintStream` convention displays `null`; null streams still rejected | `nullTextUsesPrintStreamConvention`, `constructorRejectsNull` |
| Empty histories or hands | Empty lists, zero totals/counts/rate; no division by zero | Existing history and hand tests |
| Removing nonexistent entry | Starter `remove(T)` returns false without mutation; empty `remove()` returns null | `LinkedChainTest.removeValueRemovesOnlyOneAndPreservesAbsentEntries`, `removeHeadAndRemoveEmpty` |

Every declared public application method was inspected. The following maps normal and bad-input/relevant-edge coverage; no-argument queries use an edge state rather than an invented invalid argument.

| Class / public methods | Normal coverage | Bad-input / edge coverage |
|---|---|---|
| `Card` constructor, `rank`, `suit`, `equals`, `hashCode`, `toString` | `CardTest.constructorKeepsRankAndSuit`, `toStringIsLabelPlusSuitLetter`, `recordAccessorsAndEqualityCoverOtherValuesAndNull` | `constructorRejectsNull`, `toStringOfTenHasThreeCharacters`, null/unequal records and low-rank accessors |
| `Rank.value`, `label` | Ace/ten tests in `CardTest` | Face-card, single-letter and lowest-rank tests |
| `RoundResult` constructor, all four accessors, `equals`, `hashCode`, `toString` | `constructorKeepsParts`, `toStringShowsWinWithPlus`, equal-record checks | `constructorRejectsNullOutcome`, `toStringShowsLossWithMinus`, loss/minimum-bet accessors and null/unequal comparisons |
| `Shoe` constructor, `deal`, `cardsRemaining`, `needsShuffle` | Three-deck composition, deal/shrink, counts and no-shuffle tests | Bad decks/null RNG, exhaustion, zero remaining, exact 32/31 boundary |
| `Hand` constructor, `add`, `total`, `isBust`, `isBlackjack`, `toString` | Add, soft total, bust, natural and formatted-hand tests | Null add, empty hand, exactly 21, three-card 21, multiple-ace demotions |
| `Bankroll` constructor, `placeBet`, `credit`, `balance`, `isBroke` | Initial deposit, subtraction, payout, updated balance, positive balance | Zero/negative/oversized amounts, full-balance bet, broke state, overflow |
| `GameHistory` constructor, `record`, `all`, `wins`, `losses`, `winRate` | Store/retrieve, duplicates, all outcomes and 2/5 win rate | Null record, empty history/list/counts/rate |
| `CardHistory` constructor, `record`, `all` | Store/retrieve card, duplicates/order | Null record and empty history |
| `CardCounter` constructor, `observe`, `expectedReturn`, `variance`, `suggestedBet` | Existing constructor, changed return, worked example and rich-deck bet tests | Bad deck count, null/impossible observation, 31-card cutoff, zero bankroll, one-dollar cap |
| `ConsoleUI` constructor, `promptInt`, `promptAction`, `promptYesNo`, `show` | Existing stream, number, hit, yes and printed-line tests | Null streams, bad text/re-prompting, no/stand, empty/null message, EOF, integer parsing overflow |
| `Game` constructor, `run`, `settle`, `winnings`, `main` | Existing construction, scripted win, full outcome/payout tables, main session | Null arguments, bad bets/deposit, broke, EOF/refund, natural ties, odd payout, bust, overflow and reset integration |

`Action`, `Outcome`, and `Suit` declare no methods. Compiler-generated enum methods and inherited `Object` methods are not custom application behavior. Record accessors/equality/hash behavior is nevertheless exercised. Hash codes are checked for consistency for equal records; unequal records are allowed to share a hash. `RoundResult` retains D2's integer data fields without adding numeric validation beyond its specified null-outcome check; `Game` produces valid round numbers, bets and net changes.

## Reflection

### What changed from Deliverable 2 and why

The existing prototype already resolved several contradictions in the submitted specification. These decisions are retained, rather than introducing another architecture:

| D2 design | Existing implementation retained | Reason / recorded origin |
|---|---|---|
| Class table has `Round` and `GameHistory` storing strings; method table uses `RoundResult` | `RoundResult` record, `LinkedChain<RoundResult>`, private `Game.playRound()` | Typed outcomes support statistics without parsing strings; prior GenAI draft following method table and roadmap D1 |
| `CardHistory` omitted from class table but present in methods/test plan | Separate `CardHistory` class with `LinkedChain<Card>` | Implements explicit history contract; prior GenAI draft / roadmap D2 |
| Counter API `suggestedUnits(int cardsRemaining)`, undefined Bayesian cost | `expectedReturn()`, `variance()`, `suggestedBet(int bankroll)` using existing model document | Bankroll is needed to size a Kelly fraction; remaining cards derive from observations. Prior implementation followed roadmap D6. The mathematical model's human authorship is not independently verified here |
| UI lists only `Scanner`; no injectable shuffle source on `Game` | `ConsoleUI(Scanner, PrintStream)`, `Game(ConsoleUI, Random)` | Enables deterministic tests and captured output; prior GenAI draft / roadmap D5 |
| Settlement inside `run` | Public static `settle(Hand, Hand)` and `winnings(Outcome, int)` | Directly tests outcomes and odd-dollar payouts; prior GenAI draft |
| `LinkedChain.add(null)` promised NPE; `toArray` promised automatic growth | Starter throws IAE for null/undersized arrays; app classes reject null with NPE and pass exact-size arrays | Preserve provided chain and application contracts; prior draft / roadmap D3–D4 |
| Unicode card symbols; RNG and hands listed as fields | ASCII suits, constructor-only use of shoe RNG, round-local hands | Retains existing console display and state lifetime; prior draft |
| Seed 42 scripted outcome and chain tests in D2 plan | Existing seed 1 win and seed 2 broke tests retained; new chain API tests restore absent-entry checks | Known outcomes are explicit; chain tests follow actual starter behavior, including IAE rather than specified NPE |

The following completion changes were already present as uncommitted edits when this audit began. The existing README attributed them to OpenAI Codex; this audit preserved and independently tested them:

- `Game` now settles dealer naturals without asking for an action and automatically stands on player 21. This follows the submitted spec's immediate dealer-blackjack edge case and removes an unnecessary prompt at 21.
- `ConsoleUI.InputEndedException` is a package-visible nested subtype of the existing `IllegalStateException` contract. `Game.run()` catches only that subtype, so unrelated state errors remain visible. A private `runSession()` keeps the session logic intact. EOF during an active player turn refunds the bet and omits the unfinished round from results. This closes the crash identified in the old README without changing public signatures.
- `Game.winnings()` calculates in `long` before narrowing to `int`; `Bankroll.credit()` rejects overflowing balances, and `placeBet()` rejects a bet if its maximum possible blackjack payout or resulting balance cannot fit an `int`. These add IAE boundary checks next to existing money validation. Existing valid ordinary bets and payout rounding are preserved. At `Integer.MAX_VALUE` balance no positive bet can safely win; end input to quit. Arbitrarily large bankrolls remain outside this whole-dollar `int` design.
- Tests now include direct missing-entry removal, record accessor/equality checks, multiple aces, input exhaustion and refund, integer limits, hit-to-21, bust, dealer natural, both naturals, soft 17, and a 30-round shoe reset. Existing test framework and all existing tests remain.
- README instructions, test claims, attribution and future-work promises were revised to reflect verification and distinguish historical claims from confirmed contributions.

This audit added two `CardCounterTest` cases for D2's repeated-advice contract, unchanged results after a rejected null observation, negative bankrolls, and `suggestedBet` below the 32-card minimum. No application-source changes were needed after inspecting the existing working tree. The audit also refreshed verification and attribution wording below.

No public class names or method signatures were changed by the completion edits or this audit. No dependencies, gameplay options, or new top-level application classes were added. Original scope mentions double down, but it is absent from the submitted D2 action API and model; it remains excluded, as do splitting and multiplayer.

### LinkedChain changes

**None.** `src/LinkedChain.java` is byte-for-byte unchanged from the checked-out baseline. Blackjack rules remain in application classes. New `LinkedChainTest` tests its public API, including null rejection, duplicate removal, absent entries, empty retrieval, bounds and array capacity. Specifications were not edited to match code.

### Previously identified unknowns

Shuffling is resolved by `Collections.shuffle` on a temporary `ArrayList`, then loading the chain. Card counting is implemented using the repository's exact-rank-composition recursion and approximate Kelly fraction `E / variance`, with memoized positions. Existing tests check the model document's fresh single-deck expected return and variance. The advice assumes optimal hit/stand play and ideal 1.5 blackjack profit; actual odd-dollar payouts truncate and a human may choose different actions. Therefore the displayed expectation is a model estimate, not a guaranteed return for the player's actual strategy.

### What remains unfinished or limited

No known failing tests or missing D3 bet/hit/stand MVP path after verification. The counter remains more complex than the rest of the project and incurs a noticeable calculation delay; `observe` is O(s), rather than D2's O(1), because it re-reads observations. Exact Kelly optimization for large edges is not implemented. No performance refactor or extra advice UI was added. Human file ownership and human review have not been verified. These must be confirmed before submission. Historical diagrams still show earlier APIs; the deviations above are the implementation reference.

### Big-O compared with D2

`N = 52 * decks`, `k` = hand size, `s` = cards observed in a shoe, `r` = rounds, and `P` = distinct model positions evaluated. Fixed ten-rank array work is constant per position, ignoring the length of numeric key representations.

| Methods | Actual cost and comparison |
|---|---|
| Card/record constructors, accessors, equality/hash; `Rank.value/label`; `Bankroll.*`; `Game.winnings` | O(1); fixed fields/arithmetic; unchanged by overflow checks |
| `Card.toString`, `RoundResult.toString` | O(1) for bounded fields/integers |
| `Shoe` constructor | O(N) build + shuffle + head insertion; matches D2 |
| `Shoe.deal/cardsRemaining/needsShuffle` | O(1); matches D2 |
| `Hand.add` | Amortized O(1); matches D2 |
| `Hand.total/isBust/toString` | O(k); matches D2 |
| `Hand.isBlackjack` | O(1): size check short-circuits unless exactly two cards; matches D2 |
| `GameHistory.record`, `CardHistory.record`, empty constructors | O(1); matches D2 |
| History `all/wins/losses/winRate` | O(n) for n entries; matches D2 (empty rate O(1)) |
| `CardCounter` constructor | O(1) initialization |
| `CardCounter.observe` | O(s), not D2's O(1): reconstruct unseen counts from chain |
| `CardCounter.expectedReturn/variance/suggestedBet` | O(s + P) after changed observations; O(s) with cached model result; D2's undefined B(N) is now concretely modeled |
| `ConsoleUI` constructor; `promptInt/promptAction/promptYesNo/show` | O(1) constructor; O(L) per line of length L, O(1) per bounded attempt as D2 assumes; attempts unbounded |
| `Game` constructor, `settle` | O(1) construction; O(k) settlement |
| `Game.run/main` | Per round O(s + P + k*s + k²), plus O(N) per shuffle and O(r + s) final history. Repeated scoring/printing and observations add work beyond D2's simplified O(B(N) + k) estimate. Hands and shoe are bounded by the fixed three-deck rules |

## Next Steps

| Priority | Remaining work | Owner |
|---|---|---|
| 1 | Confirm file ownership and personally review/explain the source, tests, and model before grading | TODO: assign all team members |
| 2 | Run the documented commands in the team's submission environment/Eclipse; confirm course formatting requirements | TODO: assign teammate |
| 3 | If required for final delivery, reconcile historical diagrams in a separately identified design update; retain submitted D2 documents | TODO: assign teammate |
| 4 | Profile counter delay; consider incremental counts only if necessary. Retain chain observations and existing public API | TODO: assign teammate |

There are no known remaining functional MVP fixes from the verified D3 path. The above review and environment checks remain submission tasks, not completed human work.

| Scope stretch goal | Decision for this prototype | Future tests if extended |
|---|---|---|
| Card counting | Already implemented; moved into MVP by submitted D2 §7. Retain existing advice | Repeated cached advice and rejected-null stability now tested; if optimized, retain these plus observed-rank limits, full-shoe resets and unchanged model values |
| ASCII card art | Dropped for D3 to preserve scope and avoid extra methods | None required for D3; if later approved, empty hand, multiple cards and two-digit ten rendering |
| Splitting pairs | Dropped | None planned |
| Cheating dealer | Dropped | None planned |
| Multiple players | Dropped | None planned |

The prior README's new hit/stand advice feature is also dropped for D3; it was not a scope commitment. Additional final-delivery validation: EOF after a completed round, locale-independent console parsing/formatting, and model agreement on more independently calculated shoe compositions. Owners for each are TODO until the team assigns them.

## Attribution

One row per Java source or test file. Git history identifies Marcos Salas as the author of prototype commit `3e0a378`; commit authorship does not establish who personally wrote or reviewed every line. The previous README reports Claude Code drafted the original sources/tests under Marcos's direction. That is a historical disclosure, not newly verified human work. `Updated Spec.docx` names other teammates for D2 document sections; those names are not used to invent code contributions.

| File | Recorded prior contribution | This completion pass | Human owner/reviewer |
|---|---|---|---|
| `src/Action.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Bankroll.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: revised overflow validation; tested | TODO: verify teammate |
| `src/Card.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/CardCounter.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/CardHistory.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/ConsoleUI.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: distinguished EOF; tested | TODO: verify teammate |
| `src/Game.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: revised round flow, EOF/refund and payout arithmetic; tested | TODO: verify teammate |
| `src/GameHistory.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Hand.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/LinkedChain.java` | CS 2114 staff starter (source header) | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Outcome.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Rank.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/RoundResult.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Shoe.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `src/Suit.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; exercised in suite | TODO: verify teammate |
| `test/BankrollTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/CardCounterTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: added repeatability/rejected-observation and advice boundary tests in this audit; executed | TODO: verify teammate |
| `test/CardHistoryTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; executed | TODO: verify teammate |
| `test/CardTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/ConsoleUITest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/GameHistoryTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; executed | TODO: verify teammate |
| `test/GameTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/HandTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/LinkedChainTest.java` | New in this pass | Codex: wrote public-API and generic-reuse tests; executed | TODO: verify teammate |
| `test/RoundResultTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: extended tests; executed | TODO: verify teammate |
| `test/ShoeTest.java` | Marcos Salas: prototype commit; prior README reports Claude Code draft | Codex: inspected; unchanged; executed | TODO: verify teammate |

### GenAI usage disclosure

The prior README states Claude Code generated the original prototype and tests from the team documents. The pre-existing uncommitted completion edits were disclosed as OpenAI Codex work: revisions to three application files, extensions to six test files, a new `LinkedChainTest`, and the README rewrite. During this audit OpenAI Codex inspected all 15 source files and 11 test files, the README, both D2 Word documents, Markdown design documents, diagrams, and model; preserved those edits; added two `CardCounterTest` cases; refreshed this README; compiled with Java 17; executed JUnit; and ran console smoke checks. Codex supplied the two new tests and this audit wording. No human review or individual human source contributions are claimed without confirmation. Team members must understand and verify this work before submitting it. No commits or pushes were made by Codex.

## Verification and Estimated D3 Assessment

Verification uses the local checkout of the supplied GitHub repository, not an independently fetched latest remote revision. Java 17.0.12 is available. Audit baseline: the 114 tests already in the working tree passed in 64.313 seconds before the two new tests were compiled. Verification on October 15, 2026:

- Application-only compilation with `javac --release 17` passed.
- Application + test compilation passed; JUnit reported **116 tests found, 116 started, 116 successful, 0 failed, 0 skipped, 0 aborted** in 80.081 seconds. All 14 containers succeeded. All 114 audit-baseline tests remain included. Final tests were compiled into a fresh `/private/tmp/project2-d3-audit/tests` directory to exclude stale class files; the documented repository commands compile the same sources.
- Four standalone `java -cp ... Game` console checks passed: normal stand/quit; nonnumeric/nonpositive deposits and invalid/oversized bets followed by valid input; EOF before deposit; EOF before bet. Every process exited 0 with empty stderr. The normal run and invalid-input retry run both ended at $110; unseeded live outcomes vary.
- Deterministic JUnit session tests additionally verify hit-to-21, bust without dealer draw, natural payouts/pushes, dealer blackjack, soft 17, EOF refund, broke termination, overflow retry, and 30 rounds including reshuffle/reset with complete round history.
- `git diff --check` passed. D2 documents, diagrams, mathematical model and `LinkedChain.java` have no changes. Attribution contains exactly one row for each of 26 Java files.

No known failing tests remain. The supplied GitHub page could not be fetched (cache miss); remote freshness, Eclipse execution, human review and the instructor's oral assessment have not been verified.

| D3 criterion | Estimate | Evidence / qualification |
|---|---|---|
| Working application slice | Exceeds candidate | Bet, deal, hit/stand, dealer, payout/history work; original scope's card-counting stretch works, though submitted D2 promotes it to MVP, so stretch credit is instructor-dependent |
| LinkedChain reused | Meets | Two real element types stored and retrieved; no third type added |
| Public API use | Exceeds candidate | Public-method-only storage and actual-vs-D2 complexity discussion; team must be able to explain it orally |
| JUnit tests | Exceeds candidate | Normal and bad/edge cases, empty collections and absent removals; 116 tests pass |
| Bad input survives | Exceeds candidate | Scope inputs handled in specified owner classes with re-prompt/exception behavior; EOF and overflow checks added |
| Reflection | Exceeds candidate | Named deviations, reasons, unchanged chain, unknowns and limits |
| Next Steps | Exceeds candidate | Ordered tasks with honest TODO owners; every scope stretch goal marked retained/dropped and further tests named |
| Attribution | Complete disclosure; human confirmation pending | One row per Java file and explicit GenAI roles; unknown human ownership remains TODO as requested |

These are estimates against all eight rubric categories, not an instructor-assigned grade. The rubric's detailed grading table lists the first seven categories, while attribution is also an explicit eighth submission requirement.
