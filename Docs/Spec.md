# Blackjack: Project Specification

This turns our scope into a blueprint a teammate or a coding assistant can build from. Language: Java. We build the MVP from the scope plus one stretch goal, the card counting helper; the others are dropped (see Revised scope).

## 1. Class design, data and state

One class, one job. Each row ends with the class's fields, so this table is also the data-and-state inventory.

| Class | Its one job, then its fields |
|---|---|
| `LinkedChain<T>` | Store entries of type `T` in a singly linked chain; knows nothing about cards or money. Fields: `Node firstNode`, `int numberOfEntries`. |
| `Card` (record); `Rank`, `Suit` (enums) | Name one card; `Rank` carries the point value (2..10, face cards 10, ace 11). Fields: `Rank rank`, `Suit suit`. |
| `Shoe` | Build and shuffle three decks into a chain, deal one card at a time, say when to reshuffle. Fields: `LinkedChain<Card> cards`, `Random rng`, `static final int CUT_CARD = 32`. |
| `Hand` | Hold one participant's cards and compute the total with aces as 11 or 1; one each for player and dealer. Field: `List<Card> cards`. |
| `Bankroll` | Hold the balance and enforce the money rules: positive deposit, bet between 1 and the balance. Field: `int balance` (whole dollars). |
| `RoundResult` (record); `Outcome`, `Action` (enums) | Describe one finished round; name the outcomes (`BLACKJACK`, `WIN`, `PUSH`, `LOSS`, `BUST`) and choices (`HIT`, `STAND`, `DOUBLE`). Fields: `int round`, `Outcome outcome`, `int bet`, `int netChange`. |
| `GameHistory` | Keep every `RoundResult` in a chain; report wins, losses, win rate. Field: `LinkedChain<RoundResult> rounds`. |
| `CardHistory` | Keep every card dealt since the last shuffle in a chain, for review. Field: `LinkedChain<Card> seen`. |
| `CardCounter` | Hold the Hi-Lo running count, convert it to a true count, suggest a bet size in units. Field: `int runningCount`. |
| `ConsoleUI` | Read keyboard lines into `int`, `Action` or `boolean`, re-prompting on bad text; print what `Game` hands it. Fields: `Scanner in`, `PrintStream out`. |
| `Game` | Run the session: deposit, rounds, dealer's turn (hit under 17), settlement; stop at zero balance or when the player quits. Fields: `Shoe shoe`, `Hand player`, `Hand dealer`, `Bankroll bankroll`, `GameHistory history`, `CardHistory cardHistory`, `CardCounter counter`, `ConsoleUI ui`, `int round`. |

`Node` is a private inner class of the chain with `T data` and `Node next`. `firstNode` is the newest entry, null when empty. There is no tail: every add and every `remove()` is at the head, so the far end is never needed. `numberOfEntries` is updated by `add` and both removes, so `size()` is O(1).

The chain is instantiated three times: undealt cards in `Shoe`, dealt cards in `CardHistory`, finished rounds in `GameHistory`. Each owner decides what to add and when; the chain only stores.

**Why this split.** Each bad-input rule gets one home. Money rules live in `Bankroll` because every path that changes the balance (deposit, bet, double down, payout) goes through it, so no caller can skip the check. Text parsing lives in `ConsoleUI` because a non-numeric bet is a keyboard problem, and one `promptInt` serves deposits and bets. We rejected a single `Player` class holding hand, balance and history: it would change for three unrelated reasons, and the dealer needs a `Hand` too. We also rejected a `shuffle()` on `LinkedChain`, the scope's open question: the `Shoe` constructor builds the 156 cards in an `ArrayList`, calls `Collections.shuffle`, and adds each to a new chain, so the randomness stays with the class that owns the deck.

**Two chosen numbers.** Money is whole dollars; a 3:2 payout on an odd bet uses integer division (`bet * 3 / 2`, so a $5 blackjack pays $7) so every prompt stays a whole number. The cut card is 32 because that guarantees a round can never empty the shoe: the longest non-busting hand in a three-deck shoe is 16 cards (twelve aces as 1 each, then four twos), the dealer's longest is 15 (twelve aces, then three twos reaches 18 and the rule says stand), both cannot hold all twelve aces, so 31 is a loose bound and we reshuffle between rounds when fewer than 32 remain.

