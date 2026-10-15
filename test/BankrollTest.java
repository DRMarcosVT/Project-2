import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for Bankroll: the constructor, placeBet(), credit(), balance() and
 * isBroke(). These are the money rules from the scope.
 *
 * @author Marcos Salas, Madeline Mehfoud, David Moon, Shreyas
 * @version October 2026
 */
public class BankrollTest
{
    // ---- constructor ----

    /** Normal: the deposit becomes the balance. */
    @Test
    public void constructorSetsBalance()
    {
        assertEquals(100, new Bankroll(100).balance());
    }

    /** Bad input: zero or negative deposits are rejected. */
    @Test
    public void constructorRejectsZeroAndNegativeDeposit()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new Bankroll(0));
        assertEquals("Deposit must be greater than zero", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new Bankroll(-5));
    }

    // ---- placeBet() ----

    /** Normal: the bet comes out of the balance and is returned. */
    @Test
    public void placeBetSubtractsAndReturnsAmount()
    {
        Bankroll bankroll = new Bankroll(100);
        assertEquals(30, bankroll.placeBet(30));
        assertEquals(70, bankroll.balance());
    }

    /** Bad input: too large, zero or negative; the balance never changes. */
    @Test
    public void placeBetRejectsBadAmounts()
    {
        Bankroll bankroll = new Bankroll(100);
        IllegalArgumentException tooBig = assertThrows(
            IllegalArgumentException.class, () -> bankroll.placeBet(130));
        assertTrue(tooBig.getMessage().contains("100"));
        assertThrows(IllegalArgumentException.class, () -> bankroll.placeBet(0));
        assertThrows(IllegalArgumentException.class, () -> bankroll.placeBet(-1));
        assertEquals(100, bankroll.balance());
    }

    // ---- credit() ----

    /** Normal: a payout is added; zero is allowed for a lost round. */
    @Test
    public void creditAddsPayout()
    {
        Bankroll bankroll = new Bankroll(70);
        bankroll.credit(60);
        assertEquals(130, bankroll.balance());
        bankroll.credit(0);
        assertEquals(130, bankroll.balance());
    }

    /** Bad input: a negative credit is rejected. */
    @Test
    public void creditRejectsNegative()
    {
        Bankroll bankroll = new Bankroll(70);
        assertThrows(IllegalArgumentException.class, () -> bankroll.credit(-10));
        assertEquals(70, bankroll.balance());
    }

    // ---- balance() ----

    /** Normal: balance follows a bet and a payout. */
    @Test
    public void balanceTracksBetAndCredit()
    {
        Bankroll bankroll = new Bankroll(100);
        bankroll.placeBet(10);
        bankroll.credit(20);
        assertEquals(110, bankroll.balance());
    }

    /** Edge: a bet of the whole balance leaves exactly zero. */
    @Test
    public void balanceCanReachZero()
    {
        Bankroll bankroll = new Bankroll(100);
        bankroll.placeBet(100);
        assertEquals(0, bankroll.balance());
    }

    // ---- isBroke() ----

    /** Normal: one dollar left is not broke. */
    @Test
    public void isBrokeFalseWithOneDollar()
    {
        assertFalse(new Bankroll(1).isBroke());
    }

    /** Edge: betting everything and losing is broke. */
    @Test
    public void isBrokeTrueAtZero()
    {
        Bankroll bankroll = new Bankroll(100);
        bankroll.placeBet(100);
        bankroll.credit(0);
        assertTrue(bankroll.isBroke());
    }

    @Test
    public void creditRejectsOverflowWithoutChangingBalance()
    {
        Bankroll money = new Bankroll(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> money.credit(1));
        assertEquals(Integer.MAX_VALUE, money.balance());
    }

    @Test
    public void placeBetRejectsUnrepresentablePayoutBeforeDeduction()
    {
        Bankroll money = new Bankroll(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> money.placeBet(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, money.balance());
        Bankroll nearLimit = new Bankroll(Integer.MAX_VALUE - 1);
        assertEquals(1, nearLimit.placeBet(1));
        nearLimit.credit(Game.winnings(Outcome.BLACKJACK, 1));
        assertEquals(Integer.MAX_VALUE, nearLimit.balance());
    }

}
