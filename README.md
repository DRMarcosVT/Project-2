# Blackjack on a LinkedChain

CS 2114 Project 2, Deliverable 3: the prototype and its tests.

A console Blackjack game. The player deposits money, bets, hits or stands against a dealer who stands on 17, and is paid 1:1 for a win and 3:2 for a blackjack. Three decks are shuffled into a shoe, every dealt card is tracked, and before each round a card counter reports the expected return of the next round and the Kelly bet that goes with it. All storage goes through the provided `LinkedChain<T>`, unchanged.

The specification is `Docs/Updated Spec.docx`. The card-counting maths is `blackjack-expected-return-model.md`. The rubric is `Docs/Prototype-Rubric.md`.

## How to compile and run

Needs a JDK 17 or newer (the code uses records). Run everything from the project root; the same three commands work in PowerShell, cmd and bash.

```
javac -d out -cp "lib/*" src/*.java test/*.java
java -jar lib/junit-platform-console-standalone-1.10.2.jar execute -cp out --scan-classpath
java -cp out Game
```

The second command prints a summary ending in `86 tests successful, 0 tests failed`; it takes about 7 seconds because the card counter runs inside several tests. The third command starts the game.

In Eclipse: create a Java project, add `src` and `test` as source folders, add `lib/junit-platform-console-standalone-1.10.2.jar` to the build path, then right-click `test` and choose Run As > JUnit Test. Run `Game` as a Java application.

### Layout

| Path | What it holds |
|---|---|
| `src/LinkedChain.java` | The provided generic chain, unchanged |
| `src/*.java` | The fourteen application files (table below) |
| `test/*.java` | Ten JUnit 5 test files, one per application class with public methods |
| `lib/` | The JUnit console launcher (it contains JUnit 5) |
| `Docs/` | Scope, specifications, rubric, roadmap and diagrams |
| `blackjack-expected-return-model.md` | The card-counting model `CardCounter` implements |

## What each class does

| Class | One job | LinkedChain field |
|---|---|---|
| `Rank`, `Suit` | Name the thirteen ranks (with their points) and four suits | |
| `Card` (record) | One card; equal rank and suit means equal cards | |
| `Outcome`, `Action` | The five ways a round ends; the two choices a player has | |
| `RoundResult` (record) | One finished round: number, outcome, bet, net change | |
| `Shoe` | Build and shuffle three decks, deal from the top, say when to reshuffle | `LinkedChain<Card>` |
| `Hand` | Hold one side's cards and score them with aces as 11 or 1 | |
| `Bankroll` | Hold the balance; reject bad deposits and bets | |
| `GameHistory` | Keep every `RoundResult`; count wins, losses, win rate | `LinkedChain<RoundResult>` |
| `CardHistory` | Keep every card dealt from the current shoe for review | `LinkedChain<Card>` |
| `CardCounter` | From the cards seen, compute expected return, variance and the Kelly bet | `LinkedChain<Card>` |
| `ConsoleUI` | Read numbers, hit/stand and yes/no from the keyboard, re-asking on bad text; print lines | |
| `Game` | Run the session: deposit, rounds, dealer's turn, settlement, history | |

### How the LinkedChain is used

Four instances, two element types. `Shoe`, `CardHistory` and `CardCounter` each hold a `LinkedChain<Card>`; `GameHistory` holds a `LinkedChain<RoundResult>`. Every one is the same generic class; there is no `CardChain` or `ResultChain`. The application calls only `add`, `remove()`, `size`, `isEmpty` and `toArray`; the chain never sees a rule of Blackjack. `grep -n LinkedChain src/*.java` shows field declarations and those calls, nothing else.

## Big-O of the application methods

`N` is cards in the shoe (156), `k` is cards in one hand (at most 17), `n` is entries in the chain in question, `s` is cards seen from the current shoe, and `P` is the number of distinct positions (player hard total, ace flag, dealer up card, unseen counts) the counter's recursion visits.

