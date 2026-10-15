import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Public API checks against the unchanged starter, including the D2 test plan. */
public class LinkedChainTest
{
    private static final Card ACE = new Card(Rank.ACE, Suit.SPADES);
    private static final Card KING = new Card(Rank.KING, Suit.HEARTS);
    private static final Card QUEEN = new Card(Rank.QUEEN, Suit.DIAMONDS);

    private LinkedChain<Card> cards()
    {
        LinkedChain<Card> chain = new LinkedChain<>();
        chain.add(ACE);
        chain.add(ACE);
        chain.add(KING);
        return chain;
    }

    @Test
    public void addStoresDuplicatesAndRejectsNull()
    {
        LinkedChain<Card> chain = cards();
        assertEquals(3, chain.size());
        assertEquals(2, chain.count(ACE));
        assertThrows(IllegalArgumentException.class, () -> chain.add(null));
        assertEquals(3, chain.size());
    }

    @Test
    public void removeHeadAndRemoveEmpty()
    {
        LinkedChain<Card> chain = cards();
        assertEquals(KING, chain.remove());
        assertEquals(ACE, chain.remove());
        assertEquals(ACE, chain.remove());
        assertNull(chain.remove());
        assertEquals(0, chain.size());
        assertTrue(chain.isEmpty());
    }

    @Test
    public void removeValueRemovesOnlyOneAndPreservesAbsentEntries()
    {
        LinkedChain<Card> chain = cards();
        assertFalse(chain.remove(QUEEN));
        assertEquals(3, chain.size());
        assertArrayEquals(new Card[] {KING, ACE, ACE}, chain.toArray(new Card[3]));
        assertTrue(chain.remove(new Card(Rank.ACE, Suit.SPADES)));
        assertEquals(1, chain.count(ACE));
        assertTrue(chain.remove(KING));
        assertTrue(chain.remove(ACE));
        assertFalse(chain.remove(QUEEN));
        assertFalse(chain.remove(null));
    }

    @Test
    public void containsCountSizeAndEmptyFollowState()
    {
        LinkedChain<Card> chain = cards();
        assertFalse(chain.isEmpty());
        assertTrue(chain.contains(KING));
        assertFalse(chain.contains(QUEEN));
        assertEquals(0, chain.count(QUEEN));
        chain.clear();
        assertEquals(0, chain.size());
        assertTrue(chain.isEmpty());
        assertFalse(chain.contains(ACE));
        assertFalse(chain.contains(null));
        assertEquals(0, chain.count(ACE));
        assertEquals(0, chain.count(null));
        chain.clear();
        assertTrue(chain.isEmpty());
    }

    @Test
    public void addToEndPreservesOrderAndRejectsNull()
    {
        LinkedChain<Card> chain = new LinkedChain<>();
        assertTrue(chain.addToEnd(ACE));
        assertTrue(chain.addToEnd(KING));
        assertThrows(IllegalArgumentException.class, () -> chain.addToEnd(null));
        assertArrayEquals(new Card[] {ACE, KING}, chain.toArray(new Card[2]));
    }

    @Test
    public void getReadsWithoutRemovalAndRejectsInvalidIndices()
    {
        LinkedChain<Card> chain = cards();
        assertEquals(KING, chain.get(0));
        assertEquals(ACE, chain.get(2));
        assertEquals(3, chain.size());
        assertThrows(IndexOutOfBoundsException.class, () -> chain.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> chain.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> new LinkedChain<Card>().get(0));
    }

    @Test
    public void toArrayCopiesEntriesAndChecksCapacity()
    {
        LinkedChain<Card> chain = cards();
        Card[] destination = new Card[3];
        assertSame(destination, chain.toArray(destination));
        assertArrayEquals(new Card[] {KING, ACE, ACE}, destination);
        assertThrows(IllegalArgumentException.class, () -> chain.toArray(new Card[2]));
        assertThrows(IllegalArgumentException.class, () -> chain.toArray(null));
        assertArrayEquals(new Card[0], new LinkedChain<Card>().toArray(new Card[0]));
    }

    @Test
    public void toStringListsEntriesAndHandlesEmpty()
    {
        assertEquals("[KH, AS, AS]", cards().toString());
        assertEquals("[]", new LinkedChain<Card>().toString());
    }

    @Test
    public void sameGenericClassStoresAndRetrievesRoundResults()
    {
        LinkedChain<RoundResult> rounds = new LinkedChain<>();
        RoundResult result = new RoundResult(1, Outcome.WIN, 5, 5);
        assertTrue(rounds.add(result));
        assertEquals(result, rounds.toArray(new RoundResult[1])[0]);
        assertEquals(result, rounds.remove());
        assertNull(rounds.remove());
        assertEquals(KING, cards().remove());
    }
}
