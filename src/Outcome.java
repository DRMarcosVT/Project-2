/**
 * The five ways a round can end for the player.
 *
 * Game.winnings() turns an outcome and a bet into the money paid back:
 * BLACKJACK pays 3 to 2, WIN pays 1 to 1, PUSH returns the bet, and LOSS
 * and BUST forfeit it.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public enum Outcome
{
    /** The player's first two cards total 21 and the dealer's do not. */
    BLACKJACK,
    /** The dealer busts, or the player's total is higher. */
    WIN,
    /** Equal totals, or both sides have blackjack. */
    PUSH,
    /** The dealer's total is higher, or only the dealer has blackjack. */
    LOSS,
    /** The player went over 21. */
    BUST
}