## 2. System diagram

![System diagram](system-diagram.png)

Each arrow carries the call made and the value returned. The cylinders are the three `LinkedChain` instances. A `Card` leaves the shoe's chain in `Shoe.deal()`, is added to the `CardHistory` chain, and is shown to the `CardCounter`, all inside `Game`'s private `draw()`. Source: `Docs/system-diagram.mmd`.

## 3. The generic contract

```java
public class LinkedChain<T>
```

`T` is unbounded. The chain promises three things about `T`: it stores and returns exactly the declared type (a `LinkedChain<Card>` rejects a `RoundResult` at compile time); it calls nothing on `T` except `equals`, used by `contains`, `count` and `remove(T)`; and it never stores `null`, so `remove()` returning `null` means only that the chain was empty.

Every operation is type-safe. `add`, `contains`, `count` and `remove(T)` take `T` rather than `Object`, so a wrong-type argument is a compile error. `remove()` returns `T` with no cast. `toList()` returns a `List<T>` built with `ArrayList.add`, so the class creates no generic array and has no unchecked cast.

**Why no bound.** `Card` and `RoundResult` share no useful interface. A `Comparable<T>` bound would force `RoundResult` to order pushes against losses, and nothing in the game sorts a chain or asks for a maximum. Both element types are records, so `equals` compares fields: two `Card(ACE, SPADES)` from different decks are equal, which is what `count` needs when the shoe holds three of them.

## 4. Method signatures and Big-O

`n` is the number of entries in the chain; `k` is cards in one hand, at most 16. Exception messages are listed once, in section 5.

### `LinkedChain<T>`

| Signature | Behaviour, including edge cases; Big-O and why |
|---|---|
| `boolean add(T e)` | Puts `e` at the head, returns `true`; `NullPointerException` on null. O(1): one node, two reference writes. |
| `T remove()` | Removes and returns the head entry; `null` if empty. O(1): head moves to `next`. |
| `boolean remove(T e)` | Removes one entry equal to `e`, returns `true`; `false` if absent or empty. O(n): walks until `equals` matches. |
| `boolean contains(T e)` | `true` if some entry equals `e`; `false` on empty. O(n): same walk, stops at first match. |
| `int count(T e)` | Number of entries equal to `e`; 0 on empty. O(n): duplicates may be anywhere, so every node is visited. |
| `int size()` | Number of entries. O(1): returns the counter. |
| `List<T> toList()` | New `ArrayList<T>` of every entry, head first; empty list, never null, on an empty chain. O(n): one visit per node. |

**Changes from the LinkedChain we were given** [check against the starter; delete any that already match]: `toArray()` returning `T[]` becomes `toList()` returning `List<T>`, because the array version needs the unchecked cast `(T[]) new Object[n]`; `getCurrentSize()` and `getFrequencyOf()` are renamed `size()` and `count()` to match the assignment; `add` now rejects null so `remove()` returning null is unambiguous.

### Application classes

