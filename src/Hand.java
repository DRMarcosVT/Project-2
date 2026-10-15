import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The cards one participant holds in the current round. The player and
 * the dealer each have a Hand.
 *
 * The hand knows how to score itself: aces count 11 unless that would
 * bust, in which case they count 1.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class Hand
{
    private final List<Card> cards = new ArrayList<>();  // in dealt order

    /**
     * Adds a card to the hand.
     *
     * @param card the card dealt to this hand
     * @throws NullPointerException if card is null
     */
    public void add(Card card)
    {
        cards.add(Objects.requireNonNull(card, "card must not be null"));
    }

    /**
     * Scores the hand. Every ace starts as 11; while the total is over 21
     * and an ace is still counted as 11, that ace drops to 1.
     *
     * @return the Blackjack total, 0 for an empty hand
     */
    public int total()
    {
        int total = 0;
        int acesAsEleven = 0;
        for (Card card : cards)
        {
            total += card.rank().value();
            if (card.rank() == Rank.ACE)
            {
                acesAsEleven++;
            }
        }
        while (total > 21 && acesAsEleven > 0)
        {
            total -= 10;
            acesAsEleven--;
        }
        return total;
    }

    /**
     * Says whether the hand has gone over 21.
     *
     * @return true if total() is greater than 21
     */
    public boolean isBust()
    {
        return total() > 21;
    }

    /**
     * Says whether the hand is a natural blackjack: exactly two cards that
     * total 21. A 21 made with three or more cards is not a blackjack.
     *
     * @return true for an ace plus a ten-value card as the only two cards
     */
    public boolean isBlackjack()
    {
        return cards.size() == 2 && total() == 21;
    }

    /**
     * Draws the cards as ASCII boxes in dealt order.
     *
     * @return three lines of card art, or an empty string for no cards; O(k)
     */
    public String toArt()
    {
        if (cards.isEmpty())
        {
            return "";
        }
        String border = "+----+ ".repeat(cards.size()).stripTrailing();
        StringBuilder faces = new StringBuilder();
        for (Card card : cards)
        {
            faces.append(String.format("|%-4s| ", card));
        }
        return border + "\n" + faces.toString().stripTrailing() + "\n" + border;
    }

    /**
     * Prints the cards and the total, e.g. "AS KD (21)". An empty hand
     * prints "(0)".
     *
     * @return the hand as text
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Card card : cards)
        {
            sb.append(card).append(' ');
        }
        sb.append('(').append(total()).append(')');
        return sb.toString();
    }
}
