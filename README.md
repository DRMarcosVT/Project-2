# Blackjack on a LinkedChain

CS 2114 Project 2, Deliverable 3: the prototype and its tests.

## Run the MVP

Open PowerShell in the project root (the folder holding `src`, `test` and `lib`) and run:

```powershell
javac -d out -cp "lib/*" src/*.java test/*.java
java -cp out Game
```

The first line compiles everything into `out`; the second starts the game. Needs a JDK 17 or newer on the PATH (`java -version` to check).

A console Blackjack game. The player deposits money, bets, hits or stands against a dealer who stands on 17, and is paid 1:1 for a win and 3:2 for a blackjack. Three decks are shuffled into a shoe, every dealt card is tracked, and before each round a card counter reports the expected return of the next round and the Kelly bet that goes with it. All storage goes through the provided `LinkedChain<T>`, unchanged.

The specification is `Docs/Updated Spec.docx`. The card-counting maths is `blackjack-expected-return-model.md`. The rubric is `Docs/Prototype-Rubric.md`.

## How to run the tests

After the compile command above, from the same PowerShell window:

```powershell
java -jar lib/junit-platform-console-standalone-1.10.2.jar execute -cp out --scan-classpath
```

It prints a summary ending in `116 tests successful, 0 tests failed` and takes about a minute because the card counter runs inside several tests. The same commands work unchanged in cmd and bash.

In Eclipse: create a Java project, add `src` and `test` as source folders, add `lib/junit-platform-console-standalone-1.10.2.jar` to the build path, then right-click `test` and choose Run As > JUnit Test. Run `Game` as a Java application.

### Layout

- `src/LinkedChain.java`: the provided generic chain, unchanged.
- `src/*.java`: the fourteen application files, listed in the next section.
- `test/*.java`: eleven JUnit 5 test files, one per application class with public methods plus `LinkedChainTest` for the starter's own API.
- `lib/`: the JUnit console launcher, which contains JUnit 5.
- `Docs/`: scope, specifications, rubric, roadmap and diagrams.
- `blackjack-expected-return-model.md`: the card-counting model `CardCounter` implements.

## What each class does

- `Rank`, `Suit`: name the thirteen ranks (with their points) and four suits.
- `Card` (record): one card; equal rank and suit means equal cards.
- `Outcome`, `Action`: the five ways a round ends; the two choices a player has.
- `RoundResult` (record): one finished round: number, outcome, bet, net change.
- `Shoe`: build and shuffle three decks, deal from the top, say when to reshuffle. Holds a `LinkedChain<Card>`.
- `Hand`: hold one side's cards and score them with aces as 11 or 1.
- `Bankroll`: hold the balance; reject bad deposits, bets and payouts, including any that would overflow an `int`.
- `GameHistory`: keep every `RoundResult`; count wins, losses, win rate. Holds a `LinkedChain<RoundResult>`.
- `CardHistory`: keep every card dealt from the current shoe for review. Holds a `LinkedChain<Card>`.
- `CardCounter`: from the cards seen, compute expected return, variance and the Kelly bet. Holds a `LinkedChain<Card>`.
- `ConsoleUI`: read numbers, hit/stand and yes/no from the keyboard, re-asking on bad text; print lines; report end of input.
- `Game`: run the session: deposit, rounds, dealer's turn, settlement, history. A dealer blackjack settles at once, a player total of 21 stands at once, and closing the input ends the session cleanly.

### How the LinkedChain is used

Four instances, two element types. `Shoe`, `CardHistory` and `CardCounter` each hold a `LinkedChain<Card>`; `GameHistory` holds a `LinkedChain<RoundResult>`. Every one is the same generic class; there is no `CardChain` or `ResultChain`. The application calls only `add`, `remove()`, `size`, `isEmpty` and `toArray`; the chain never sees a rule of Blackjack. `grep -n LinkedChain src/*.java` shows field declarations and those calls, nothing else. `LinkedChainTest.sameGenericClassStoresAndRetrievesRoundResults` stores both types through one class in one test.

## Big-O of the application methods

`N` is cards in the shoe (156), `k` is cards in one hand (at most 17), `n` is entries in the chain in question, `s` is cards seen from the current shoe, and `P` is the number of distinct positions (player hard total, ace flag, dealer up card, unseen counts) the counter's recursion visits.

Each entry gives what was built, then the spec's estimate, then whether they match.

