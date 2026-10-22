import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Tests for Game: the constructor, run(), settle(), winnings() and main().
 *
 * run() is tested with a seeded Random and a script of typed lines. The
 * seeds were chosen by running the game once and reading the result:
 * with seed 1, standing on the first hand wins; with seed 2 it loses.
 * The script "100, 10, s, n" means deposit $100, bet $10, stand, and
 * answer no to another round.
 *
 * @author Team
 * @version October 2026
 */
public class GameTest
{
    private static final Card ACE_S = new Card(Rank.ACE, Suit.SPADES);
    private static final Card KING_D = new Card(Rank.KING, Suit.DIAMONDS);
    private static final Card TEN_S = new Card(Rank.TEN, Suit.SPADES);
    private static final Card NINE_D = new Card(Rank.NINE, Suit.DIAMONDS);
    private static final Card EIGHT_C = new Card(Rank.EIGHT, Suit.CLUBS);
    private static final Card SEVEN_C = new Card(Rank.SEVEN, Suit.CLUBS);
    private static final Card FIVE_C = new Card(Rank.FIVE, Suit.CLUBS);

    /** Builds a hand from the given cards in order. */
    private static Hand handOf(Card... cards)
    {
        Hand hand = new Hand();
        for (Card card : cards)
        {
            hand.add(card);
        }
        return hand;
    }

    /** Plays a whole session from a script and returns everything printed. */
    private static String play(int seed, String typed)
    {
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        ConsoleUI ui = new ConsoleUI(new Scanner(typed), new PrintStream(printed, true));
        new Game(ui, new Random(seed)).run();
        return printed.toString();
    }

    /** Counts how many times a message appears in the output. */
    private static int timesIn(String text, String message)
    {
        int count = 0;
        int at = text.indexOf(message);
        while (at >= 0)
        {
            count++;
            at = text.indexOf(message, at + 1);
        }
        return count;
    }

    // ---- constructor ----

    /** Normal: a game can be built from a UI and a Random. */
    @Test
    public void constructorAcceptsUiAndRandom()
    {
        ConsoleUI ui = new ConsoleUI(new Scanner(""), System.out);
        assertTrue(new Game(ui, new Random(1)) != null);
    }

    /** Bad input: either part missing is rejected. */
    @Test
    public void constructorRejectsNull()
    {
        ConsoleUI ui = new ConsoleUI(new Scanner(""), System.out);
        assertThrows(NullPointerException.class, () -> new Game(null, new Random(1)));
        assertThrows(NullPointerException.class, () -> new Game(ui, null));
    }

    // ---- run() ----

    /** Normal: one round, stand, quit; seed 1 wins and ends at $110. */
    @Test
    public void runPlaysOneRoundAndShowsHistory()
    {
        String out = play(1, "100\n10\ns\nn\n");
        assertTrue(out.contains("Round 1: WIN, bet $10, net +$10"), out);
        assertTrue(out.contains("Wins: 1  Losses: 0  Win rate: 100%"), out);
        assertTrue(out.contains("Final balance: $110"), out);
        assertTrue(out.contains("+----+ +----+\n|"), out);
    }

    /** Bad input: a negative deposit and an oversized bet are re-asked once each. */
    @Test
    public void runRepromptsBadDepositAndBadBet()
    {
        String out = play(1, "-50\n100\n500\n10\ns\nn\n");
        assertEquals(1, timesIn(out, "Deposit must be greater than zero"), out);
        assertEquals(1, timesIn(out, "Bet of $500 exceeds balance of $100"), out);
        assertTrue(out.contains("Final balance: $110"), out);
    }

    /** Edge: losing the whole balance ends the session with no "again" prompt. */
    @Test
    public void runStopsWhenBroke()
    {
        String out = play(2, "10\n10\ns\nn\n");
        assertTrue(out.contains("You are out of money."), out);
        assertTrue(out.contains("Final balance: $0"), out);
        assertFalse(out.contains("Play another round?"), out);
    }

    // ---- settle() ----

