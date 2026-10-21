import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for Hand: add(), total(), isBust(), isBlackjack() and toString().
 *
 * @author Team
 * @version October 2026
 */
public class HandTest
{
    private static final Card ACE_S = new Card(Rank.ACE, Suit.SPADES);
    private static final Card KING_D = new Card(Rank.KING, Suit.DIAMONDS);
    private static final Card NINE_D = new Card(Rank.NINE, Suit.DIAMONDS);
    private static final Card TEN_S = new Card(Rank.TEN, Suit.SPADES);
    private static final Card SEVEN_C = new Card(Rank.SEVEN, Suit.CLUBS);
    private static final Card FIVE_C = new Card(Rank.FIVE, Suit.CLUBS);
    private static final Card TWO_C = new Card(Rank.TWO, Suit.CLUBS);

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

    // ---- add() ----

    /** Normal: the first card sets the total. */
    @Test
    public void addPutsCardInHand()
    {
        Hand hand = new Hand();
        hand.add(SEVEN_C);
        assertEquals(7, hand.total());
    }

    /** Bad input: null is rejected and the hand is unchanged. */
    @Test
    public void addRejectsNull()
    {
        Hand hand = new Hand();
        assertThrows(NullPointerException.class, () -> hand.add(null));
        assertEquals(0, hand.total());
    }

    // ---- total() ----

    /** Normal: an ace counts 11 when that fits. */
    @Test
    public void totalCountsAceAsElevenWhenItFits()
    {
        assertEquals(20, handOf(ACE_S, NINE_D).total());
    }

    /** Edge: the ace drops to 1 when 11 would bust, and an empty hand is 0. */
    @Test
    public void totalDropsAceToOneAndEmptyIsZero()
    {
        assertEquals(15, handOf(ACE_S, NINE_D, FIVE_C).total());
        assertEquals(0, new Hand().total());
    }

    // ---- isBust() ----

    /** Normal: 24 is a bust. */
    @Test
    public void isBustOverTwentyOne()
    {
        assertTrue(handOf(TEN_S, NINE_D, FIVE_C).isBust());
    }

    /** Edge: exactly 21 is not a bust. */
    @Test
    public void isBustFalseAtTwentyOne()
    {
        assertFalse(handOf(TEN_S, NINE_D, TWO_C).isBust());
    }

    // ---- isBlackjack() ----

    /** Normal: ace plus a face card. */
    @Test
    public void isBlackjackForAceAndKing()
    {
        assertTrue(handOf(ACE_S, KING_D).isBlackjack());
    }

    /** Edge: 21 with three cards is not a blackjack. */
    @Test
    public void isBlackjackFalseForThreeCardTwentyOne()
    {
        assertFalse(handOf(SEVEN_C, SEVEN_C, SEVEN_C).isBlackjack());
    }

    // ---- toString() ----

    /** Normal: cards in order, then the total in brackets. */
    @Test
    public void toStringListsCardsAndTotal()
    {
        assertEquals("AS KD (21)", handOf(ACE_S, KING_D).toString());
    }

    /** Edge: an empty hand prints only the zero total. */
    @Test
    public void toStringOfEmptyHand()
    {
        assertEquals("(0)", new Hand().toString());
    }

    @Test
    public void toArtDrawsCardsInOrderIncludingTen()
    {
        assertEquals("+----+ +----+\n|AS  | |10S |\n+----+ +----+",
            handOf(ACE_S, TEN_S).toArt());
    }

    @Test
    public void toArtOfEmptyHandIsEmpty()
    {
        assertEquals("", new Hand().toArt());
    }

    @Test
    public void multipleAcesDemoteIndividually()
    {
        Hand hand = handOf(ACE_S, ACE_S, NINE_D);
        assertEquals(21, hand.total());
        assertFalse(hand.isBlackjack());
        hand.add(KING_D);
        assertEquals(21, hand.total());
        hand.add(TWO_C);
        assertTrue(hand.isBust());
        assertFalse(new Hand().isBust());
        assertFalse(new Hand().isBlackjack());
    }

}
