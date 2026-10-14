import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for CardCounter: the constructor, observe(), expectedReturn(),
 * variance() and suggestedBet().
 *
 * The numbers come from the worked example in
 * blackjack-expected-return-model.md: a fresh single deck has an expected
 * return of -0.0193 per dollar and a variance of 0.974.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class CardCounterTest
{
    /** Observes every card of the given ranks from one deck (4 per rank). */
    private static void observeAllOf(CardCounter counter, Rank... ranks)
    {
        for (Rank rank : ranks)
        {
            for (Suit suit : Suit.values())
            {
                counter.observe(new Card(rank, suit));
            }
        }
    }

    /**
     * A single deck with every 2, 3, 4, 5 and 6 already seen: 32 cards
     * remain, all 7 or higher, which favours the player.
     */
    private static CardCounter tenRichDeck()
    {
        CardCounter counter = new CardCounter(1);
        observeAllOf(counter, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX);
        return counter;
    }

    // ---- constructor ----

    /** Normal: a one-deck counter can be built and used. */
    @Test
    public void constructorAcceptsOneDeck()
    {
        CardCounter counter = new CardCounter(1);
        assertTrue(counter.expectedReturn() < 0);
    }

    /** Bad input: zero decks is rejected. */
    @Test
    public void constructorRejectsZeroDecks()
    {
        assertThrows(IllegalArgumentException.class, () -> new CardCounter(0));
    }

    // ---- observe() ----

    /** Normal: seeing a card changes the expected return. */
    @Test
    public void observeChangesExpectedReturn()
    {
        CardCounter counter = new CardCounter(1);
        double fresh = counter.expectedReturn();
        counter.observe(new Card(Rank.FIVE, Suit.CLUBS));
        assertTrue(counter.expectedReturn() > fresh,
            "removing a 5 should help the player");
    }

    /** Bad input: null, and a fifth ace from a one-deck shoe. */
    @Test
    public void observeRejectsNullAndImpossibleCard()
    {
        CardCounter counter = new CardCounter(1);
        assertThrows(NullPointerException.class, () -> counter.observe(null));
        observeAllOf(counter, Rank.ACE);
        assertThrows(IllegalStateException.class,
            () -> counter.observe(new Card(Rank.ACE, Suit.SPADES)));
    }

    // ---- expectedReturn() ----

    /** Normal: a fresh single deck matches the worked example. */
    @Test
    public void expectedReturnOfFreshDeckMatchesModel()
    {
        assertEquals(-0.0193, new CardCounter(1).expectedReturn(), 0.0005);
    }

    /** Bad input: 31 unseen cards is below the model's minimum. */
    @Test
    public void expectedReturnRejectsTooFewUnseenCards()
    {
        CardCounter counter = tenRichDeck();          // 32 unseen: allowed
        counter.expectedReturn();
        counter.observe(new Card(Rank.SEVEN, Suit.CLUBS));  // 31 unseen
        assertThrows(IllegalStateException.class, counter::expectedReturn);
    }

    // ---- variance() ----

    /** Normal: a fresh single deck matches the worked example. */
    @Test
    public void varianceOfFreshDeckMatchesModel()
    {
        assertEquals(0.974, new CardCounter(1).variance(), 0.001);
    }

    /** Bad input: the same minimum applies. */
    @Test
    public void varianceRejectsTooFewUnseenCards()
    {
        CardCounter counter = tenRichDeck();
        counter.observe(new Card(Rank.SEVEN, Suit.CLUBS));
        assertThrows(IllegalStateException.class, counter::variance);
    }

    // ---- suggestedBet() ----

    /** Normal: a losing deck suggests the minimum; a rich deck bets more. */
    @Test
    public void suggestedBetFollowsTheEdge()
    {
        assertEquals(CardCounter.MINIMUM_BET, new CardCounter(1).suggestedBet(100));

        CardCounter rich = tenRichDeck();
        double edge = rich.expectedReturn();
        assertTrue(edge > 0, "a deck of 7s and up should favour the player");
        int expected = (int) Math.floor(edge / rich.variance() * 1000);
        assertEquals(expected, rich.suggestedBet(1000));
        assertTrue(expected > CardCounter.MINIMUM_BET);
    }

    /** Bad input: a bankroll of zero is rejected; a tiny one is capped. */
    @Test
    public void suggestedBetRejectsZeroBankrollAndNeverExceedsIt()
    {
        CardCounter rich = tenRichDeck();
        assertThrows(IllegalArgumentException.class, () -> rich.suggestedBet(0));
        assertEquals(1, rich.suggestedBet(1));
    }
}