| Signature | Behaviour; Big-O and why |
|---|---|
| `Shoe(int decks, Random rng)` | Builds `decks` decks, shuffles, loads a new chain; `IllegalArgumentException` if `decks < 1`. O(n), n = 52·decks: Fisher-Yates on an `ArrayList`, then n O(1) adds. |
| `Card Shoe.deal()` | Removes and returns the head card; `IllegalStateException` if none. O(1): one `remove()`. |
| `int Shoe.cardsRemaining()`; `boolean needsShuffle()` | `cards.size()`; `cardsRemaining() < CUT_CARD`. Both O(1). |
| `void Hand.add(Card c)` | Appends; `NullPointerException` on null. O(1) amortised. |
| `int Hand.total()` | Sum of values; while over 21 and an ace counts 11, count it 1. Empty hand is 0. O(k): one pass, one demotion per ace at most. |
| `boolean Hand.isBust()`; `boolean isBlackjack()`; `String toString()` | `total() > 21`, O(k); two cards and `total() == 21`, O(1); e.g. `A♠ K♦ (21)`, O(k). |
| `Bankroll(int deposit)` | Sets balance; `IllegalArgumentException` if `deposit <= 0`. O(1). |
| `int Bankroll.placeBet(int amount)` | Subtracts and returns `amount`; `IllegalArgumentException` if `<= 0` or `> balance`. O(1). |
| `void Bankroll.credit(int amount)` | Adds a payout; 0 allowed; `IllegalArgumentException` if negative. O(1). |
| `int Bankroll.balance()`; `boolean isBroke()` | Current balance; `balance == 0`. O(1). |
| `void GameHistory.record(RoundResult r)` | `rounds.add(r)`. O(1). |
| `int GameHistory.wins()`; `int losses()`; `double winRate()` | Count of `WIN`+`BLACKJACK`; count of `LOSS`+`BUST`; `wins / rounds.size()`. All zero when empty; no division by zero. O(n) each: walks `toList()`. |
| `List<RoundResult> GameHistory.all()` | `rounds.toList()`. O(n). |
| `void CardHistory.record(Card c)`; `List<Card> all()` | `seen.add(c)`, O(1); `seen.toList()`, O(n). |
| `void CardCounter.observe(Card c)` | Hi-Lo: 2..6 adds 1, 7..9 adds 0, 10/J/Q/K/A subtracts 1. O(1). |
| `double CardCounter.trueCount(int cardsRemaining)` | `runningCount / max(0.5, cardsRemaining / 52.0)`; the half-deck floor stops the division inflating the count near the cut card (32 cards is 0.6 decks). O(1). |
| `int CardCounter.suggestedUnits(int cardsRemaining)` | 1 when the true count is below 2, else `floor(trueCount) - 1`. O(1). |
| `int ConsoleUI.promptInt(String prompt)` | Reads a line as an `int`; re-prompts on `NumberFormatException` until it has one. O(1) per attempt. |
| `Action ConsoleUI.promptAction(boolean canDouble)` | Accepts `hit`/`h`, `stand`/`s`, and `double`/`d` only if `canDouble`; case-insensitive, trimmed; re-prompts otherwise. O(1) per attempt. |
| `boolean ConsoleUI.promptYesNo(String prompt)`; `void show(String s)` | Accepts `y`/`yes`/`n`/`no`, re-prompts otherwise; prints one line. O(1) per attempt; O(1). |
| `void Game.run()` | Deposit; rounds until `isBroke()` or the player says no; print the history. A round: new `Shoe`, `CardHistory` and `CardCounter` if `needsShuffle()`; show the count advice; bet; deal two each; check blackjack; player turn; dealer hits while `total() < 17`; settle; `history.record`. O(r·n) over r rounds, from the per-round history line. |

Settlement, with `bet` already subtracted by `placeBet` (doubled after a double down):

| Outcome | When | `credit` | `netChange` |
|---|---|---|---|
| `BLACKJACK` | Player's two cards total 21, dealer's do not | `bet + bet * 3 / 2` | `+bet * 3 / 2` |
| `BUST` | Player over 21; dealer does not draw | 0 | `-bet` |
| `WIN` | Dealer busts, or player's total is higher | `2 * bet` | `+bet` |
| `PUSH` | Equal totals, or both blackjack | `bet` | 0 |
| `LOSS` | Dealer's total is higher | 0 | `-bet` |

## 5. Where validation lives