- `Shoe(decks, rng)`: O(N), build, shuffle, N adds at the head. Spec O(N). Match.
- `Shoe.deal`, `cardsRemaining`, `needsShuffle`: O(1). Spec O(1). Match.
- `Hand.add`: O(1) amortised (`ArrayList`). Spec O(1) amortised. Match.
- `Hand.total`, `isBust`, `toString`: O(k). Spec O(k). Match.
- `Hand.isBlackjack`: O(1); the size check runs first and `total()` is only called on a two-card hand. Spec O(1). Match.
- `Bankroll.*`: O(1); the overflow checks are two comparisons. Spec O(1). Match.
- `GameHistory.record`, `CardHistory.record`: O(1), one `add` at the head. Spec O(1). Match.
- `GameHistory.all`, `wins`, `losses`, `winRate`; `CardHistory.all`: O(n), one `toArray` walk. Spec O(n). Match.
- `CardCounter.observe`: O(s), it rebuilds the unseen counts to reject an impossible card. Spec O(1). No match; see Next Steps item 2.
- `CardCounter.expectedReturn`, `variance`, `suggestedBet`: O(P) on the first call after a new card, each position costing ten array copies; measured 0.6 s for a fresh single deck and 0.75 s for three decks. Later calls with no new card are O(s). Spec O(B(n)), left undefined. B(n) is now P.
- `ConsoleUI.prompt*`: O(1) per attempt. Spec O(1) per attempt. Match.
- `Game.settle`: O(k). Not in the spec.
- `Game.winnings`: O(1). Not in the spec.
- `Game.run`: O(N) per shuffle plus O(P + k) per round plus O(r) to print r rounds. Spec O(sN + r(B(N) + k) + r). Match, with B = P.

## Bad input survives

The scope (`Docs/Scope.md`, "What bad input must it survive?" and the MVP list) names seven cases. Section 5 of `Docs/Updated Spec.docx` says where each is caught and, for most, what is printed. Each item below gives the scope's words, the spec's location and message, where the code catches it, what the player sees, the tests, and a way to see it at the console.

1. **Scope: "bet negative money".** Spec: `Bankroll.placeBet` throws `IllegalArgumentException`, `Game.run` shows the message and asks again; message "Bet must be greater than zero". Code: `Bankroll.placeBet` throws on zero or negative; `Game.promptBet`, called from `run`, catches and loops. Player sees "Bet must be greater than zero" and the bet prompt again. Tests: `BankrollTest.placeBetRejectsBadAmounts`, `GameTest.runRepromptsZeroAndNegativeBets`. Try it: type `-5` or `0` at "Bet:".
2. **Scope: "bets larger bets than the player's current balance".** Spec: same row as item 1. Code: `Bankroll.placeBet` throws when the bet exceeds the balance; `Game.promptBet` catches and loops. Player sees "Bet of $500 exceeds balance of $100" and the bet prompt again. Tests: `BankrollTest.placeBetRejectsBadAmounts`, `GameTest.runRepromptsBadDepositAndBadBet`. Try it: deposit 100, bet 500.
3. **Scope: "deposit money ... rejects negative values".** Spec: `Bankroll(int)` throws `IllegalArgumentException`, `Game.run` asks again; message "Deposit must be greater than zero". Code: the `Bankroll` constructor throws on zero or negative; `Game.promptDeposit`, called from `run`, catches and loops. Player sees "Deposit must be greater than zero" and the deposit prompt again. Tests: `BankrollTest.constructorRejectsZeroAndNegativeDeposit`, `GameTest.runRepromptsBadDepositAndBadBet`. Try it: type `-50` at "Deposit (whole dollars):".
4. **Scope: "if a player does not type in 'hit' or 'stand' when prompted, they will be prompted again".** Spec: `ConsoleUI.promptAction` tells the player to type hit or stand and keeps asking. Code: `ConsoleUI.promptAction` accepts `hit`, `h`, `stand`, `s` in any letter case and loops on anything else. Player sees "Type hit or stand." and the prompt again. Test: `ConsoleUITest.promptActionRepromptsUntilHitOrStand`. Try it: type `double` at the action prompt.
5. **Scope: "hit more than there are cards left in the stack".** Spec: `Game.run` checks `needsShuffle()` before each round so the player cannot run out mid-round; `Shoe.deal()` throws `IllegalStateException` on an empty shoe. Code: `Game.run` replaces the shoe, `CardHistory` and `CardCounter` when fewer than `Shoe.CUT_CARD` (32) cards remain, so a round always starts with at least 32 cards and a hand holds at most 17; `Shoe.deal` on an empty chain throws as the spec says. Player sees "Fewer than 32 cards left: shuffling a new shoe." before the round that would have run short. Tests: `ShoeTest.needsShuffleTurnsTrueBelowCutCard`, `ShoeTest.dealFromEmptyShoeThrows`, `GameTest.runReshufflesAndResetsCardTrackersButKeepsRoundHistory` (30 rounds through one reshuffle). Try it: play about 25 rounds and watch for the message.
6. **Scope: "if the money reaches zero, they cannot play anymore".** Spec: `Game.run` checks `isBroke()` after each round, prints "You are out of money." and the history, and ends. Code: exactly that, in `Game.run`; no "play again" prompt is shown. Player sees "You are out of money." then the round history. Test: `GameTest.runStopsWhenBroke` (seed 2 loses the first hand). Try it: deposit 10, bet 10, lose.
7. **Scope: "Handle invalid inputs" (letters where a number belongs).** Spec: `ConsoleUI.promptInt` catches `NumberFormatException` and asks for a whole number. Code: `ConsoleUI.promptInt` loops on any line `Integer.parseInt` rejects, which covers words, blank lines, decimals and numbers past `Integer.MAX_VALUE`. Player sees "Please enter a whole number." and the same prompt again. Tests: `ConsoleUITest.promptIntRepromptsUntilNumber`, `ConsoleUITest.promptIntRejectsIntegerOverflow`, `GameTest.mainSurvivesNonNumericDeposit`. Try it: type `ten` at the deposit prompt.

