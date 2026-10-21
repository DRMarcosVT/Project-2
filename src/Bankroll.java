/**
 * The player's money, in whole dollars, and the rules for changing it.
 *
 * Every path that changes the balance (deposit, bet, payout) goes through
 * this class, so the money rules from the scope are checked in exactly
 * one place: a deposit must be positive, and a bet must be between 1 and
 * the current balance.
 *
 * @author Team
 * @version October 2026
 */
public class Bankroll
{
    private int balance;  // current money in whole dollars, never negative

    /**
     * Opens a bankroll with a first deposit.
     *
     * @param deposit the starting money in whole dollars
     * @throws IllegalArgumentException if deposit is zero or negative
     */
    public Bankroll(int deposit)
    {
        if (deposit <= 0)
        {
            throw new IllegalArgumentException(
                "Deposit must be greater than zero");
        }
        balance = deposit;
    }

    /**
     * Takes a bet out of the balance.
     *
     * @param amount the bet in whole dollars
     * @return the amount bet, so the caller can keep it for settlement
     * @throws IllegalArgumentException if amount is zero or negative, or
     *         larger than the balance, or a possible payout exceeds int range;
     *         the balance is unchanged
     */
    public int placeBet(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException(
                "Bet must be greater than zero");
        }
        if (amount > balance)
        {
            throw new IllegalArgumentException(
                "Bet of $" + amount + " exceeds balance of $" + balance);
        }
        // Reject before deducting: even a blackjack must fit both int APIs.
        long payout = amount + amount * 3L / 2;
        if (payout > Integer.MAX_VALUE
            || balance - (long) amount + payout > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Bet could exceed whole-dollar integer limit");
        }
        balance -= amount;
        return amount;
    }

    /**
     * Pays money back into the balance after a round.
     *
     * @param amount the payout in whole dollars; 0 is allowed for a loss
     * @throws IllegalArgumentException if amount is negative or the
     *         balance would overflow
     */
    public void credit(int amount)
    {
        if (amount < 0)
        {
            throw new IllegalArgumentException(
                "Credit must not be negative, got " + amount);
        }
        if ((long) balance + amount > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Credit exceeds whole-dollar integer limit");
        }
        balance += amount;
    }

    /**
     * Gets the current balance.
     *
     * @return the balance in whole dollars
     */
    public int balance()
    {
        return balance;
    }

    /**
     * Says whether the player has run out of money.
     *
     * @return true when the balance is exactly zero
     */
    public boolean isBroke()
    {
        return balance == 0;
    }
}