| Bad input (from the scope) | Caught where, what happens, and why there |
|---|---|
| Bet of zero or negative; bet larger than the balance | `Bankroll.placeBet` throws `IllegalArgumentException("bet must be greater than zero")` or `("bet exceeds balance of $X")`; `Game.run` catches, shows the message, calls `promptInt` again. Only `Bankroll` knows the balance, and the same method takes the second bet of a double down, so the rule is written once. |
| Deposit of zero or negative | `Bankroll(int)` throws `IllegalArgumentException("deposit must be greater than zero")`; `Game.run` re-prompts. The balance is private to `Bankroll`. |
| Non-numeric text where a number is expected | `ConsoleUI.promptInt` catches `NumberFormatException`, prints `"Please enter a whole number."`, loops. The text has not become a number yet, so no other class can see it. |
| Anything but hit/stand/double, or `double` when not allowed (more than two cards, or balance below the bet) | `ConsoleUI.promptAction` prints `"Type hit, stand"` plus `" or double"` when allowed, and re-prompts; parsing belongs to the UI. `Game.run` computes `canDouble` because only it knows both hand size and balance; if a caller skipped the flag, `placeBet` would still throw. |
| Hitting when no cards remain | `Game.run` checks `needsShuffle()` before every round, so no player can hit into an empty shoe; the round loop owns the moment to reshuffle. `Shoe.deal()` on an empty chain throws `IllegalStateException("shoe is empty")`, left uncaught: reaching it means the cut-card arithmetic is wrong and the program should stop loudly. |
| Balance reaches zero | `Game.run` checks `isBroke()` after each round, prints `"You are out of money."` and the history, and ends; only `Game` knows a round is over. No mid-game deposit, since the scope says such a player cannot play. |
| `add(null)`; `remove()` on empty; `remove(T)` of an absent entry | `LinkedChain` throws `NullPointerException`; returns `null`; returns `false` with no change. Null would break `equals` in the search methods. Absence is an ordinary answer; an exception would make every caller walk the chain twice. |

## 6. Test plan

One normal case and one bad-input case per public method, written as input, then expected result. "Chain A♠, A♠, K♥" means those three added in that order, so K♥ is the head.

**`LinkedChain`.** `add`: A♠ into empty, `size()` 1 and `contains(A♠)` true; `add(null)`, `NullPointerException` and size still 0. `remove()`: chain A♠, A♠, K♥ returns K♥ with size 2; empty chain returns `null` with size 0. `remove(T)`: `remove(A♠)` on that chain is true and `count(A♠)` becomes 1; `remove(Q♦)`, which is not there, is false and size stays 3. `contains`: `contains(K♥)` true; on an empty chain false. `count`: `count(A♠)` is 2; `count(Q♦)` is 0. `size`: after three adds, 3; after one add and one `remove()`, 0. `toList`: that chain gives a list of 3 starting with K♥; an empty chain gives an empty list, not null.

**`Shoe`.** Constructor: `new Shoe(3, new Random(42))` has 156 remaining and each distinct card appears 3 times; `new Shoe(0, rng)` throws `IllegalArgumentException`. `deal`: a fresh shoe returns a `Card` and 155 remain; the 157th call throws `IllegalStateException`. `cardsRemaining` and `needsShuffle`: after 10 deals, 146 and false; after 125 deals, 31 and true (32 is false).

**`Hand`.** `add`: 7♣ into an empty hand, `total()` 7; `add(null)`, `NullPointerException`. `total`: A♠, 9♦ is 20; A♠, 9♦, 5♣ is 15 and an empty hand is 0. `isBust`: 10♠, 9♦, 5♣ true; 10♠, 9♦, 2♣ false. `isBlackjack`: A♠, K♦ true; 7♠, 7♦, 7♣ false (21 with three cards). `toString`: A♠, K♦ contains `21`; an empty hand contains `0` and throws nothing.

**`Bankroll`.** Constructor: `new Bankroll(100)` has balance 100; `new Bankroll(0)` throws `IllegalArgumentException`. `placeBet`: from 100, `placeBet(30)` returns 30 and leaves 70; `placeBet(130)` throws with a message naming 100 and the balance is unchanged, as does `placeBet(-1)`. `credit`: from 70, `credit(60)` gives 130; `credit(-10)` throws. `balance` and `isBroke`: balance 1 is not broke; `placeBet(100)` from 100 leaves `isBroke()` true.

**`GameHistory`.** `record` and `all`: one record gives `all()` of size 1; the same `RoundResult` recorded twice gives 2 (duplicates allowed), and an empty history gives an empty list. `wins`, `losses`, `winRate`: WIN, BLACKJACK, LOSS, BUST, PUSH give 2, 2 and 0.4; an empty history gives 0, 0 and 0.0 with no `ArithmeticException`.

**`CardHistory`.** `record` and `all`: K♥ once gives `all()` of size 1; K♥ three times gives 3, and an empty history gives an empty list.