Every case is caught in the class the spec named, and items 1, 2, 3, 6 and 7 print the exact text the spec gave. For items 4 and 5 the spec named the behaviour (keep asking; reshuffle before the round) without quoting a message. Items 1 to 3 are caught in `Game.promptBet` and `Game.promptDeposit`, two private helpers that `Game.run` calls, which is the location the spec gave at method granularity.

The spec's last row, "add(null), remove() on empty", is outside the scope's list and is covered as follows. `Hand.add`, `GameHistory.record`, `CardHistory.record`, `CardCounter.observe` and `Game.settle` throw `NullPointerException` before touching the chain (the `*RejectsNull*` tests). `remove()` on the empty chain returns null, which `Shoe.deal` turns into `IllegalStateException` (`ShoeTest.dealFromEmptyShoeThrows`, `LinkedChainTest.removeHeadAndRemoveEmpty`). `GameHistory.all/wins/losses/winRate`, `CardHistory.all` and `Hand.total/toString` on nothing return an empty list, 0, 0.0 or "(0)" (`GameHistoryTest.*OfEmptyHistory*`, `CardHistoryTest.allOfEmptyHistoryIsEmptyList`, `HandTest.toStringOfEmptyHand`).

Three further cases that neither the scope nor the spec named are handled the same way:

- Anything but y or n at the play-again prompt: `ConsoleUI.promptYesNo` prints "Type y or n." and asks again (`ConsoleUITest.promptYesNoRepromptsUntilAnswer`).
- Input closed mid-game (Ctrl+Z on Windows, Ctrl+D elsewhere): every prompt throws `ConsoleUI.InputEndedException`, a subtype of `IllegalStateException`; `Game.run` catches that subtype, prints "Input ended; goodbye." and the history, and exits with status 0. During the player's turn the bet is first returned with "Unfinished round cancelled; bet returned." (`ConsoleUITest.allPromptsReportEndOfInput`, `GameTest.mainExitsCleanlyWhenInputEnds`, `GameTest.runRefundsUnfinishedRoundOnInputEnd`).
- A bet whose 3:2 payout or resulting balance would pass `Integer.MAX_VALUE`: `Bankroll.placeBet` throws before deducting, and `Bankroll.credit` and `Game.winnings` make the same check on the way back; the player sees "Bet could exceed whole-dollar integer limit" and the bet prompt again (`BankrollTest.placeBetRejectsUnrepresentablePayoutBeforeDeduction`, `BankrollTest.creditRejectsOverflowWithoutChangingBalance`, `GameTest.winningsRejectsNegativeAndOverflowingBets`, `GameTest.runRejectsBetThatCouldOverflowAndAllowsRetry`).

## Tests per public method

One line per public method. Each names the test for a normal call and the test for a bad call. Where a method takes no argument, the bad case is the worst state it can be called in: an empty history, an empty shoe, a zero balance.

**`Rank`**

- `value()`: normal `CardTest.valueOfAceIsEleven`, an ace is 11; edge `CardTest.valueOfFaceCardsIsTen`, jack, queen and king all score 10.
- `label()`: normal `CardTest.labelOfTenIsTwoCharacters`, a ten prints as "10"; edge `CardTest.labelOfAceIsA`, face cards and the ace print as one letter.

**`Card`** (a record; `Suit` has no methods of its own)

