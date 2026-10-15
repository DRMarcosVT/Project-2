import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * Runs a Blackjack session from the first deposit to the final history.
 *
 * Game owns the other objects and decides when each is used: the Shoe
 * deals, the two Hands score themselves, the Bankroll enforces the money
 * rules, the CardCounter gives betting advice, and the two histories keep
 * what happened. Game itself holds the rules of a round: deal two cards
 * each, let the player hit or stand, play the dealer to 17, then settle.
 *
 * One round, in order:
 *   1. If fewer than Shoe.CUT_CARD cards remain, start a fresh shoe and
 *      reset the card history and the counter.
 *   2. Show the counter's advice, then take a bet (re-asking on a bad one).
 *   3. Deal player, dealer, player, dealer. The dealer's second card is
 *      not shown until the dealer's turn.
 *   4. Unless the player has blackjack, let the player hit until they
 *      stand or bust.
 *   5. If the player neither busted nor has blackjack, the dealer draws
 *      while under 17.
 *   6. Settle the bet, record the round, and stop if the balance is zero.
 *
 * @author Marcos Salas, Madeline Mehfoud, David Moon, Shreyas
 * @version October 2026
 */
public class Game
{
    /** Decks shuffled into each shoe, as casinos do. */
    public static final int DECKS = 3;

    /** The dealer stops drawing at this total or higher. */
    public static final int DEALER_STANDS_ON = 17;

    private final ConsoleUI ui;      // all input and output
    private final Random rng;        // shuffles every shoe of the session
    private final GameHistory history = new GameHistory();

    private Shoe shoe;               // replaced at every shuffle
    private CardHistory cardHistory; // replaced with the shoe
    private CardCounter counter;     // replaced with the shoe
    private Bankroll bankroll;       // set by the first deposit
    private int round = 0;           // rounds completed so far

    /**
     * Creates a session. Nothing happens until run() is called.
     *
     * @param ui  the console to talk to the player through
     * @param rng the random source for shuffling; the tests pass a seeded
     *            Random so every deal is repeatable
     * @throws NullPointerException if either argument is null
     */
    public Game(ConsoleUI ui, Random rng)
    {
        this.ui = Objects.requireNonNull(ui, "ui must not be null");
        this.rng = Objects.requireNonNull(rng, "rng must not be null");
    }

    /**
     * Starts the program: a session on the real keyboard and screen.
     *
     * @param args unused
     */
    public static void main(String[] args)
    {
        ConsoleUI console = new ConsoleUI(new Scanner(System.in), System.out);
        new Game(console, new Random()).run();
    }

    /**
     * Plays the whole session: deposit, then rounds until the player says
     * no or runs out of money, then the history. EOF ends cleanly; an
     * unfinished round is cancelled and its bet returned.
     */
    public void run()
    {
        try
        {
            runSession();
        }
        catch (ConsoleUI.InputEndedException e)
        {
            ui.show("Input ended; goodbye.");
            if (bankroll != null && cardHistory != null)
            {
                showHistory();
            }
        }
    }

    private void runSession()
    {
        ui.show("Welcome to Blackjack. Dealer stands on 17; blackjack pays 3 to 2.");
        bankroll = promptDeposit();
        newShoe();

        boolean playing = true;
        while (playing)
        {
            if (shoe.needsShuffle())
            {
                ui.show("Fewer than " + Shoe.CUT_CARD
                    + " cards left: shuffling a new shoe.");
                newShoe();
            }
            playRound();
            if (bankroll.isBroke())
            {
                ui.show("You are out of money.");
                playing = false;
            }
            else
            {
                playing = ui.promptYesNo("Play another round? (y/n) ");
            }
        }
        showHistory();
    }

    /**
     * Decides how a finished round ends for the player. This is the whole
     * rule table in one place; it is public and static so it can be tested
     * with hand-built hands.
     *
     * @param player the player's final hand
     * @param dealer the dealer's final hand
     * @return the outcome
     * @throws NullPointerException if either hand is null
     */
    public static Outcome settle(Hand player, Hand dealer)
    {
        Objects.requireNonNull(player, "player hand must not be null");
        Objects.requireNonNull(dealer, "dealer hand must not be null");

        if (player.isBust())
        {
            return Outcome.BUST;
        }
        if (player.isBlackjack())
        {
            return dealer.isBlackjack() ? Outcome.PUSH : Outcome.BLACKJACK;
        }
        if (dealer.isBlackjack())
        {
            return Outcome.LOSS;
        }
        if (dealer.isBust() || player.total() > dealer.total())
        {
            return Outcome.WIN;
        }
        if (player.total() == dealer.total())
        {
            return Outcome.PUSH;
        }
        return Outcome.LOSS;
    }