**`CardCounter`.** `observe`: 5♣, K♥, 8♦, 2♠, 3♠ give a running count of +2, so `trueCount(104)` is 1.0; an ace gives −1, not 0. `trueCount`: running +6 with 104 left is 3.0; running +4 with 10 left is 8.0 by the half-deck floor, not 20.8. `suggestedUnits`: true count 4 gives 3 units; true count −3 gives 1, never 0.

**`ConsoleUI`** (a `Scanner` over a prepared string). `promptInt`: `25` returns 25; `ten`, a blank line, `5.5`, then `25` prints the whole-number message three times and returns 25. `promptAction`: `HIT` with `canDouble` true returns `HIT`; `double`, `split`, ` s ` with `canDouble` false re-prompts twice and returns `STAND`. `promptYesNo`: `y` is true; `maybe` then `no` re-prompts once and is false. `show`: `show("hi")` prints `hi`; `show("")` prints a bare newline.

**`Game.run`** (seeded `Random`, scripted input). Deposit 100, bet 10, stand, no: one `RoundResult` is recorded and the final balance equals the value recorded once by hand for seed 42. Deposit `-50` then `100`: the deposit message prints once and play proceeds. Deposit 10, bet 10 on a losing seed: prints `You are out of money.` and the history, with no "play again" prompt.

## 7. Revised scope

| Change | Why, and where it originated |
|---|---|
| "hold or stand" becomes "hit or stand". | Typo; the MVP list already said hit. Own reflection. |
| A three-deck shoe of 156 cards, where the scope said both 52 and three decks. | Three decks gives the chain its duplicates and makes counting worth doing. Group discussion. |
| Card counting is the one stretch goal; ASCII art, splitting, the cheating dealer and multiple players are dropped. | Time for one. Counting adds one class and one line in `draw()`; splitting would change `Hand`, `Bankroll` and settlement; multiple players would change nearly every class. Group discussion. |
| Counting method is Hi-Lo with a true count and a "true count minus one" bet ramp. | The scope said we had not studied the mathematics. Hi-Lo is balanced (plus and minus cards cancel over a full deck) and needs one integer of state. GenAI, during drafting. |
| Payouts (1:1, 3:2, push) and double down are MVP. | The scope's first paragraph promised them; the MVP's money rules mean nothing without a payout rule. Own reflection. |
| A third chain instance, `CardHistory`, holds dealt cards. | The MVP already required tracking every used card; it is a third place where duplicates must be allowed. GenAI, during drafting. |
| Shuffle in an `ArrayList` before loading the chain; no `shuffle()` on the chain. | Resolves the scope's open question without putting game logic in the chain. GenAI, during drafting. |
| Cut card at 32; whole-dollar money; session ends at zero balance; `toList()` replaces `toArray()`. | In order: makes "hit with no cards left" unreachable; keeps prompts integer; the scope says a broke player cannot play; removes the only unchecked cast. GenAI, during drafting. |

## 8. Work attribution

[Fill in names. Rows marked GenAI-drafted were produced by a coding assistant from the scope and the instructions; the named person reads, corrects and owns that section before submission.]

| Section | Team member(s) | Contribution (how) |
|---|---|---|
| Class design, data and state | [Name] | GenAI-drafted; cut-card arithmetic [verified by hand by name] |
| System diagram | [Name] | GenAI-drafted in Mermaid, rendered to PNG; [checked against the tables by name] |
| Generic contract | [Name] | GenAI-drafted; [reviewed by name] |
| Method signatures and Big-O | [Name] | GenAI-drafted; Big-O [verified by hand by name] |
| Where validation lives | [Name] | GenAI-drafted; [reviewed by name] |
| Test plan | [Name] | GenAI-drafted; [reviewed by name] |
| Revised scope | [Name] | Card-counting decision from group discussion; table GenAI-drafted; [reviewed by name] |
| Attribution and GenAI reflection | [Name] | [Written by name] |

**How we used GenAI.** After we decided as a group to build the card counting stretch goal, we gave a coding assistant (Claude) our scope document and the assignment instructions and asked it to draft this specification. It proposed the class split, Hi-Lo, the cut card and its arithmetic, the `toList()` change, the exception messages and the test cases. [State what the team changed after reading the draft, and which Big-O estimates and tests were checked by hand.] The scope document was written without GenAI.