- Constructor: normal `CardTest.constructorKeepsRankAndSuit`, the card keeps both parts and equal parts give equal cards; bad input `CardTest.constructorRejectsNull`, a missing rank or suit throws `NullPointerException`.
- `rank()`, `suit()`, `equals`, `hashCode`: normal and bad `CardTest.recordAccessorsAndEqualityCoverOtherValuesAndNull`, the accessors return what went in, equal cards share a hash, and comparing with null or a different card is false.
- `toString()`: normal `CardTest.toStringIsLabelPlusSuitLetter`, "AS" for the ace of spades; edge `CardTest.toStringOfTenHasThreeCharacters`, "10H" is the one three-character card.

**`RoundResult`** (a record; `Outcome` and `Action` have no methods of their own)

- Constructor: normal `RoundResultTest.constructorKeepsParts`, all four fields come back; bad input `RoundResultTest.constructorRejectsNullOutcome`, a null outcome throws `NullPointerException`.
- `roundNumber()`, `outcome()`, `bet()`, `netChange()`, `equals`, `hashCode`: normal and bad `RoundResultTest.recordAccessorsAndEqualityCoverLossAndNull`, a loss record reads back its parts and compares false against null and a different record.
- `toString()`: normal `RoundResultTest.toStringShowsWinWithPlus`, a win prints "+$10"; edge `RoundResultTest.toStringShowsLossWithMinus`, a loss prints "-$10".

**`Shoe`**

- Constructor: normal `ShoeTest.constructorBuildsThreeFullDecks`, 156 cards with three of each; bad input `ShoeTest.constructorRejectsZeroDecksAndNullRandom`, zero decks or a null `Random` throws.
- `deal()`: normal `ShoeTest.dealReturnsCardAndShrinksShoe`, one card out and one fewer left; bad input `ShoeTest.dealFromEmptyShoeThrows`, dealing from an empty shoe throws `IllegalStateException`.
- `cardsRemaining()`: normal `ShoeTest.cardsRemainingCountsDown`, 146 after ten deals; edge `ShoeTest.cardsRemainingIsZeroWhenEmpty`, an emptied shoe reports 0.
- `needsShuffle()`: normal `ShoeTest.needsShuffleIsFalseWithPlentyOfCards`; edge `ShoeTest.needsShuffleTurnsTrueBelowCutCard`, false at 32 cards and true at 31.

**`Hand`**

- `add(Card)`: normal `HandTest.addPutsCardInHand`, the first card sets the total; bad input `HandTest.addRejectsNull`, null throws and the hand is unchanged.
- `total()`: normal `HandTest.totalCountsAceAsElevenWhenItFits`; edge `HandTest.totalDropsAceToOneAndEmptyIsZero`, the ace drops to 1 when 11 would bust and an empty hand is 0; `HandTest.multipleAcesDemoteIndividually`, each ace drops on its own.
- `isBust()`: normal `HandTest.isBustOverTwentyOne`, 24 is a bust; edge `HandTest.isBustFalseAtTwentyOne`, exactly 21 is not.
- `isBlackjack()`: normal `HandTest.isBlackjackForAceAndKing`; edge `HandTest.isBlackjackFalseForThreeCardTwentyOne`, 21 with three cards is not a blackjack.
- `toString()`: normal `HandTest.toStringListsCardsAndTotal`; edge `HandTest.toStringOfEmptyHand`, an empty hand prints "(0)".

**`Bankroll`**

- Constructor: normal `BankrollTest.constructorSetsBalance`; bad input `BankrollTest.constructorRejectsZeroAndNegativeDeposit`.
- `placeBet(int)`: normal `BankrollTest.placeBetSubtractsAndReturnsAmount`; bad input `BankrollTest.placeBetRejectsBadAmounts`, zero, negative and more than the balance each throw with the balance unchanged; `BankrollTest.placeBetRejectsUnrepresentablePayoutBeforeDeduction`, a bet whose 3:2 payout would pass `Integer.MAX_VALUE` throws before anything is deducted.
- `credit(int)`: normal `BankrollTest.creditAddsPayout`, zero is allowed for a lost round; bad input `BankrollTest.creditRejectsNegative`; `BankrollTest.creditRejectsOverflowWithoutChangingBalance`, a payout that would pass `Integer.MAX_VALUE` throws.
- `balance()`: normal `BankrollTest.balanceTracksBetAndCredit`; edge `BankrollTest.balanceCanReachZero`, betting everything leaves exactly 0.
- `isBroke()`: normal `BankrollTest.isBrokeFalseWithOneDollar`; edge `BankrollTest.isBrokeTrueAtZero`.

**`GameHistory`**

