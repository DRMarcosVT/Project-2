import java.util.Objects;

/**
 * One playing card: a rank and a suit.
 *
 * This is a record, so Java writes the constructor, the rank() and suit()
 * accessors, equals() and hashCode() for us. Two cards with the same rank
 * and suit are equal, which is what a three-deck shoe needs: the shoe
 * holds three equal copies of every card, and LinkedChain.count() can
 * count them.
 *
 * @param rank the card's rank, never null
 * @param suit the card's suit, never null
 *
 * @author Team
 * @version October 2026
 */
public record Card(Rank rank, Suit suit)
{
    /**
     * Checks the two parts of a card. A record's "compact constructor"
     * runs before the fields are assigned.
     *
     * @throws NullPointerException if rank or suit is null
     */
    public Card
    {
        Objects.requireNonNull(rank, "rank must not be null");
        Objects.requireNonNull(suit, "suit must not be null");
    }

    /**
     * Prints the card as its rank label followed by the first letter of
     * its suit, e.g. "AS" for the ace of spades or "10H" for the ten of
     * hearts. Plain letters are used because the Windows console does
     * not print suit symbols reliably.
     *
     * @return the short text for this card
     */
    @Override
    public String toString()
    {
        return rank.label() + suit.name().charAt(0);
    }
}
