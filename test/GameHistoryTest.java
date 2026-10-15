import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for GameHistory: record(), all(), wins(), losses() and winRate(),
 * including the empty-collection cases.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class GameHistoryTest
{
    private static final RoundResult WIN = new RoundResult(1, Outcome.WIN, 10, 10);
    private static final RoundResult BLACKJACK = new RoundResult(2, Outcome.BLACKJACK, 10, 15);
    private static final RoundResult LOSS = new RoundResult(3, Outcome.LOSS, 10, -10);
    private static final RoundResult BUST = new RoundResult(4, Outcome.BUST, 10, -10);
    private static final RoundResult PUSH = new RoundResult(5, Outcome.PUSH, 10, 0);

    /** A history holding one of each outcome. */
    private static GameHistory fiveRounds()
    {
        GameHistory history = new GameHistory();
        history.record(WIN);
        history.record(BLACKJACK);
        history.record(LOSS);
        history.record(BUST);
        history.record(PUSH);
        return history;
    }

    // ---- record() ----

    /** Normal: one record is stored and comes back. */
    @Test
    public void recordStoresRound()
    {
        GameHistory history = new GameHistory();
        history.record(WIN);
        assertEquals(1, history.all().size());
        assertEquals(WIN, history.all().get(0));
    }

    /** Bad input: null is rejected and nothing is stored. */
    @Test
    public void recordRejectsNull()
    {
        GameHistory history = new GameHistory();
        assertThrows(NullPointerException.class, () -> history.record(null));
        assertEquals(0, history.all().size());
    }

    // ---- all() ----

    /** Normal: duplicates are kept, and the newest round comes first. */
    @Test
    public void allKeepsDuplicatesNewestFirst()
    {
        GameHistory history = new GameHistory();
        history.record(WIN);
        history.record(WIN);
        history.record(LOSS);
        assertEquals(3, history.all().size());
        assertEquals(LOSS, history.all().get(0));
    }

    /** Edge: an empty history gives an empty list, never null. */
    @Test
    public void allOfEmptyHistoryIsEmptyList()
    {
        GameHistory history = new GameHistory();
        assertNotNull(history.all());
        assertTrue(history.all().isEmpty());
    }

    // ---- wins() ----

    /** Normal: WIN and BLACKJACK both count as wins. */
    @Test
    public void winsCountsWinAndBlackjack()
    {
        assertEquals(2, fiveRounds().wins());
    }

    /** Edge: nothing recorded means zero wins. */
    @Test
    public void winsOfEmptyHistoryIsZero()
    {
        assertEquals(0, new GameHistory().wins());
    }

    // ---- losses() ----

    /** Normal: LOSS and BUST both count as losses. */
    @Test
    public void lossesCountsLossAndBust()
    {
        assertEquals(2, fiveRounds().losses());
    }

    /** Edge: nothing recorded means zero losses. */
    @Test
    public void lossesOfEmptyHistoryIsZero()
    {
        assertEquals(0, new GameHistory().losses());
    }

    // ---- winRate() ----

    /** Normal: two wins in five rounds is 0.4. */
    @Test
    public void winRateIsWinsOverRounds()
    {
        assertEquals(0.4, fiveRounds().winRate(), 1e-9);
    }

    /** Edge: an empty history is 0.0 with no division by zero. */
    @Test
    public void winRateOfEmptyHistoryIsZero()
    {
        assertEquals(0.0, new GameHistory().winRate(), 1e-9);
    }
}