    /** Normal: every row of the outcome table. */
    @Test
    public void settleCoversEveryOutcome()
    {
        assertEquals(Outcome.BUST, Game.settle(handOf(TEN_S, NINE_D, FIVE_C), handOf(TEN_S, SEVEN_C)));
        assertEquals(Outcome.BLACKJACK, Game.settle(handOf(ACE_S, KING_D), handOf(TEN_S, NINE_D)));
        assertEquals(Outcome.PUSH, Game.settle(handOf(ACE_S, KING_D), handOf(KING_D, ACE_S)));
        assertEquals(Outcome.LOSS, Game.settle(handOf(TEN_S, NINE_D), handOf(ACE_S, KING_D)));
        assertEquals(Outcome.WIN, Game.settle(handOf(TEN_S, SEVEN_C), handOf(TEN_S, NINE_D, FIVE_C)));
        assertEquals(Outcome.WIN, Game.settle(handOf(TEN_S, NINE_D), handOf(TEN_S, EIGHT_C)));
        assertEquals(Outcome.PUSH, Game.settle(handOf(TEN_S, NINE_D), handOf(NINE_D, TEN_S)));
        assertEquals(Outcome.LOSS, Game.settle(handOf(TEN_S, SEVEN_C), handOf(TEN_S, NINE_D)));
    }

    /** Bad input: a missing hand is rejected. */
    @Test
    public void settleRejectsNullHand()
    {
        assertThrows(NullPointerException.class, () -> Game.settle(null, new Hand()));
        assertThrows(NullPointerException.class, () -> Game.settle(new Hand(), null));
    }

    // ---- winnings() ----

    /** Normal: the payout table, including the $5 blackjack paying $7 profit. */
    @Test
    public void winningsFollowPayoutTable()
    {
        assertEquals(12, Game.winnings(Outcome.BLACKJACK, 5));
        assertEquals(20, Game.winnings(Outcome.WIN, 10));
        assertEquals(10, Game.winnings(Outcome.PUSH, 10));
        assertEquals(0, Game.winnings(Outcome.LOSS, 10));
        assertEquals(0, Game.winnings(Outcome.BUST, 10));
    }

    /** Bad input: a zero bet or a missing outcome is rejected. */
    @Test
    public void winningsRejectsZeroBetAndNullOutcome()
    {
        assertThrows(IllegalArgumentException.class, () -> Game.winnings(Outcome.WIN, 0));
        assertThrows(NullPointerException.class, () -> Game.winnings(null, 10));
    }

    // ---- main() ----

    /**
     * Runs main() with the keyboard and screen swapped for a script and a
     * buffer. main() uses an unseeded Random, so the script only stands
     * and quits: if the first hand happens to be a blackjack there is no
     * hit-or-stand prompt, the "s" is re-asked by the yes/no prompt, and
     * the "n" still ends the session.
     */
    private static String runMain(String typed)
    {
        InputStream realIn = System.in;
        PrintStream realOut = System.out;
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        try
        {
            System.setIn(new ByteArrayInputStream(typed.getBytes()));
            System.setOut(new PrintStream(printed, true));
            Game.main(new String[0]);
        }
        finally
        {
            System.setIn(realIn);
            System.setOut(realOut);
        }
        return printed.toString();
    }

    /** Normal: main plays a session on System.in and System.out. */
    @Test
    public void mainRunsASession()
    {
        String out = runMain("100\n10\ns\nn\n");
        assertTrue(out.contains("--- Round 1 ---"), out);
        assertTrue(out.contains("Thanks for playing."), out);
    }

    /** Bad input: a word where the deposit should be is re-asked. */
    @Test
    public void mainSurvivesNonNumericDeposit()
    {
        String out = runMain("abc\n100\n10\ns\nn\n");
        assertTrue(out.contains("Please enter a whole number."), out);
        assertTrue(out.contains("Thanks for playing."), out);
    }

