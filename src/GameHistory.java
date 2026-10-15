import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Every finished round of the session, kept in a LinkedChain so the
 * player can review them and see the win rate.
 *
 * The chain stores RoundResult records. Order does not matter for the
 * win rate, and the same result can appear many times, which is why an
 * unordered chain that allows duplicates fits.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class GameHistory
{
    private final LinkedChain<RoundResult> rounds = new LinkedChain<>();
    private final LinkedChain<String> summaries = new LinkedChain<>();

    /**
     * Stores one finished round. LinkedChain.add() puts it at the head, so
     * the newest round comes first in all().
     *
     * @param result the round to remember
     * @throws NullPointerException if result is null
     */
    public void record(RoundResult result)
    {
        rounds.add(Objects.requireNonNull(result, "result must not be null"));
        summaries.add(result.toString());
    }

    /**
     * Gets every recorded round, newest first.
     *
     * @return a list of the rounds; empty, never null, when nothing has
     *         been recorded
     */
    public List<RoundResult> all()
    {
        RoundResult[] array = new RoundResult[rounds.size()];
        return Arrays.asList(rounds.toArray(array));
    }

    /**
     * Gets display summaries, newest first, as a separate snapshot.
     *
     * @return the recorded summary strings, or an empty list; O(n)
     */
    public List<String> summaries()
    {
        return Arrays.asList(summaries.toArray(new String[summaries.size()]));
    }

    /**
     * Counts the rounds the player won, including blackjacks.
     *
     * @return the number of WIN and BLACKJACK outcomes
     */
    public int wins()
    {
        int count = 0;
        for (RoundResult result : all())
        {
            if (result.outcome() == Outcome.WIN
                || result.outcome() == Outcome.BLACKJACK)
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the rounds the player lost, including busts.
     *
     * @return the number of LOSS and BUST outcomes
     */
    public int losses()
    {
        int count = 0;
        for (RoundResult result : all())
        {
            if (result.outcome() == Outcome.LOSS
                || result.outcome() == Outcome.BUST)
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the share of recorded rounds that were wins. Pushes count as
     * rounds played but not as wins.
     *
     * @return wins divided by rounds, or 0.0 when no rounds are recorded
     */
    public double winRate()
    {
        if (rounds.isEmpty())
        {
            return 0.0;
        }
        return (double) wins() / rounds.size();
    }
}
