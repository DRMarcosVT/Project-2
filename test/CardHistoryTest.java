import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for CardHistory: record() and all(), including an empty history.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class CardHistoryTest
{
    private static final Card KING_H = new Card(Rank.KING, Suit.HEARTS);
    private static final Card ACE_S = new Card(Rank.ACE, Suit.SPADES);

    // ---- record() ----

    /** Normal: one card is stored. */
    @Test
    public void recordStoresCard()
    {
        CardHistory history = new CardHistory();
        history.record(KING_H);
        assertEquals(1, history.all().size());
        assertEquals(KING_H, history.all().get(0));
    }

    /** Bad input: null is rejected and nothing is stored. */
    @Test
    public void recordRejectsNull()
    {
        CardHistory history = new CardHistory();
        assertThrows(NullPointerException.class, () -> history.record(null));
        assertEquals(0, history.all().size());
    }

    // ---- all() ----

    /** Normal: the same card three times gives three entries, newest first. */
    @Test
    public void allKeepsDuplicatesNewestFirst()
    {
        CardHistory history = new CardHistory();
        history.record(ACE_S);
        history.record(KING_H);
        history.record(KING_H);
        history.record(KING_H);
        assertEquals(4, history.all().size());
        assertEquals(KING_H, history.all().get(0));
        assertEquals(ACE_S, history.all().get(3));
    }

    /** Edge: a fresh history gives an empty list, never null. */
    @Test
    public void allOfEmptyHistoryIsEmptyList()
    {
        CardHistory history = new CardHistory();
        assertNotNull(history.all());
        assertTrue(history.all().isEmpty());
    }
}