| Method | Built | Spec estimate | Match? |
|---|---|---|---|
| `Shoe(decks, rng)` | O(N): build, shuffle, N adds at the head | O(N) | Yes |
| `Shoe.deal`, `cardsRemaining`, `needsShuffle` | O(1) | O(1) | Yes |
| `Hand.add` | O(1) amortised (`ArrayList`) | O(1) amortised | Yes |
| `Hand.total`, `isBust`, `toString` | O(k) | O(k) | Yes |
| `Hand.isBlackjack` | O(k): it calls `total()` | O(1) | No; k is at most 2 when it matters, so the difference is nil in practice |
| `Bankroll.*` | O(1) | O(1) | Yes |
| `GameHistory.record`, `CardHistory.record` | O(1): one `add` at the head | O(1) | Yes |
| `GameHistory.all`, `wins`, `losses`, `winRate`; `CardHistory.all` | O(n): one `toArray` walk | O(n) | Yes |
| `CardCounter.observe` | O(s): it rebuilds the unseen counts to reject an impossible card | O(1) | No; see Next Steps item 4 |
| `CardCounter.expectedReturn`, `variance`, `suggestedBet` | O(P) on the first call after a new card, each position costing ten array copies; measured 0.6 s for a fresh single deck and 0.75 s for three decks. Later calls with no new card are O(s). | O(B(n)), left undefined | B(n) is now P |
| `ConsoleUI.prompt*` | O(1) per attempt | O(1) per attempt | Yes |
| `Game.settle` | O(k) | not in spec | |
| `Game.winnings` | O(1) | not in spec | |
| `Game.run` | O(N) per shuffle plus O(P + k) per round plus O(r) to print r rounds | O(sN + r(B(N) + k) + r) | Yes, with B = P |

## Where validation lives

Every bad-input case from the scope, where it is caught, what the player sees, and the test that proves it.

| Case | Caught in | Response | Proved by |
|---|---|---|---|
| Deposit of 0 or less | `Bankroll(int)` throws `IllegalArgumentException`; `Game.promptDeposit` catches it | "Deposit must be greater than zero", asked again | `BankrollTest.constructorRejectsZeroAndNegativeDeposit`, `GameTest.runRepromptsBadDepositAndBadBet` |
| Bet of 0 or less | `Bankroll.placeBet` throws; `Game.promptBet` catches | "Bet must be greater than zero", asked again | `BankrollTest.placeBetRejectsBadAmounts` |
| Bet larger than the balance | `Bankroll.placeBet` throws; `Game.promptBet` catches | "Bet of $500 exceeds balance of $100", asked again | `BankrollTest.placeBetRejectsBadAmounts`, `GameTest.runRepromptsBadDepositAndBadBet` |
| Letters, a blank line or a decimal where a number is expected | `ConsoleUI.promptInt` | "Please enter a whole number.", asked again | `ConsoleUITest.promptIntRepromptsUntilNumber`, `GameTest.mainSurvivesNonNumericDeposit` |
| Anything but hit or stand | `ConsoleUI.promptAction` | "Type hit or stand.", asked again | `ConsoleUITest.promptActionRepromptsUntilHitOrStand` |
| Anything but y or n | `ConsoleUI.promptYesNo` | "Type y or n.", asked again | `ConsoleUITest.promptYesNoRepromptsUntilAnswer` |
| Hitting when no cards remain | `Game.run` checks `Shoe.needsShuffle()` before every round and replaces the shoe below 32 cards, so a round never starts with too few. `Shoe.deal` on an empty chain throws `IllegalStateException` as a backstop. | "Fewer than 32 cards left: shuffling a new shoe." | `ShoeTest.needsShuffleTurnsTrueBelowCutCard`, `ShoeTest.dealFromEmptyShoeThrows` |
| Balance reaches zero | `Game.run` checks `Bankroll.isBroke()` after each round | "You are out of money.", then the history; no "play again" prompt | `GameTest.runStopsWhenBroke` |
| `null` passed to an application class | `Hand.add`, `GameHistory.record`, `CardHistory.record`, `CardCounter.observe`, `Game.settle` throw `NullPointerException` before touching the chain | exception, state unchanged | the `*RejectsNull*` tests in each test file |
| Empty collections | `GameHistory.all/wins/losses/winRate`, `CardHistory.all`, `Hand.total/toString` | empty list, 0, 0.0, "(0)"; never an exception | `GameHistoryTest.*OfEmptyHistory*`, `CardHistoryTest.allOfEmptyHistoryIsEmptyList`, `HandTest.toStringOfEmptyHand` |

## Reflection

### What changed from the specification

Each item gives what `Updated Spec.docx` said, what was built, why, and where the change came from.

