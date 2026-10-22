import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The card counter behind the betting advice shown before each round.
 *
 * It remembers every card dealt from the current shoe in a LinkedChain.
 * From the cards that remain unseen it works out two numbers: the
 * player's expected return per dollar on the next round, and the bet the
 * Kelly criterion recommends. The maths follows
 * blackjack-expected-return-model.md row by row, and every method below
 * names the row it implements.
 *
 * How the counter represents cards: a rank is a number from 1 to 10,
 * where 1 is an ace and 10 stands for ten, jack, queen and king together
 * (row 2 of the model). The unseen cards are an int array n of length 11
 * where n[r] is how many unseen cards have rank r; n[0] is unused and
 * stays 0.
 *
 * How the maths is run: the model is recursive (the value of hitting
 * depends on the value of every hand you could reach), and many branches
 * reach the same position by different routes. Each position's answer is
 * stored in a HashMap the first time it is computed and looked up after
 * that. The map key is a String built from the position, which is the
 * simplest key that works.
 *
 * @author Team
 * @version October 2026
 */
public class CardCounter
{
    /** The smallest bet the advice suggests: row 13's "table minimum". */
    public static final int MINIMUM_BET = 1;

    /**
     * The fewest unseen cards the model accepts. One round never uses
     * more than 31 cards, so with 32 or more unseen no branch of the
     * recursion can run out of cards. This equals Shoe.CUT_CARD.
     */
    public static final int MINIMUM_UNSEEN = Shoe.CUT_CARD;

    // Indexes into a "result chances" array: the chance the round ends in
    // each of the four results of row 11, and what each result pays.
    private static final int BLACKJACK = 0;
    private static final int WIN = 1;
    private static final int PUSH = 2;
    private static final int LOSS = 3;
    private static final double[] PAYOUT = {1.5, 1.0, 0.0, -1.0};

    // Indexes into a "dealer finish" array: index 0 to 4 are the totals
    // 17 to 21 (index = total - 17), then bust, then dealer blackjack.
    private static final int DEALER_BUST = 5;
    private static final int DEALER_BLACKJACK = 6;

    private final int decks;                       // decks in the shoe
    private final LinkedChain<Card> seen = new LinkedChain<>();

    // Stored answers, keyed by position. Cleared each time a new set of
    // unseen cards is analysed so memory stays bounded.
    private final Map<String, double[]> dealerMemo = new HashMap<>();
    private final Map<String, double[]> playerMemo = new HashMap<>();

    // The last full answer, so expectedReturn(), variance() and
    // suggestedBet() can be called in a row without recomputing.
    private String lastUnseenKey = null;
    private double[] lastChances = null;

    /**
     * Creates a counter for a fresh shoe.
     *
     * @param decks the number of decks in the shoe, at least 1
     * @throws IllegalArgumentException if decks is less than 1
     */
    public CardCounter(int decks)
    {
        if (decks < 1)
        {
            throw new IllegalArgumentException(
                "A shoe needs at least one deck, got " + decks);
        }
        this.decks = decks;
    }

    /**
     * Records one dealt card so it leaves the unseen cards.
     *
     * @param card the card that was just dealt
     * @throws NullPointerException  if card is null
     * @throws IllegalStateException if every card of that rank in the shoe
     *         has already been seen, which means the caller forgot to
     *         replace the counter when the shoe was shuffled
     */
    public void observe(Card card)
    {
        Objects.requireNonNull(card, "card must not be null");
        int observed = 0;
        for (Card dealt : seen.toArray(new Card[seen.size()]))
        {
            if (dealt.rank() == card.rank())
            {
                observed++;
            }
        }
        // Validate the actual rank before combining ten-point ranks for the model.
        if (observed >= 4L * decks)
        {
            throw new IllegalStateException("Every " + card.rank() + " in a "
                + decks + "-deck shoe has already been seen");
        }
        seen.add(card);
    }

    /**
     * Row 10: the player's average result per dollar bet on the next
     * round, playing hit or stand as well as possible. Negative means the
     * remaining cards favour the dealer. A fresh single deck gives about
     * -0.0193 (the model's worked example).
     *
     * @return the expected return per dollar
     * @throws IllegalStateException if fewer than MINIMUM_UNSEEN cards
     *         remain unseen
     */
    public double expectedReturn()
    {
        return value(nextRoundChances());
    }