- `record(RoundResult)`: normal `GameHistoryTest.recordStoresRound`; bad input `GameHistoryTest.recordRejectsNull`, null throws and nothing is stored.
- `all()`: normal `GameHistoryTest.allKeepsDuplicatesNewestFirst`; edge `GameHistoryTest.allOfEmptyHistoryIsEmptyList`, an empty list rather than null.
- `wins()`: normal `GameHistoryTest.winsCountsWinAndBlackjack`; edge `GameHistoryTest.winsOfEmptyHistoryIsZero`.
- `losses()`: normal `GameHistoryTest.lossesCountsLossAndBust`; edge `GameHistoryTest.lossesOfEmptyHistoryIsZero`.
- `winRate()`: normal `GameHistoryTest.winRateIsWinsOverRounds`, two wins in five rounds is 0.4; edge `GameHistoryTest.winRateOfEmptyHistoryIsZero`, 0.0 with no division by zero.

**`CardHistory`**

- `record(Card)`: normal `CardHistoryTest.recordStoresCard`; bad input `CardHistoryTest.recordRejectsNull`.
- `all()`: normal `CardHistoryTest.allKeepsDuplicatesNewestFirst`, the same card three times gives three entries; edge `CardHistoryTest.allOfEmptyHistoryIsEmptyList`.

**`CardCounter`**

- Constructor: normal `CardCounterTest.constructorAcceptsOneDeck`; bad input `CardCounterTest.constructorRejectsZeroDecks`.
- `observe(Card)`: normal `CardCounterTest.observeChangesExpectedReturn`, seeing a card moves the expected return; bad input `CardCounterTest.observeRejectsNullAndImpossibleCard`, null and a fifth ace from a one-deck shoe both throw; `CardCounterTest.repeatedAdviceAndRejectedObservationPreserveModel`, a rejected card leaves the advice unchanged.
- `expectedReturn()`: normal `CardCounterTest.expectedReturnOfFreshDeckMatchesModel`, −0.0193 for a fresh deck; bad state `CardCounterTest.expectedReturnRejectsTooFewUnseenCards`, 31 unseen cards throws.
- `variance()`: normal `CardCounterTest.varianceOfFreshDeckMatchesModel`, 0.974 for a fresh deck; bad state `CardCounterTest.varianceRejectsTooFewUnseenCards`.
- `suggestedBet(int)`: normal `CardCounterTest.suggestedBetFollowsTheEdge`, a poor shoe gets the minimum and a rich shoe more; bad input `CardCounterTest.suggestedBetRejectsZeroBankrollAndNeverExceedsIt` and `CardCounterTest.suggestedBetRejectsNegativeBankrollAndTooFewCards`, zero or negative bankroll throws and the bet never exceeds the bankroll.

**`ConsoleUI`**

- Constructor: normal `ConsoleUITest.constructorBuildsUsableUI`; bad input `ConsoleUITest.constructorRejectsNull`, a null `Scanner` or `PrintStream` throws.
- `promptInt(String)`: normal `ConsoleUITest.promptIntReadsNumber`; bad input `ConsoleUITest.promptIntRepromptsUntilNumber`, a word, a blank line and a decimal are each asked again; `ConsoleUITest.promptIntRejectsIntegerOverflow`, a number past `Integer.MAX_VALUE` is asked again.
- `promptAction()`: normal `ConsoleUITest.promptActionReadsHit`, any letter case; bad input `ConsoleUITest.promptActionRepromptsUntilHitOrStand`.
- `promptYesNo(String)`: normal `ConsoleUITest.promptYesNoReadsYes`; bad input `ConsoleUITest.promptYesNoRepromptsUntilAnswer`.
- `show(String)`: normal `ConsoleUITest.showPrintsLine`; edge `ConsoleUITest.showPrintsBlankLineForEmptyMessage` and `ConsoleUITest.nullTextUsesPrintStreamConvention`, an empty message prints a bare line break and null prints "null".
- All three prompts at end of input: `ConsoleUITest.promptThrowsWhenInputEnds` and `ConsoleUITest.allPromptsReportEndOfInput`, each throws `InputEndedException` when the lines run out.

**`Game`**

