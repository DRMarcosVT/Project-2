import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for Rank and Card: the hand-written methods value(), label(),
 * the Card constructor's null check and Card.toString().
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class CardTest
{
    // ---- Rank.value() ----

    /** Normal: an ace scores 11. */
    @Test
    public void valueOfAceIsEleven()
    {
        assertEquals(11, Rank.ACE.value());
    }

    /** Edge: every face card scores 10, the same as a ten. */
    @Test
    public void valueOfFaceCardsIsTen()
    {
        assertEquals(10, Rank.JACK.value());
        assertEquals(10, Rank.QUEEN.value());
        assertEquals(10, Rank.KING.value());
        assertEquals(10, Rank.TEN.value());
    }

    // ---- Rank.label() ----

    /** Normal: a number card's label is its number. */
    @Test
    public void labelOfTenIsTwoCharacters()
    {
        assertEquals("10", Rank.TEN.label());
    }

    /** Edge: face cards and the ace use one letter. */
    @Test
    public void labelOfAceIsA()
    {
        assertEquals("A", Rank.ACE.label());
        assertEquals("K", Rank.KING.label());
    }

    // ---- Card constructor ----

    /** Normal: a card keeps its parts, and equal parts make equal cards. */
    @Test
    public void constructorKeepsRankAndSuit()
    {
        Card card = new Card(Rank.ACE, Suit.SPADES);
        assertEquals(Rank.ACE, card.rank());
        assertEquals(Suit.SPADES, card.suit());
        assertEquals(new Card(Rank.ACE, Suit.SPADES), card);
    }

    /** Bad input: a missing rank or suit is rejected. */
    @Test
    public void constructorRejectsNull()
    {
        assertThrows(NullPointerException.class,
            () -> new Card(null, Suit.SPADES));
        assertThrows(NullPointerException.class,
            () -> new Card(Rank.ACE, null));
    }

    // ---- Card.toString() ----

    /** Normal: rank label then the first letter of the suit. */
    @Test
    public void toStringIsLabelPlusSuitLetter()
    {
        assertEquals("AS", new Card(Rank.ACE, Suit.SPADES).toString());
        assertEquals("KD", new Card(Rank.KING, Suit.DIAMONDS).toString());
    }

    /** Edge: a ten prints as three characters. */
    @Test
    public void toStringOfTenHasThreeCharacters()
    {
        assertEquals("10H", new Card(Rank.TEN, Suit.HEARTS).toString());
    }
}