    /**
     * Works out how much money comes back to the player after a round,
     * counting the original bet. A blackjack pays 3 to 2 using integer
     * division, so a $5 blackjack returns $5 + $7 = $12.
     *
     * @param outcome how the round ended
     * @param bet     the bet that was placed, at least 1
     * @return the dollars to credit: 0 for a loss or bust, the bet for a
     *         push, twice the bet for a win, bet + bet * 3 / 2 for blackjack
     * @throws NullPointerException     if outcome is null
     * @throws IllegalArgumentException if bet is zero or negative, or the
     *         payout exceeds int range
     */
    public static int winnings(Outcome outcome, int bet)
    {
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (bet <= 0)
        {
            throw new IllegalArgumentException(
                "Bet must be greater than zero, got " + bet);
        }
        switch (outcome)
        {
            case BLACKJACK:
                return checkedPayout(bet + bet * 3L / 2);
            case WIN:
                return checkedPayout(2L * bet);
            case PUSH:
                return bet;
            default:
                return 0;
        }
    }

    private static int checkedPayout(long amount)
    {
        if (amount > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Payout exceeds whole-dollar integer limit");
        }
        return (int) amount;
    }

    /**
     * Asks for the opening deposit until Bankroll accepts it.
     *
     * @return the new bankroll
     */
    private Bankroll promptDeposit()
    {
        while (true)
        {
            int amount = ui.promptInt("Deposit (whole dollars): ");
            try
            {
                return new Bankroll(amount);
            }
            catch (IllegalArgumentException e)
            {
                ui.show(e.getMessage());
            }
        }
    }

    /**
     * Asks for a bet until Bankroll accepts it.
     *
     * @return the bet, already taken out of the balance
     */
    private int promptBet()
    {
        while (true)
        {
            int amount = ui.promptInt("Bet: ");
            try
            {
                return bankroll.placeBet(amount);
            }
            catch (IllegalArgumentException e)
            {
                ui.show(e.getMessage());
            }
        }
    }

    /**
     * Replaces the shoe and everything that tracks it.
     */
    private void newShoe()
    {
        shoe = new Shoe(DECKS, rng);
        cardHistory = new CardHistory();
        counter = new CardCounter(DECKS);
    }

    /**
     * Deals one card from the shoe into a hand and tells both trackers.
     * Every card of the round goes through here, including the dealer's
     * face-down card; that is fine because advice is only given before
     * the deal, when all previous rounds' cards are already known.
     *
     * @param hand the hand receiving the card
     * @return the card dealt, so the caller can show it
     */
    private Card draw(Hand hand)
    {
        Card card = shoe.deal();
        hand.add(card);
        cardHistory.record(card);
        counter.observe(card);
        return card;
    }

    /**
     * Plays one round from advice to settlement.
     */
    private void playRound()
    {
        round++;
        ui.show("");
        ui.show("--- Round " + round + " --- Balance: $" + bankroll.balance());
        showAdvice();
        int bet = promptBet();

        Hand player = new Hand();
        Hand dealer = new Hand();
        draw(player);
        Card dealerUp = draw(dealer);
        draw(player);
        draw(dealer);
        ui.show("Your hand: " + player);
        ui.show("Dealer shows: " + dealerUp);

        if (player.isBlackjack())
        {
            ui.show("Blackjack!");
        }
        else if (dealer.isBlackjack())
        {
            ui.show("Dealer has blackjack.");
        }
        else
        {
            try
            {
                while (player.total() < 21 && ui.promptAction() == Action.HIT)
                {
                    draw(player);
                    ui.show("Your hand: " + player);
                }
            }
            catch (ConsoleUI.InputEndedException e)
            {
                bankroll.credit(bet);
                ui.show("Unfinished round cancelled; bet returned.");
                throw e;
            }
            if (player.isBust())
            {
                ui.show("Bust.");
            }
            else
            {
                while (dealer.total() < DEALER_STANDS_ON)
                {
                    draw(dealer);
                }
            }
        }
        ui.show("Dealer's hand: " + dealer);

        Outcome outcome = settle(player, dealer);
        int paidBack = winnings(outcome, bet);
        bankroll.credit(paidBack);
        RoundResult result = new RoundResult(round, outcome, bet, paidBack - bet);
        history.record(result);
        ui.show(result + ". Balance: $" + bankroll.balance());
    }

    /**
     * Shows the counter's view of the next round and its suggested bet.
     */
    private void showAdvice()
    {
        double edge = counter.expectedReturn();
        int suggested = counter.suggestedBet(bankroll.balance());
        ui.show(String.format(
            "Cards seen this shoe: %d, unseen: %d. Expected return: %+.1f cents per dollar.",
            cardHistory.all().size(), shoe.cardsRemaining(), edge * 100));
        ui.show("Suggested bet: $" + suggested
            + (edge <= 0 ? " (the minimum; the cards favour the dealer)." : "."));
    }

    /**
     * Prints every round, the win rate, the final balance and the cards
     * dealt from the current shoe.
     */
    private void showHistory()
    {
        ui.show("");
        ui.show("Rounds played (most recent first):");
        for (RoundResult result : history.all())
        {
            ui.show("  " + result);
        }
        ui.show(String.format("Wins: %d  Losses: %d  Win rate: %.0f%%",
            history.wins(), history.losses(), history.winRate() * 100));
        ui.show("Final balance: $" + bankroll.balance());
        ui.show("Cards dealt from the current shoe (most recent first): "
            + cardHistory.all());
        ui.show("Thanks for playing.");
    }
}