    /**
     * Row 12: how widely single rounds vary around the expected return.
     * Nearly every round is a full win or a full loss, so this is a little
     * under 1 (0.974 for a fresh single deck).
     *
     * @return the variance of one round's result per dollar
     * @throws IllegalStateException if fewer than MINIMUM_UNSEEN cards
     *         remain unseen
     */
    public double variance()
    {
        double[] chances = nextRoundChances();
        double mean = value(chances);
        double variance = 0.0;
        for (int i = 0; i < chances.length; i++)
        {
            double distance = PAYOUT[i] - mean;
            variance += chances[i] * distance * distance;
        }
        return variance;
    }

    /**
     * Row 11: the chance the next round ends in each of its four results.
     * This is what variance() and the web table's charts are built from.
     *
     * @return a new length-4 array: index 0 is the chance of a player
     *         blackjack paid 3 to 2, 1 a win, 2 a push and 3 a loss; the
     *         four add to 1
     * @throws IllegalStateException if fewer than MINIMUM_UNSEEN cards
     *         remain unseen
     */
    public double[] resultChances()
    {
        return nextRoundChances().clone();
    }

    /**
     * Row 13: the Kelly bet for the next round. When the expected return
     * is positive the fraction of the bankroll to bet is the expected
     * return divided by the variance; otherwise the advice is the table
     * minimum. The result is always between MINIMUM_BET and the bankroll.
     *
     * @param bankroll the player's current balance in whole dollars
     * @return the suggested bet in whole dollars
     * @throws IllegalArgumentException if bankroll is zero or negative
     * @throws IllegalStateException    if fewer than MINIMUM_UNSEEN cards
     *         remain unseen
     */
    public int suggestedBet(int bankroll)
    {
        if (bankroll <= 0)
        {
            throw new IllegalArgumentException(
                "Bankroll must be greater than zero, got " + bankroll);
        }
        double edge = expectedReturn();
        if (edge <= 0)
        {
            return Math.min(MINIMUM_BET, bankroll);
        }
        double fraction = edge / variance();
        int bet = (int) Math.floor(fraction * bankroll);
        return Math.max(MINIMUM_BET, Math.min(bet, bankroll));
    }

    // ------------------------------------------------------------------
    // Rows 1 to 3: hand totals, the unseen cards, and the dealer's up card
    // ------------------------------------------------------------------

    /**
     * Row 1: the total the game scores for a hand.
     *
     * @param hard   the hand's cards added with every ace as 1
     * @param hasAce whether the hand holds at least one ace
     * @return hard + 10 when an ace can count as 11 without busting,
     *         otherwise hard
     */
    private static int handTotal(int hard, boolean hasAce)
    {
        if (hasAce && hard + 10 <= 21)
        {
            return hard + 10;
        }
        return hard;
    }

    /**
     * Row 2: counts the unseen cards of each rank, which is the fresh shoe
     * minus every card in the seen chain.
     *
     * @return an array n where n[r] is the unseen cards of rank r (1 to 10)
     */
    private int[] unseen()
    {
        int[] n = new int[11];
        for (int rank = 1; rank <= 9; rank++)
        {
            n[rank] = 4 * decks;           // four of each rank per deck
        }
        n[10] = 16 * decks;                // ten, jack, queen, king

        Card[] dealt = seen.toArray(new Card[seen.size()]);
        for (Card card : dealt)
        {
            n[rankIndex(card)]--;
        }
        return n;
    }

    /**
     * Row 2: the total number of unseen cards.
     *
     * @param n the unseen counts
     * @return the sum of the counts
     */
    private static int unseenCount(int[] n)
    {
        int count = 0;
        for (int rank = 1; rank <= 10; rank++)
        {
            count += n[rank];
        }
        return count;
    }

    /**
     * Row 4's n^(-r): the unseen counts after one card of a rank is drawn.
     * Returns a copy so the caller's array is untouched.
     *
     * @param n    the unseen counts
     * @param rank the rank drawn
     * @return a new array with n[rank] lowered by one
     */
    private static int[] without(int[] n, int rank)
    {
        int[] copy = n.clone();
        copy[rank]--;
        return copy;
    }

    /**
     * Converts a Card to the model's rank number: ace is 1, two to nine
     * are themselves, and ten, jack, queen and king are all 10.
     *
     * @param card the card
     * @return 1 to 10
     */
    private static int rankIndex(Card card)
    {
        int value = card.rank().value();
        return value == 11 ? 1 : value;
    }