- Constructor: normal `GameTest.constructorAcceptsUiAndRandom`; bad input `GameTest.constructorRejectsNull`.
- `run()`: normal `GameTest.runPlaysOneRoundAndShowsHistory`, seed 1 wins and ends at $110; bad input `GameTest.runRepromptsBadDepositAndBadBet`, `GameTest.runRepromptsZeroAndNegativeBets` and `GameTest.runRejectsBetThatCouldOverflowAndAllowsRetry`, each bad amount is asked again; `GameTest.runRefundsUnfinishedRoundOnInputEnd`, closing the input mid-round returns the bet; edge `GameTest.runStopsWhenBroke`, `GameTest.runHitsToTwentyOneAndAutomaticallyStands`, `GameTest.runHitBustForfeitsBetWithoutDealerDraw`, `GameTest.runSettlesDealerBlackjackWithoutActionPrompt`, `GameTest.runPaysOddBlackjackAndPushesBothNaturals`, `GameTest.runDealerStandsOnSoftSeventeen` and `GameTest.runReshufflesAndResetsCardTrackersButKeepsRoundHistory`, one scripted session per rule of the round.
- `settle(Hand, Hand)`: normal `GameTest.settleCoversEveryOutcome`, every row of the outcome table; bad input `GameTest.settleRejectsNullHand`.
- `winnings(Outcome, int)`: normal `GameTest.winningsFollowPayoutTable`, a $5 blackjack pays $7 profit; bad input `GameTest.winningsRejectsZeroBetAndNullOutcome` and `GameTest.winningsRejectsNegativeAndOverflowingBets`.
- `main(String[])`: normal `GameTest.mainRunsASession`; bad input `GameTest.mainSurvivesNonNumericDeposit`, a word where the deposit should be is asked again; `GameTest.mainExitsCleanlyWhenInputEnds`, empty input prints the goodbye line and throws nothing.

**`LinkedChain`** (the unchanged starter; tested so its real behaviour is on record)

- `add(T)`: `LinkedChainTest.addStoresDuplicatesAndRejectsNull`, duplicates are kept and null throws `IllegalArgumentException`.
- `remove()`: `LinkedChainTest.removeHeadAndRemoveEmpty`, the head comes off and an empty chain returns null.
- `remove(T)`: `LinkedChainTest.removeValueRemovesOnlyOneAndPreservesAbsentEntries`, one copy goes and an absent entry returns false with nothing changed.
- `contains`, `count`, `size`, `isEmpty`: `LinkedChainTest.containsCountSizeAndEmptyFollowState`.
- `addToEnd(T)`: `LinkedChainTest.addToEndPreservesOrderAndRejectsNull`.
- `get(int)`: `LinkedChainTest.getReadsWithoutRemovalAndRejectsInvalidIndices`, a bad index throws.
- `toArray(T[])`: `LinkedChainTest.toArrayCopiesEntriesAndChecksCapacity`, an array that is too small throws.
- `toString()`: `LinkedChainTest.toStringListsEntriesAndHandlesEmpty`.
- Generic reuse: `LinkedChainTest.sameGenericClassStoresAndRetrievesRoundResults`, one class holds `Card` in one chain and `RoundResult` in another.

## Reflection

### What changed from the specification

Each item gives what `Updated Spec.docx` said, what was built, why, and where the change came from.

