import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The cards the dealer deals from: several decks shuffled together.
 *
 * The cards live in a LinkedChain. Dealing is LinkedChain.remove(), which
 * takes the entry at the head, so the shoe is used like a stack of cards
 * with the top card dealt first. The chain never sees a card's meaning;
 * it only stores and hands back Card objects.
 *
 * Shuffling happens before the cards go into the chain: the constructor
 * builds every card in an ArrayList, shuffles the list with
 * Collections.shuffle, then adds the cards to the chain one by one. That
 * keeps the randomness in the class that owns the deck and leaves the
 * LinkedChain unchanged.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class Shoe
{
    /**
     * When fewer cards than this remain, Game starts the next round with a
     * fresh shoe. One round can never use more than 31 cards, so a round
     * that starts with 32 or more can never run the shoe dry.
     */
    public static final int CUT_CARD = 32;

    private final LinkedChain<Card> cards;  // undealt cards, head dealt first

    /**
     * Builds and shuffles a shoe.
     *
     * @param decks how many 52-card decks to shuffle together, at least 1
     * @param rng   the random source used to shuffle; pass a seeded Random
     *              to get the same order every time, which the tests do
     * @throws IllegalArgumentException if decks is less than 1
     * @throws NullPointerException     if rng is null
     */
    public Shoe(int decks, Random rng)
    {
        if (decks < 1)
        {
            throw new IllegalArgumentException(
                "A shoe needs at least one deck, got " + decks);
        }
        Objects.requireNonNull(rng, "rng must not be null");

        List<Card> all = new ArrayList<>();
        for (int d = 0; d < decks; d++)
        {
            for (Suit suit : Suit.values())
            {
                for (Rank rank : Rank.values())
                {
                    all.add(new Card(rank, suit));
                }
            }
        }
        Collections.shuffle(all, rng);

        cards = new LinkedChain<>();
        for (Card card : all)
        {
            cards.add(card);
        }
    }

    /**
     * Deals the top card.
     *
     * @return the card removed from the head of the chain
     * @throws IllegalStateException if no cards remain; Game checks
     *         needsShuffle() before every round so this is never reached
     *         in play
     */
    public Card deal()
    {
        Card card = cards.remove();
        if (card == null)
        {
            throw new IllegalStateException("The shoe is empty");
        }
        return card;
    }

    /**
     * Gets how many cards have not been dealt yet.
     *
     * @return the number of cards still in the shoe
     */
    public int cardsRemaining()
    {
        return cards.size();
    }

    /**
     * Says whether the next round should start with a fresh shoe.
     *
     * @return true when fewer than CUT_CARD cards remain
     */
    public boolean needsShuffle()
    {
        return cards.size() < CUT_CARD;
    }
}