1. **What `GameHistory` stores.** The spec's class table said `LinkedChain<String>` and listed a separate `Round` class; its method table said `record(RoundResult)` and `List<RoundResult> all()`. Built: a `RoundResult` record stored in a `LinkedChain<RoundResult>`, and no `Round` class (its job, running one round, is `Game.playRound()`). A `String` cannot be counted as a win without parsing it back. Origin: GenAI draft, following the spec's own method table.
2. **`CardHistory` is a class.** The class table left it out and the class diagram put a `List<Card>` inside `Game`, while sections 4, 6 and 7 all named `CardHistory`. Built as its own class with a `LinkedChain<Card>`, as those sections say. Origin: GenAI draft.
3. **`CardCounter`'s model and API.** The spec said "Bayesian probability model" and "Kelly criterion" with `observe(Card)`, `suggestedUnits(int cardsRemaining)` and a field `int recommendedBet`, and left the cost as an undefined B(n). Built: the exact-composition expected-return model in `blackjack-expected-return-model.md`. The "prior" is the fresh shoe's composition and the "update" is removing each seen card, so there is no separate probability field. The public methods are `expectedReturn()` (row 10), `variance()` (row 12) and `suggestedBet(int bankroll)` (row 13); the bankroll is needed because Kelly sizes the bet as a fraction of it, and cards remaining is derived from the seen chain. Origin: the model document was written by the team; the Java is a GenAI draft of it. The code reproduces the document's worked example: E = −0.01928 against −0.0193, σ² = 0.9736 against 0.974.
4. **Null handling.** The spec said `LinkedChain.add(null)` throws `NullPointerException`; the provided class throws `IllegalArgumentException`. Rather than edit the chain, every application method that stores something calls `Objects.requireNonNull` first, so the application throws the NPE the spec promised and the chain is unchanged. Origin: GenAI, on reading the starter.
5. **`toArray`.** The spec said `toArray` makes a bigger array when the one given is too small; the provided class throws instead. The `all()` methods pass an array of exactly `size()` elements, so the exception can never fire. Origin: GenAI.
6. **Constructors that tests can drive.** The spec gave `ConsoleUI` only a `Scanner` and `Game` no random source. Built: `ConsoleUI(Scanner, PrintStream)` and `Game(ConsoleUI, Random)`, so tests feed typed lines from a `String`, capture what was printed, and get the same shuffle every run from a seeded `Random`. `main` wires up `System.in`, `System.out` and `new Random()`. Origin: GenAI.
7. **`settle` and `winnings` are public static on `Game`.** The spec folded settlement into `Game.run`. Pulling the outcome table and the payout table into two pure methods made the spec's "$5 blackjack pays $7 profit" test a one-liner with hand-built hands instead of a hunt for a seed that deals a blackjack. Origin: GenAI.
8. **Card text.** The spec printed cards as `A♠`. The Windows console prints suit symbols as `?` under the default code page, so cards print as `AS`, `10H`, `KD`. Origin: GenAI.
9. **Test plan.** The spec tested `LinkedChain`; the prototype tests only the application classes, since the chain is the unchanged starter and the rubric asks for application methods. The spec's `Game.run` test used seed 42 and a balance "recorded by hand"; the built tests use seeds 1 (first hand wins, ends at $110) and 2 (first hand loses, player goes broke), both found by running the game once. Test files for `Card`/`Rank` and `RoundResult` were added because their constructors and `toString` methods are hand-written. Origin: GenAI.
10. **Small field differences.** `Shoe` does not keep the `Random` as a field (it is only used in the constructor). `Game` does not keep `Hand player`/`Hand dealer` as fields; they are local to `playRound()` because a new pair is made every round. Origin: GenAI.

### What changed in the LinkedChain

Nothing. `src/LinkedChain.java` is the file we were given. The application uses `add`, `remove()`, `size`, `isEmpty` and `toArray`. Two properties of the starter shaped the application code: it rejects `null` with `IllegalArgumentException` (so `remove()` returning `null` can only mean an empty chain, which is how `Shoe.deal` detects an empty shoe), and `toArray` wants an array of at least `size()` elements (so the `all()` methods build one of exactly that size).

### What happened to our unknowns

- **Shuffling a whole linked chain.** Solved without touching the chain: `Shoe`'s constructor builds the 156 cards in an `ArrayList`, calls `Collections.shuffle(list, rng)`, then `add`s each card to a fresh chain. Dealing is `remove()` from the head.
- **The mathematics of card counting.** Solved as a written model (`blackjack-expected-return-model.md`, rows 1 to 13) and as code (`CardCounter`, one method per row group, with memoisation in a `HashMap` keyed by a string of the position). Still open: the model says to use the exact Kelly form when the edge is large near the end of a shoe, and the code always uses the approximation E/σ²; the recursion also computes the best hit-or-stand choice at every point (row 7) but the game does not show it to the player yet.

### What doesn't work yet

Every MVP feature from the scope runs, and all 86 tests pass. What remains:

- **End of input.** If the player closes the input (Ctrl+Z on Windows, Ctrl+D elsewhere) mid-game, `ConsoleUI` throws `IllegalStateException("Input ended before the game finished")` and the program ends with a stack trace. The scope never named this case; it is still a crash.
- **Advice delay.** Each round pauses about 0.75 seconds while the counter runs the model over 156 cards.
- **`CardCounter.observe` is O(s), not the spec's O(1)**, because it recounts the seen chain to reject a card the shoe cannot contain.
- **Attribution owners.** The table below has placeholders where teammates' names go.

## Next Steps

### What is left to finish the MVP, in order