1. **What `GameHistory` stores.** The spec's class table said `LinkedChain<String>` and listed a separate `Round` class; its method table said `record(RoundResult)` and `List<RoundResult> all()`. Built: a `RoundResult` record stored in a `LinkedChain<RoundResult>`, and no `Round` class (its job, running one round, is `Game.playRound()`). A `String` cannot be counted as a win without parsing it back. Origin: the spec's own method table.
2. **`CardHistory` is a class.** The class table left it out and the class diagram put a `List<Card>` inside `Game`, while sections 4, 6 and 7 all named `CardHistory`. Built as its own class with a `LinkedChain<Card>`, as those sections say.
3. **`CardCounter`'s model and API.** The spec said "Bayesian probability model" and "Kelly criterion" with `observe(Card)`, `suggestedUnits(int cardsRemaining)` and a field `int recommendedBet`, and left the cost as an undefined B(n). Built: the exact-composition expected-return model in `blackjack-expected-return-model.md`. The "prior" is the fresh shoe's composition and the "update" is removing each seen card, so there is no separate probability field. The public methods are `expectedReturn()` (row 10), `variance()` (row 12) and `suggestedBet(int bankroll)` (row 13); the bankroll is needed because Kelly sizes the bet as a fraction of it, and cards remaining is derived from the seen chain. Origin: the model document, drafted by Marcos with genAI assistance. The code reproduces the document's worked example: E = −0.01928 against −0.0193, σ² = 0.9736 against 0.974.
4. **Null handling.** The spec said `LinkedChain.add(null)` throws `NullPointerException`; the provided class throws `IllegalArgumentException`. Rather than edit the chain, every application method that stores something calls `Objects.requireNonNull` first, so the application throws the NPE the spec promised and the chain is unchanged. Origin: reading the starter.
5. **`toArray`.** The spec said `toArray` makes a bigger array when the one given is too small; the provided class throws instead. The `all()` methods pass an array of exactly `size()` elements, so the exception can never fire.
6. **Constructors that tests can drive.** The spec gave `ConsoleUI` only a `Scanner` and `Game` no random source. Built: `ConsoleUI(Scanner, PrintStream)` and `Game(ConsoleUI, Random)`, so tests feed typed lines from a `String`, capture what was printed, and get the same shuffle every run from a seeded `Random`. `main` wires up `System.in`, `System.out` and `new Random()`.
7. **`settle` and `winnings` are public static on `Game`.** The spec folded settlement into `Game.run`. Pulling the outcome table and the payout table into two pure methods made the spec's "$5 blackjack pays $7 profit" test a one-liner with hand-built hands instead of a hunt for a seed that deals a blackjack.
8. **Card text.** The spec printed cards as `A♠`. The Windows console prints suit symbols as `?` under the default code page, so cards print as `AS`, `10H`, `KD`.
9. **Test plan.** The spec's `Game.run` test used seed 42 and a balance "recorded by hand"; the built tests use seeds 1 (first hand wins, ends at $110) and 2 (first hand loses, player goes broke), both found by running the game once. Test files for `Card`/`Rank` and `RoundResult` were added because their constructors and `toString` methods are hand-written. `LinkedChainTest` was added on October 15 to pin down the starter's real behaviour (`IllegalArgumentException` for null, false for removing an absent entry, null for `remove()` on an empty chain) and to store both element types through one class in a single test.
10. **Small field differences.** `Shoe` does not keep the `Random` as a field (it is only used in the constructor). `Game` does not keep `Hand player`/`Hand dealer` as fields; they are local to `playRound()` because a new pair is made every round.
11. **Money that would overflow an `int`.** The spec said nothing about it. `Bankroll.placeBet` now refuses a bet whose 3:2 payout or resulting balance would pass `Integer.MAX_VALUE`, and `Bankroll.credit` and `Game.winnings` refuse the same on the way back, each with `IllegalArgumentException` next to the existing money checks. Origin: the October 15 test pass.
12. **End of input.** The spec never named it. `ConsoleUI` now throws `InputEndedException`, a subtype of the `IllegalStateException` the spec promised, and `Game.run` catches that subtype, refunds an unfinished bet, prints the history and exits with status 0. Origin: the October 15 test pass, closing the crash listed in the first version of this README.
13. **Two round-flow rules.** A dealer blackjack settles the round without asking the player for an action, and a player total of 21 stands without a prompt. The spec's section 6 names the dealer-blackjack case; the auto-stand removes a prompt whose only legal answer was stand. Origin: the October 15 test pass.

### What changed in the LinkedChain

Nothing. `src/LinkedChain.java` is the file we were given. The application uses `add`, `remove()`, `size`, `isEmpty` and `toArray`. Two properties of the starter shaped the application code: it rejects `null` with `IllegalArgumentException` (so `remove()` returning `null` can only mean an empty chain, which is how `Shoe.deal` detects an empty shoe), and `toArray` wants an array of at least `size()` elements (so the `all()` methods build one of exactly that size). `LinkedChainTest` records both properties.

### What happened to our unknowns

- **Shuffling a whole linked chain.** Solved without touching the chain: `Shoe`'s constructor builds the 156 cards in an `ArrayList`, calls `Collections.shuffle(list, rng)`, then `add`s each card to a fresh chain. Dealing is `remove()` from the head.
- **The mathematics of card counting.** Solved as a written model (`blackjack-expected-return-model.md`, rows 1 to 13) and as code (`CardCounter`, one method per row group), and we decided to size the bet with the Kelly approximation E/σ² rather than the exact Kelly number because the exact form is an optimisation, a search for the fraction that maximises the average of log(1 + fW), while the approximation is one division.

### What doesn't work yet

Every MVP feature from the scope runs, and all 116 tests pass. What remains:

- **Advice delay.** Each round pauses about 0.75 seconds while the counter runs the model over 156 cards.
- **`CardCounter.observe` is O(s) where the spec estimated O(1)**, because it recounts the seen chain to reject a card the shoe cannot contain.

## Next Steps

### What is left to finish the MVP, in order

1. Each teammate reads the files they own and fixes anything they disagree with. Owner: everyone.
2. Keep an `int[] seenCounts` next to the chain in `CardCounter` so `observe` is O(1) as the spec estimated. Owner: [name].
3. Add a package-private `kellyFraction(double[] chances)` to `CardCounter` that bisects f in (0, 1) on Σ P(W=w)·w/(1 + fw) = 0, and have `suggestedBet` call it when the edge is above 5%. Below that the result equals E/σ² to the dollar, so the fresh-deck worked example is unchanged. Owner: [name].

