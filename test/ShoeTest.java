import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests for Shoe: the constructor, deal(), cardsRemaining() and
 * needsShuffle(). A seeded Random makes every shuffle repeatable.
 *
 * @author Team
 * @version October 2026
 */
public class ShoeTest
{
    /** Deals the given number of cards and throws them away. */
    private static void dealMany(Shoe shoe, int count)
    {
        for (int i = 0; i < count; i++)
        {
            shoe.deal();
        }
    }

    // ---- constructor ----

    /** Normal: three decks give 156 cards, three of each distinct card. */
    @Test
    public void constructorBuildsThreeFullDecks()
    {
        Shoe shoe = new Shoe(3, new Random(42));
        assertEquals(156, shoe.cardsRemaining());

        Map<Card, Integer> copies = new HashMap<>();
        for (int i = 0; i < 156; i++)
        {
            Card card = shoe.deal();
            copies.put(card, copies.getOrDefault(card, 0) + 1);
        }
        assertEquals(52, copies.size());
        for (int count : copies.values())
        {
            assertEquals(3, count);
        }
    }

    /** Bad input: zero decks, or no random source, is rejected. */
    @Test
    public void constructorRejectsZeroDecksAndNullRandom()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new Shoe(0, new Random(42)));
        assertThrows(NullPointerException.class, () -> new Shoe(1, null));
    }

    // ---- deal() ----

    /** Normal: dealing returns a card and leaves one fewer. */
    @Test
    public void dealReturnsCardAndShrinksShoe()
    {
        Shoe shoe = new Shoe(3, new Random(42));
        assertNotNull(shoe.deal());
        assertEquals(155, shoe.cardsRemaining());
    }

    /** Bad input: dealing from an empty shoe (removing what isn't there). */
    @Test
    public void dealFromEmptyShoeThrows()
    {
        Shoe shoe = new Shoe(1, new Random(42));
        dealMany(shoe, 52);
        assertEquals(0, shoe.cardsRemaining());
        assertThrows(IllegalStateException.class, shoe::deal);
    }

    // ---- cardsRemaining() ----

    /** Normal: ten deals from 156 leave 146. */
    @Test
    public void cardsRemainingCountsDown()
    {
        Shoe shoe = new Shoe(3, new Random(42));
        dealMany(shoe, 10);
        assertEquals(146, shoe.cardsRemaining());
    }

    /** Edge: an emptied shoe reports zero, not a negative number. */
    @Test
    public void cardsRemainingIsZeroWhenEmpty()
    {
        Shoe shoe = new Shoe(1, new Random(42));
        dealMany(shoe, 52);
        assertEquals(0, shoe.cardsRemaining());
    }

    // ---- needsShuffle() ----

    /** Normal: a nearly full shoe does not need a shuffle. */
    @Test
    public void needsShuffleIsFalseWithPlentyOfCards()
    {
        Shoe shoe = new Shoe(3, new Random(42));
        dealMany(shoe, 10);
        assertFalse(shoe.needsShuffle());
    }

    /** Edge: exactly 32 cards is fine, 31 is not. */
    @Test
    public void needsShuffleTurnsTrueBelowCutCard()
    {
        Shoe shoe = new Shoe(3, new Random(42));
        dealMany(shoe, 156 - Shoe.CUT_CARD);
        assertEquals(32, shoe.cardsRemaining());
        assertFalse(shoe.needsShuffle());
        shoe.deal();
        assertTrue(shoe.needsShuffle());
    }
}
