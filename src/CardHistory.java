import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Every card dealt from the current shoe, kept in a LinkedChain so the
 * player can review what has been played. Game replaces it together with
 * the shoe.
 *
 * Duplicates are expected: a three-deck shoe deals the same card up to
 * three times before a shuffle.
 *
 * @author Team
 * @version October 2026
 */
public class CardHistory
{
    private final LinkedChain<Card> seen = new LinkedChain<>();

    /**
     * Remembers one dealt card.
     *
     * @param card the card that was just dealt
     * @throws NullPointerException if card is null
     */
    public void record(Card card)
    {
        seen.add(Objects.requireNonNull(card, "card must not be null"));
    }

    /**
     * Gets every recorded card, most recently dealt first.
     *
     * @return a list of the cards; empty, never null, after a fresh shuffle
     */
    public List<Card> all()
    {
        Card[] array = new Card[seen.size()];
        return Arrays.asList(seen.toArray(array));
    }
}