    /**
     * Row 3: the rank that would complete a dealer blackjack with the
     * given up card. Returns 0 when no rank can, and because n[0] is
     * always 0, n[blackjackPartner(up)] is then 0 as row 5 requires.
     *
     * @param up the dealer's face-up rank
     * @return 10 for an ace up, 1 for a ten up, otherwise 0
     */
    private static int blackjackPartner(int up)
    {
        if (up == 1)
        {
            return 10;
        }
        if (up == 10)
        {
            return 1;
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Rows 4 and 5: the dealer's finish
    // ------------------------------------------------------------------

    /**
     * Row 4: the chance of each dealer finish from a dealer hand of two or
     * more cards. The dealer makes no choices: over 21 is a bust, 17 to 21
     * is the finish, and below 17 the dealer draws each rank with its
     * chance and this method repeats from the new hand.
     *
     * @param hard   the dealer's hard total
     * @param hasAce whether the dealer holds an ace
     * @param n      the unseen counts
     * @return a length-7 array of chances; index DEALER_BLACKJACK stays 0
     *         because a dealer blackjack is handled in row 5
     */
    private double[] dealerFinish(int hard, boolean hasAce, int[] n)
    {
        double[] chances = new double[7];
        if (hard > 21)
        {
            chances[DEALER_BUST] = 1.0;
            return chances;
        }
        int total = handTotal(hard, hasAce);
        if (total >= 17)
        {
            chances[total - 17] = 1.0;
            return chances;
        }

        String key = hard + "," + hasAce + "," + Arrays.toString(n);
        double[] stored = dealerMemo.get(key);
        if (stored != null)
        {
            return stored;
        }

        int count = unseenCount(n);
        for (int rank = 1; rank <= 10; rank++)
        {
            if (n[rank] == 0)
            {
                continue;
            }
            double drawChance = (double) n[rank] / count;
            double[] next = dealerFinish(
                hard + rank, hasAce || rank == 1, without(n, rank));
            for (int i = 0; i < chances.length; i++)
            {
                chances[i] += drawChance * next[i];
            }
        }
        dealerMemo.put(key, chances);
        return chances;
    }

    /**
     * Row 5: the chance of each dealer final hand given only the face-up
     * card. The face-down card is the dealer's first draw: if it is the
     * rank that completes blackjack, the dealer has blackjack; any other
     * rank gives a two-card hand that row 4 plays out.
     *
     * @param up the dealer's face-up rank
     * @param n  the unseen counts
     * @return a length-7 array of chances including DEALER_BLACKJACK
     */
    private double[] dealerFromUpCard(int up, int[] n)
    {
        String key = "up" + up + "," + Arrays.toString(n);
        double[] stored = dealerMemo.get(key);
        if (stored != null)
        {
            return stored;
        }

        double[] chances = new double[7];
        int count = unseenCount(n);
        int partner = blackjackPartner(up);
        chances[DEALER_BLACKJACK] = (double) n[partner] / count;
        for (int rank = 1; rank <= 10; rank++)
        {
            if (rank == partner || n[rank] == 0)
            {
                continue;
            }
            double drawChance = (double) n[rank] / count;
            double[] next = dealerFinish(
                up + rank, up == 1 || rank == 1, without(n, rank));
            for (int i = 0; i < chances.length; i++)
            {
                chances[i] += drawChance * next[i];
            }
        }
        dealerMemo.put(key, chances);
        return chances;
    }

    // ------------------------------------------------------------------
    // Rows 6, 7 and 11: the player's turn
    // ------------------------------------------------------------------

    /**
     * Rows 6, 7 and 11 together: the chance of each result when the
     * player holds this hand and plays hit or stand as well as possible
     * from here. Row 6 values standing, row 7 values hitting by drawing
     * each rank in turn, and the better of the two is kept (standing on a
     * tie, as row 11 says). Keeping the four chances rather than only the
     * average lets variance() reuse the same recursion.
     *
     * @param hard   the player's hard total
     * @param hasAce whether the player holds an ace
     * @param up     the dealer's face-up rank
     * @param n      the unseen counts at this point
     * @return a length-4 array of chances indexed by BLACKJACK, WIN, PUSH,
     *         LOSS; BLACKJACK stays 0 because a natural is handled in row 8
     */
    private double[] playerChances(int hard, boolean hasAce, int up, int[] n)
    {
        if (hard > 21)
        {
            double[] bust = new double[4];
            bust[LOSS] = 1.0;
            return bust;
        }

        String key = hard + "," + hasAce + "," + up + "," + Arrays.toString(n);
        double[] stored = playerMemo.get(key);
        if (stored != null)
        {
            return stored;
        }

        // Row 6: stand. Win if the dealer busts or stops below us, lose if
        // the dealer stops above us or has blackjack, push otherwise.
        double[] dealer = dealerFromUpCard(up, n);
        int total = handTotal(hard, hasAce);
        double win = dealer[DEALER_BUST];
        double lose = dealer[DEALER_BLACKJACK];
        for (int finish = 17; finish <= 21; finish++)
        {
            if (finish < total)
            {
                win += dealer[finish - 17];
            }
            else if (finish > total)
            {
                lose += dealer[finish - 17];
            }
        }
        double[] stand = new double[4];
        stand[WIN] = win;
        stand[PUSH] = 1.0 - win - lose;
        stand[LOSS] = lose;

        // Row 7: hit. Draw each rank with its chance and play on.
        double[] hit = new double[4];
        int count = unseenCount(n);
        for (int rank = 1; rank <= 10; rank++)
        {
            if (n[rank] == 0)
            {
                continue;
            }
            double drawChance = (double) n[rank] / count;
            double[] next = playerChances(
                hard + rank, hasAce || rank == 1, up, without(n, rank));
            for (int i = 0; i < hit.length; i++)
            {
                hit[i] += drawChance * next[i];
            }
        }

        double[] best = value(stand) >= value(hit) ? stand : hit;
        playerMemo.put(key, best);
        return best;
    }

    /**
     * Rows 10 and 11: the average result per dollar for a set of result
     * chances: 1.5 x blackjack chance + win chance - loss chance.
     *
     * @param chances a length-4 array indexed by BLACKJACK, WIN, PUSH, LOSS
     * @return the expected return
     */
    private static double value(double[] chances)
    {
        double sum = 0.0;
        for (int i = 0; i < chances.length; i++)
        {
            sum += PAYOUT[i] * chances[i];
        }
        return sum;
    }

    // ------------------------------------------------------------------
    // Rows 8 to 11: every possible opening of the next round
    // ------------------------------------------------------------------

    /**
     * Rows 8 to 11 for the current unseen cards, with the answer cached so
     * repeated calls are free until another card is observed.
     *
     * @return the chance of each result of the next round
     * @throws IllegalStateException if fewer than MINIMUM_UNSEEN cards
     *         remain unseen
     */
    private double[] nextRoundChances()
    {
        int[] n = unseen();
        String key = Arrays.toString(n);
        if (key.equals(lastUnseenKey))
        {
            return lastChances;
        }
        if (unseenCount(n) < MINIMUM_UNSEEN)
        {
            throw new IllegalStateException("Only " + unseenCount(n)
                + " unseen cards; the shoe should have been shuffled");
        }
        dealerMemo.clear();
        playerMemo.clear();
        lastChances = allOpenings(n);
        lastUnseenKey = key;
        return lastChances;
    }

    /**
     * Rows 8, 9, 10 and 11: goes through all 1,000 openings (first card,
     * second card, dealer up card), multiplies each opening's chance by
     * the chance of each result from that opening, and adds them up.
     *
     * @param n the unseen counts before the deal
     * @return a length-4 array: the chance the next round ends in each
     *         result
     */
    private double[] allOpenings(int[] n)
    {
        double[] chances = new double[4];
        int count = unseenCount(n);
        for (int first = 1; first <= 10; first++)
        {
            for (int second = 1; second <= 10; second++)
            {
                for (int up = 1; up <= 10; up++)
                {
                    // Row 9: chance of this opening. Each later card has
                    // one fewer card to choose from, and one fewer of its
                    // rank for every earlier card of the same rank.
                    int secondLeft = n[second] - (second == first ? 1 : 0);
                    int upLeft = n[up] - (up == first ? 1 : 0)
                        - (up == second ? 1 : 0);
                    double openingChance = (double) n[first] / count
                        * secondLeft / (count - 1)
                        * upLeft / (count - 2);
                    if (openingChance <= 0)
                    {
                        continue;
                    }

                    int[] rest = without(without(without(n, first), second), up);
                    double[] result;
                    boolean natural = (first == 1 && second == 10)
                        || (first == 10 && second == 1);
                    if (natural)
                    {
                        // Row 8, blackjack: paid 1.5 unless the dealer
                        // also has blackjack, which is a push.
                        double dealerBlackjack =
                            (double) rest[blackjackPartner(up)] / unseenCount(rest);
                        result = new double[4];
                        result[BLACKJACK] = 1.0 - dealerBlackjack;
                        result[PUSH] = dealerBlackjack;
                    }
                    else
                    {
                        // Row 8, otherwise: play on from the two cards.
                        result = playerChances(
                            first + second, first == 1 || second == 1, up, rest);
                    }

                    for (int i = 0; i < chances.length; i++)
                    {
                        chances[i] += openingChance * result[i];
                    }
                }
            }
        }
        return chances;
    }
}