| # | Item | Owner |
|---|---|---|
| 1 | Each teammate reads the files they own, fixes anything they disagree with, and replaces their placeholder in the attribution table | everyone |
| 2 | Confirm whether the grading session runs the tests from the command line or from Eclipse, and check the suite runs there | [name] |
| 3 | Catch `IllegalStateException` in `Game.main`, print "Input ended; goodbye." and exit cleanly | [name] |
| 4 | Keep an `int[] seenCounts` next to the chain in `CardCounter` so `observe` is O(1) as the spec estimated | [name] |

### Stretch goals from the scope

| Stretch goal | Decision | Why |
|---|---|---|
| Card counting | Already in the MVP (spec section 7 moved it there); built | |
| ASCII card art on the console | Committed | Smallest remaining goal: one `toArt()` method on `Hand`, no change to any other class |
| Show the model's hit-or-stand advice during the round | Committed (new, not in the scope) | `CardCounter` already computes it (row 7); it needs one public method and one line in `Game.playRound` |
| Splitting pairs | Dropped | Changes `Hand`, `Bankroll`, settlement and the model |
| Cheating dealer | Dropped | Needs a second shuffle path and a peek rule in `Shoe` |
| Multiple players | Dropped | Changes nearly every class |

### New tests the added work needs

| Work item | Tests |
|---|---|
| End-of-input handling (item 3) | `GameTest.mainExitsCleanlyWhenInputEnds`: empty input prints the goodbye line and throws nothing |
| O(1) `observe` (item 4) | No new behaviour; the existing `CardCounterTest.observe*` tests must still pass |
| ASCII card art | `HandTest.toArtDrawsEachCard` (two cards, two rows of boxes) and `HandTest.toArtOfEmptyHandIsEmpty`; a ten must fit in the box (`CardTest`) |
| Hit-or-stand advice | `CardCounterTest.bestActionHitsHard16AgainstTen` (the model's worked example: hit, −0.547 vs −0.580) and `CardCounterTest.bestActionRejectsNullHand` |

### Who does what

Fill in from the four team members (Marcos, Madeline Mehfoud, David Moon, Shreyas). Items 1 to 4 above and the two committed stretch goals each need one name; the person who owns a source file owns its test file.

## Attribution

Every file was drafted by a coding assistant (Claude Code) under Marcos's direction from `Docs/Updated Spec.docx`, `Docs/Prototype-Roadmap.md` and `blackjack-expected-return-model.md`. Each row's reviewer reads the file, changes what they disagree with, and is the person who defends it at the grading session. Replace `[name]` before submitting.

| File | Who | How |
|---|---|---|
| `src/LinkedChain.java` | CS 2114 staff | Provided; unchanged |
| `src/Rank.java`, `src/Suit.java`, `src/Card.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/Outcome.java`, `src/Action.java`, `src/RoundResult.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/Shoe.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/Hand.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/Bankroll.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/GameHistory.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/CardHistory.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/CardCounter.java` | Marcos; [name] | Model written by the team; Java GenAI-drafted from it; worked-example numbers checked by running the code |
| `src/ConsoleUI.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `src/Game.java` | Marcos; [name] | GenAI-drafted; reviewed |
| `test/CardTest.java`, `test/RoundResultTest.java` | Marcos; [name] | GenAI-drafted; reviewed; run |
| `test/ShoeTest.java` | Marcos; Madeline | GenAI-drafted; reviewed; run |
| `test/HandTest.java` | Marcos; Madeline | GenAI-drafted; reviewed; run |
| `test/BankrollTest.java` | Marcos; Madeline | GenAI-drafted; reviewed; run |
| `test/GameHistoryTest.java` | Marcos; Madeline | GenAI-drafted; reviewed; run |
| `test/CardHistoryTest.java` | Marcos; [name] | GenAI-drafted; reviewed; run |
| `test/CardCounterTest.java` | Marcos; [name] | GenAI-drafted; reviewed; run |
| `test/ConsoleUITest.java` | Marcos; [name] | GenAI-drafted; reviewed; run |
| `test/GameTest.java` | Marcos; [name] | GenAI-drafted; seeds 1 and 2 chosen by running the game; reviewed; run |
| `README.md` | Marcos; [name] | GenAI-drafted; reviewed |

### How we used GenAI

We gave Claude Code the submitted specification, the scope, this deliverable's rubric, the roadmap we wrote from the rubric, and our expected-return model. It wrote every source and test file in one pass, compiled them, ran the suite, and checked `CardCounter` against the model's worked example. It also chose the two seeds in `GameTest` by running the game. The model document itself and the decisions in the roadmap's Step 0 (which of the spec's contradictions to resolve which way) are the team's. Before the grading session each of us reads the tests for the files we own, because a test we cannot explain does not count as ours.
