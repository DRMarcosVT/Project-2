import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for RoundResult's null check and toString().
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class RoundResultTest
{
    // ---- constructor ----

    /** Normal: the record keeps its four parts. */
    @Test
    public void constructorKeepsParts()
    {
        RoundResult result = new RoundResult(2, Outcome.PUSH, 25, 0);
        assertEquals(2, result.round());
        assertEquals(Outcome.PUSH, result.outcome());
        assertEquals(25, result.bet());
        assertEquals(0, result.netChange());
    }

    /** Bad input: a round must have an outcome. */
    @Test
    public void constructorRejectsNullOutcome()
    {
        assertThrows(NullPointerException.class,
            () -> new RoundResult(1, null, 10, 10));
    }

    // ---- toString() ----

    /** Normal: a win prints with a plus sign. */
    @Test
    public void toStringShowsWinWithPlus()
    {
        RoundResult result = new RoundResult(3, Outcome.WIN, 10, 10);
        assertEquals("Round 3: WIN, bet $10, net +$10", result.toString());
    }

    /** Edge: a loss prints with a minus sign in front of the dollar sign. */
    @Test
    public void toStringShowsLossWithMinus()
    {
        RoundResult result = new RoundResult(4, Outcome.BUST, 10, -10);
        assertEquals("Round 4: BUST, bet $10, net -$10", result.toString());
    }
}
