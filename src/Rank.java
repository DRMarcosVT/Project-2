/**
 * The thirteen card ranks and the points each one scores in Blackjack.
 *
 * Two through ten score their face value, jack, queen and king score 10,
 * and an ace scores 11 here. Hand.total() drops an ace to 1 whenever
 * counting it as 11 would push the hand over 21.
 *
 * @author Team
 * @version October 2026
 */
public enum Rank
{
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(10, "J"),
    QUEEN(10, "Q"),
    KING(10, "K"),
    ACE(11, "A");

    private final int value;     // points this rank scores (ace as 11)
    private final String label;  // short text used when printing a card

    /**
     * Creates one rank.
     *
     * @param value the points the rank scores, with an ace as 11
     * @param label the short text printed for the rank, e.g. "10" or "K"
     */
    Rank(int value, String label)
    {
        this.value = value;
        this.label = label;
    }

    /**
     * Gets the points this rank scores, counting an ace as 11.
     *
     * @return 2 to 10 for number cards, 10 for faces, 11 for an ace
     */
    public int value()
    {
        return value;
    }

    /**
     * Gets the short text printed for this rank.
     *
     * @return "2" to "10", or "J", "Q", "K", "A"
     */
    public String label()
    {
        return label;
    }
}
