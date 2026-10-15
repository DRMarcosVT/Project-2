import java.util.Objects;

/**
 * Everything worth remembering about one finished round. GameHistory
 * stores these in a LinkedChain so the player can review them at the end.
 *
 * @param round     the round number, starting at 1
 * @param outcome   how the round ended, never null
 * @param bet       the dollars the player put down
 * @param netChange the change to the balance: positive for a win, zero for
 *                  a push, negative for a loss or bust
 *
 * @author Marcos Salas, Madeline Mehfoud, David Moon, Shreyas
 * @version October 2026
 */
public record RoundResult(int round, Outcome outcome, int bet, int netChange)
{
    /**
     * Checks that the outcome is present.
     *
     * @throws NullPointerException if outcome is null
     */
    public RoundResult
    {
        Objects.requireNonNull(outcome, "outcome must not be null");
    }

    /**
     * Prints the round on one line, e.g. "Round 3: WIN, bet $10, net +$10".
     *
     * @return a one-line summary of the round
     */
    @Override
    public String toString()
    {
        String sign = netChange < 0 ? "-$" : "+$";
        return "Round " + round + ": " + outcome + ", bet $" + bet
            + ", net " + sign + Math.abs(netChange);
    }
}