    @Test
    public void winningsRejectsNegativeAndOverflowingBets()
    {
        assertThrows(IllegalArgumentException.class, () -> Game.winnings(Outcome.WIN, -1));
        assertThrows(IllegalArgumentException.class,
            () -> Game.winnings(Outcome.BLACKJACK, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
            () -> Game.winnings(Outcome.WIN, Integer.MAX_VALUE));
        assertEquals(1250000000, Game.winnings(Outcome.BLACKJACK, 500000000));
    }

    @Test
    public void mainExitsCleanlyWhenInputEnds()
    {
        assertTrue(runMain("").contains("Input ended; goodbye."));
        assertTrue(runMain("100\n").contains("Final balance: $100"));
    }

    @Test
    public void runRefundsUnfinishedRoundOnInputEnd()
    {
        String out = play(1, "100\n10\n");
        assertTrue(out.contains("Unfinished round cancelled; bet returned."), out);
        assertTrue(out.contains("Final balance: $100"), out);
        assertFalse(out.contains("Round 1: WIN"), out);
    }

    @Test
    public void runRepromptsZeroAndNegativeBets()
    {
        String out = play(1, "100\n0\n-1\n10\ns\nn\n");
        assertEquals(2, timesIn(out, "Bet must be greater than zero"), out);
        assertTrue(out.contains("Final balance: $110"), out);
    }


    @Test
    public void runHitsToTwentyOneAndAutomaticallyStands()
    {
        // Seed 0: 7C + 10C + 4H = 21; dealer 9D + 4S + 9S busts.
        String out = play(0, "100\n10\nh\nn\n");
        assertTrue(out.contains("Your hand: 7C 10C 4H (21)"), out);
        assertEquals(1, timesIn(out, "Hit or stand?"), out);
        assertTrue(out.contains("Round 1: WIN"), out);
        assertTrue(out.contains("Final balance: $110"), out);
    }

    @Test
    public void runHitBustForfeitsBetWithoutDealerDraw()
    {
        String out = play(3, "100\n10\nh\nn\n");
        assertTrue(out.contains("Your hand: 7H QS 10S (27)"), out);
        assertTrue(out.contains("Dealer's hand: 6C 9H (15)"), out);
        assertTrue(out.contains("Round 1: BUST"), out);
        assertTrue(out.contains("Final balance: $90"), out);
    }

    @Test
    public void runSettlesDealerBlackjackWithoutActionPrompt()
    {
        String out = play(20, "100\n10\nn\n");
        assertFalse(out.contains("Hit or stand?"), out);
        assertTrue(out.contains("Round 1: LOSS"), out);
        assertTrue(out.contains("Final balance: $90"), out);
    }

    @Test
    public void runPaysOddBlackjackAndPushesBothNaturals()
    {
        String win = play(16, "100\n5\nn\n");
        assertTrue(win.contains("Round 1: BLACKJACK, bet $5, net +$7"), win);
        assertTrue(win.contains("Final balance: $107"), win);
        String push = play(48, "100\n10\nn\n");
        assertTrue(push.contains("Round 1: PUSH"), push);
        assertTrue(push.contains("Final balance: $100"), push);
    }

    @Test
    public void runDealerStandsOnSoftSeventeen()
    {
        String out = play(40, "100\n10\ns\nn\n");
        assertTrue(out.contains("Dealer's hand: 6D AS (17)"), out);
        assertTrue(out.contains("Round 1: LOSS"), out);
    }

    @Test
    public void runReshufflesAndResetsCardTrackersButKeepsRoundHistory()
    {
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        ConsoleUI ui = new ConsoleUI(new Scanner(""), new PrintStream(printed, true))
        {
            private int numberPrompts;
            private int rounds;

            @Override
            public int promptInt(String prompt)
            {
                return numberPrompts++ == 0 ? 1000 : 1;
            }

            @Override
            public Action promptAction()
            {
                return Action.STAND;
            }

            @Override
            public boolean promptYesNo(String prompt)
            {
                return ++rounds < 30;
            }
        };
        new Game(ui, new Random(1)).run();
        String out = printed.toString();
        assertTrue(out.contains("Fewer than 32 cards left: shuffling a new shoe."), out);
        assertTrue(timesIn(out, "Cards seen this shoe: 0, unseen: 156.") >= 2, out);
        assertTrue(out.contains("Cards seen this shoe: 5, unseen: 151."), out);
        String history = out.substring(out.indexOf("Rounds played (most recent first):"));
        assertEquals(30, timesIn(history, "  Round "), history);
        assertTrue(history.contains("  Round 1: WIN"), history);
        assertTrue(history.contains("  Round 30:"), history);
    }

    @Test
    public void runRejectsBetThatCouldOverflowAndAllowsRetry()
    {
        String out = play(1, "2147483646\n2147483646\n1\ns\nn\n");
        assertTrue(out.contains("Bet could exceed whole-dollar integer limit"), out);
        assertTrue(out.contains("Final balance: $2147483647"), out);
    }

    @Test
    public void runEndsAtMaximumDepositWithoutAskingForBet()
    {
        String out = play(1, "2147483647\n");
        assertTrue(out.contains("Balance limit reached"), out);
        assertTrue(out.contains("Final balance: $2147483647"), out);
        assertFalse(out.contains("Bet: "), out);
        assertFalse(out.contains("Input ended"), out);
    }

    @Test
    public void runEndsAfterWinReachesBalanceLimit()
    {
        String out = play(1, "2147483646\n1\ns\n");
        assertTrue(out.contains("Round 1: WIN"), out);
        assertTrue(out.contains("Balance limit reached"), out);
        assertTrue(out.contains("Final balance: $2147483647"), out);
        assertFalse(out.contains("Play another round?"), out);
        assertFalse(out.contains("Input ended"), out);
    }
}