### Stretch goals from the scope

- **Card counting.** A stretch goal in the scope; the updated spec (section 7) moved it into the MVP. Built and running: `CardCounter` prints the expected return and the Kelly bet before every round, and `CardCounterTest` checks it against the model's worked example.
- **ASCII card art on the console.** Committed. Smallest remaining goal: one `toArt()` method on `Hand`, no change to any other class.
- **Show the model's hit-or-stand advice during the round.** Committed; this one is new and was not in the scope. `CardCounter` already computes it (row 7); it needs one public method and one line in `Game.playRound`.
- **Splitting pairs.** Dropped. We're keeping the BlackJack simple.
- **Cheating dealer.** Dropped. Redundant by the linkedchain.
- **Multiple players.** Dropped. Out of Scope.

### New tests the added work needs

- **O(1) `observe` (item 2).** No new behaviour; the existing `CardCounterTest.observe*` and `repeatedAdviceAndRejectedObservationPreserveModel` tests must still pass.
- **Exact Kelly (item 3).** `CardCounterTest.kellyFractionMatchesExactFormForLargeEdge`: chances of 0.8 for +1 and 0.2 for −1 must give 0.60, where E/σ² gives 0.94. `CardCounterTest.kellyFractionMatchesApproximationForSmallEdge`: the fresh-deck chances from the worked example must give the same whole-dollar bet as before. `CardCounterTest.kellyFractionRejectsChancesThatDoNotSumToOne` for the bad input.
- **ASCII card art.** `HandTest.toArtDrawsEachCard` (two cards, two rows of boxes) and `HandTest.toArtOfEmptyHandIsEmpty`; a ten must fit in the box (`CardTest`).

## Attribution

Every source file was drafted by Marcos from `Docs/Updated Spec.docx`, `Docs/Prototype-Roadmap.md` and `blackjack-expected-return-model.md`. On October 15 David Moon revised `Bankroll`, `ConsoleUI` and `Game` (overflow checks, end-of-input handling, dealer blackjack, auto-stand on 21) and extended the tests. Each file's reviewer reads the file, changes what they disagree with, and is the person who defends it at the grading session.

The test files were drafted by an AI assistant so that every branch of every public method has a test. We have no coverage tool that warns us of an uncovered branch, so the assistant fills that role: it reads each method and writes a case for each path. Marcos and David Moon reviewed every test and ran the suite.

- `src/LinkedChain.java`: CS 2114 staff; provided, unchanged.
- `src/Rank.java`: drafted by Marcos; reviewed by Madeline Mehfoud.
- `src/Suit.java`: drafted by Marcos; reviewed by Madeline Mehfoud.
- `src/Card.java`: drafted by Marcos; reviewed by Madeline Mehfoud.
- `src/Outcome.java`: drafted by Marcos; reviewed by [name].
- `src/Action.java`: drafted by Marcos; reviewed by [name].
- `src/RoundResult.java`: drafted by Marcos; reviewed by [name].
- `src/Shoe.java`: drafted by Marcos; reviewed by Madeline Mehfoud.
- `src/Hand.java`: drafted by Marcos; reviewed by Madeline Mehfoud.
- `src/Bankroll.java`: drafted by Marcos; overflow checks added by David Moon; reviewed by [name].
- `src/GameHistory.java`: drafted by Marcos; reviewed by [name].
- `src/CardHistory.java`: drafted by Marcos; reviewed by [name].
- `src/CardCounter.java`: model drafted and coded by Marcos with genAI assistance; worked-example numbers checked by running the code; reviewed by [name].
- `src/ConsoleUI.java`: drafted by Marcos; `InputEndedException` added by David Moon; reviewed by David Moon.
- `src/Game.java`: drafted by Marcos; end-of-input handling, dealer blackjack and auto-stand added by David Moon; reviewed by David Moon.
- `test/CardTest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/RoundResultTest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/ShoeTest.java`: AI-drafted for branch coverage; reviewed by Marcos and David Moon; run.
- `test/HandTest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/BankrollTest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/GameHistoryTest.java`: AI-drafted for branch coverage; reviewed by Marcos and David Moon; run.
- `test/CardHistoryTest.java`: AI-drafted for branch coverage; reviewed by Marcos and David Moon; run.
- `test/CardCounterTest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/ConsoleUITest.java`: AI-drafted for branch coverage; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/GameTest.java`: AI-drafted for branch coverage, seeds 1 and 2 chosen by running the game; extended and reviewed by David Moon; reviewed by Marcos; run.
- `test/LinkedChainTest.java`: written by David Moon; reviewed by Marcos; run.
