import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The same Blackjack session as Game, played from a web page instead of
 * the console.
 *
 * A browser cannot run Java, so main() starts a small HTTP server on this
 * machine, serves web/index.html, and answers the page's requests. The
 * page draws the table, the cards and the card counter's maths; this
 * class holds the game. It composes the same objects Game does (Shoe,
 * Hand, Bankroll, CardCounter, CardHistory, GameHistory) and uses
 * Game.settle() and Game.winnings() for the rules, so a round plays out
 * exactly as on the console: deal player, dealer, player, dealer; a
 * blackjack on either side settles at once; the player hits until they
 * stand, bust or reach 21; the dealer draws to Game.DEALER_STANDS_ON.
 *
 * Where the console asks a question and waits, this class instead sits
 * in a Phase and accepts the one or two moves that phase allows. A move
 * sent in the wrong phase, or with a bad amount, is rejected with the
 * same exceptions and messages the console version shows, and the page
 * shows the message and lets the player try again.
 *
 * Requests the server answers, all returning the whole state as JSON:
 *   GET  /api/state
 *   POST /api/deposit?amount=N   POST /api/bet?amount=N
 *   POST /api/hit                POST /api/stand
 *   POST /api/next               POST /api/quit
 *   POST /api/restart            (a fresh session)
 *
 * @author Team
 * @version October 2026
 */
public class WebGame
{
    /** The port main() listens on when none is given. */
    public static final int DEFAULT_PORT = 8000;

    /** The page the server serves, relative to the working directory. */
    public static final String PAGE = "web/index.html";

    /** Where the session is, which decides which moves are allowed. */
    public enum Phase
    {
        /** Waiting for the opening deposit. */
        DEPOSIT,
        /** Advice is shown; waiting for a bet (or quit). */
        BETTING,
        /** Cards are dealt; waiting for hit or stand. */
        PLAYER_TURN,
        /** The round is settled; waiting for next round or quit. */
        ROUND_OVER,
        /** The session ended: quit, out of money, or balance at the int limit. */
        OVER
    }

    private final Random rng;
    private final GameHistory history = new GameHistory();

    private Shoe shoe;
    private CardHistory cardHistory;
    private CardCounter counter;
    private Bankroll bankroll;

    private Phase phase = Phase.DEPOSIT;
    private int round = 0;              // rounds started so far
    private boolean shuffled = false;   // a fresh shoe since the last round
    private String message = "";        // why the last move was rejected

    private Hand player;
    private Hand dealer;
    private final List<Card> playerCards = new ArrayList<>(); // in dealt order
    private final List<Card> dealerCards = new ArrayList<>(); // in dealt order
    private int bet;
    private Outcome outcome;
    private int netChange;

    // The counter's view before the current round, fixed at bet time.
    private double edge;
    private double variance;
    private double[] chances;
    private int suggested;

    /**
     * Creates a session waiting for its deposit.
     *
     * @param rng the random source for shuffling; the tests pass a seeded
     *            Random so every deal is repeatable
     * @throws NullPointerException if rng is null
     */
    public WebGame(Random rng)
    {
        this.rng = Objects.requireNonNull(rng, "rng must not be null");
    }

    /**
     * Starts the program: serves the table on DEFAULT_PORT, or on the port
     * given as the only argument, and opens it in the default browser.
     * A port that is not a whole number in 0..65535 prints usage and returns.
     *
     * @param args optionally the port number
     * @throws IOException if the port cannot be opened
     */
    public static void main(String[] args) throws IOException
    {
        int port = DEFAULT_PORT;
        if (args.length > 0)
        {
            try
            {
                port = Integer.parseInt(args[0].trim());
                if (port < 0 || port > 65535)
                {
                    throw new IllegalArgumentException("Port must be in 0..65535");
                }
            }
            catch (IllegalArgumentException e)
            {
                System.out.println("Usage: java -cp out WebGame [port]");
                return;
            }
        }
        HttpServer server = start(new WebGame(new Random()), port);
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        System.out.println("Blackjack table at " + url + "  (Ctrl+C to stop)");
        openBrowser(url);
    }

    /**
     * Starts an HTTP server that serves the page and plays this session.
     *
     * @param game the session to play
     * @param port the port to listen on; 0 lets the system pick one, which
     *             the tests use
     * @return the running server; call stop(0) to end it
     * @throws IOException              if the port cannot be opened
     * @throws IllegalArgumentException if port is negative or above 65535
     * @throws NullPointerException     if game is null
     */
    public static HttpServer start(WebGame game, int port) throws IOException
    {
        Objects.requireNonNull(game, "game must not be null");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        WebGame[] current = {game};   // /api/restart swaps in a fresh session
        server.createContext("/api/", exchange ->
        {
            if (exchange.getRequestMethod().equals("POST")
                && exchange.getRequestURI().getPath().equals("/api/restart"))
            {
                current[0] = new WebGame(game.rng);
            }
            current[0].handleApi(exchange);
        });
        server.createContext("/", WebGame::handlePage);
        server.start();
        return server;
    }

    private static void openBrowser(String url)
    {
        try
        {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(URI.create(url));
            }
        }
        catch (IOException | RuntimeException e)
        {
            System.out.println("Open the address above in a browser.");
        }
    }

    // ------------------------------------------------------------------
    // The moves. Each one is the web version of one console prompt.
    // ------------------------------------------------------------------

    /**
     * Gets the current phase.
     *
     * @return the phase
     */
    public Phase phase()
    {
        return phase;
    }

    /**
     * Opens the bankroll, as the console's "Deposit (whole dollars):".
     *
     * @param amount the deposit in whole dollars
     * @throws IllegalStateException    if the session is past the deposit
     * @throws IllegalArgumentException if amount is zero or negative; the
     *         session stays at the deposit prompt
     */
    public synchronized void deposit(int amount)
    {
        require(Phase.DEPOSIT, "The deposit has already been made");
        bankroll = new Bankroll(amount);
        newShoe();
        message = "";
        startBetting();
    }

    /**
     * Places the bet and deals, as the console's "Bet:". A blackjack on
     * either side settles the round at once; otherwise the player's turn
     * begins.
     *
     * @param amount the bet in whole dollars
     * @throws IllegalStateException    if no bet is being asked for
     * @throws IllegalArgumentException if amount is zero, negative, above
     *         the balance or could overflow; the balance is unchanged
     */
    public synchronized void bet(int amount)
    {
        require(Phase.BETTING, "No bet is being asked for");
        bet = bankroll.placeBet(amount);
        message = "";
        round++;
        player = new Hand();
        dealer = new Hand();
        playerCards.clear();
        dealerCards.clear();
        outcome = null;
        draw(player, playerCards);
        draw(dealer, dealerCards);
        draw(player, playerCards);
        draw(dealer, dealerCards);
        if (player.isBlackjack() || dealer.isBlackjack())
        {
            finishRound();
        }
        else
        {
            phase = Phase.PLAYER_TURN;
        }
    }

    /**
     * Takes one more card. A bust ends the round; reaching 21 stands
     * automatically, as Game does.
     *
     * @throws IllegalStateException if it is not the player's turn
     */
    public synchronized void hit()
    {
        require(Phase.PLAYER_TURN, "It is not your turn");
        message = "";
        draw(player, playerCards);
        if (player.isBust())
        {
            finishRound();
        }
        else if (player.total() >= 21)
        {
            stand();
        }
    }

    /**
     * Ends the player's turn: the dealer draws to Game.DEALER_STANDS_ON
     * and the round is settled.
     *
     * @throws IllegalStateException if it is not the player's turn
     */
    public synchronized void stand()
    {
        require(Phase.PLAYER_TURN, "It is not your turn");
        message = "";
        while (dealer.total() < Game.DEALER_STANDS_ON)
        {
            draw(dealer, dealerCards);
        }
        finishRound();
    }

    /**
     * Answers yes to "Play another round?": shuffles if fewer than
     * Shoe.CUT_CARD cards remain, then asks for the next bet.
     *
     * @throws IllegalStateException if no round has just finished
     */
    public synchronized void nextRound()
    {
        require(Phase.ROUND_OVER, "No round has finished");
        message = "";
        shuffled = false;
        if (shoe.needsShuffle())
        {
            newShoe();
            shuffled = true;
        }
        startBetting();
    }

    /**
     * Answers no to "Play another round?", or leaves the table before a
     * bet is placed. Nothing is at stake in either phase.
     *
     * @throws IllegalStateException if a round is in progress or the
     *         session has not started or has already ended
     */
    public synchronized void quit()
    {
        if (phase != Phase.ROUND_OVER && phase != Phase.BETTING)
        {
            throw new IllegalStateException("Finish the round before leaving");
        }
        message = "";
        phase = Phase.OVER;
    }

    private void require(Phase expected, String otherwise)
    {
        if (phase != expected)
        {
            throw new IllegalStateException(otherwise);
        }
    }

    private void newShoe()
    {
        shoe = new Shoe(Game.DECKS, rng);
        cardHistory = new CardHistory();
        counter = new CardCounter(Game.DECKS);
    }

    /** Deals one card into a hand and tells both trackers, as Game does. */
    private void draw(Hand hand, List<Card> record)
    {
        Card card = shoe.deal();
        hand.add(card);
        record.add(card);
        cardHistory.record(card);
        counter.observe(card);
    }

    /** Runs the counter for the coming round and waits for the bet. */
    private void startBetting()
    {
        if (bankroll.balance() == Integer.MAX_VALUE)
        {
            phase = Phase.OVER;
            message = "Balance limit reached; session ended. Your balance is preserved.";
            return;
        }
        edge = counter.expectedReturn();
        variance = counter.variance();
        chances = counter.resultChances();
        suggested = counter.suggestedBet(bankroll.balance());
        phase = Phase.BETTING;
    }

    private void finishRound()
    {
        outcome = Game.settle(player, dealer);
        int paidBack = Game.winnings(outcome, bet);
        bankroll.credit(paidBack);
        netChange = paidBack - bet;
        history.record(new RoundResult(round, outcome, bet, netChange));
        phase = bankroll.isBroke() ? Phase.OVER : Phase.ROUND_OVER;
        if (bankroll.balance() == Integer.MAX_VALUE)
        {
            phase = Phase.OVER;
            message = "Balance limit reached; session ended. Your balance is preserved.";
        }
    }

    // ------------------------------------------------------------------
    // The state as JSON, which is everything the page draws
    // ------------------------------------------------------------------

    /**
     * Writes the whole session as one JSON object: phase, money, the two
     * hands, the counter's advice, the round history and the cards dealt
     * from the shoe. During the player's turn the dealer's hole card is
     * left out, so the page cannot peek.
     *
     * @return the JSON text
     */
    public synchronized String stateJson()
    {
        List<Card> visible = visibleCards();
        StringBuilder sb = new StringBuilder("{");
        field(sb, "phase", phase.name());
        sb.append(",\"round\":").append(round);
        sb.append(",\"balance\":").append(bankroll == null ? 0 : bankroll.balance());
        sb.append(",\"message\":").append(quote(message));
        sb.append(",\"decks\":").append(Game.DECKS);
        sb.append(",\"cutCard\":").append(Shoe.CUT_CARD);
        sb.append(",\"minimumBet\":").append(CardCounter.MINIMUM_BET);
        sb.append(",\"dealerStandsOn\":").append(Game.DEALER_STANDS_ON);

        sb.append(",\"shoe\":");
        if (shoe == null)
        {
            sb.append("null");
        }
        else
        {
            sb.append("{\"remaining\":").append(shoe.cardsRemaining())
              .append(",\"seen\":").append(visible.size())
              .append(",\"total\":").append(Game.DECKS * 52)
              .append(",\"shuffled\":").append(shuffled)
              .append(",\"unseenByRank\":").append(unseenByRank(visible))
              .append('}');
        }

        sb.append(",\"advice\":");
        if (chances == null)
        {
            sb.append("null");
        }
        else
        {
            sb.append("{\"edge\":").append(edge)
              .append(",\"variance\":").append(variance)
              .append(",\"fraction\":").append(edge / variance)
              .append(",\"suggested\":").append(suggested)
              .append(",\"chances\":").append(numbers(chances))
              .append('}');
        }

        sb.append(",\"bet\":").append(phase == Phase.BETTING || player == null ? 0 : bet);
        sb.append(",\"player\":").append(hand(player, playerCards, false));
        sb.append(",\"dealer\":").append(hand(dealer, dealerCards, phase == Phase.PLAYER_TURN));
        sb.append(",\"outcome\":").append(outcome == null ? "null" : quote(outcome.name()));
        sb.append(",\"net\":").append(outcome == null ? 0 : netChange);

        sb.append(",\"history\":[");
        String separator = "";
        for (RoundResult r : history.all())
        {
            sb.append(separator).append("{\"round\":").append(r.round())
              .append(",\"outcome\":").append(quote(r.outcome().name()))
              .append(",\"bet\":").append(r.bet())
              .append(",\"net\":").append(r.netChange()).append('}');
            separator = ",";
        }
        sb.append("],\"wins\":").append(history.wins())
          .append(",\"losses\":").append(history.losses())
          .append(",\"winRate\":").append(history.winRate());

        sb.append(",\"cardsDealt\":[");
        separator = "";
        for (Card c : visible)
        {
            sb.append(separator).append(card(c));
            separator = ",";
        }
        sb.append("]}");
        return sb.toString();
    }

    /** A public snapshot; the internal history still records every dealt card. */
    private List<Card> visibleCards()
    {
        List<Card> visible = new ArrayList<>();
        if (cardHistory != null)
        {
            visible.addAll(cardHistory.all());
        }
        if (phase == Phase.PLAYER_TURN)
        {
            // History is newest first: only player hits follow the fourth (hole) card.
            visible.remove(playerCards.size() - 2);
        }
        return visible;
    }

    /** Fresh shoe minus publicly visible cards, including the hidden card as unseen. */
    private String unseenByRank(List<Card> visible)
    {
        int[] n = new int[11];
        for (int rank = 1; rank <= 9; rank++)
        {
            n[rank] = 4 * Game.DECKS;
        }
        n[10] = 16 * Game.DECKS;
        for (Card c : visible)
        {
            int value = c.rank().value();
            n[value == 11 ? 1 : value]--;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int rank = 0; rank <= 10; rank++)
        {
            sb.append(rank == 0 ? "" : ",").append(n[rank]);
        }
        return sb.append(']').toString();
    }

    private static String hand(Hand hand, List<Card> cards, boolean hideHole)
    {
        if (hand == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("{\"cards\":[");
        int shown = hideHole ? 1 : cards.size();
        for (int i = 0; i < shown; i++)
        {
            sb.append(i == 0 ? "" : ",").append(card(cards.get(i)));
        }
        sb.append("],\"holeHidden\":").append(hideHole);
        if (hideHole)
        {
            sb.append(",\"total\":").append(cards.get(0).rank().value());
        }
        else
        {
            sb.append(",\"total\":").append(hand.total())
              .append(",\"bust\":").append(hand.isBust())
              .append(",\"blackjack\":").append(hand.isBlackjack());
        }
        return sb.append('}').toString();
    }

    private static String card(Card c)
    {
        return "{\"rank\":" + quote(c.rank().label())
            + ",\"suit\":" + quote(c.suit().name())
            + ",\"value\":" + c.rank().value() + "}";
    }

    private static String numbers(double[] values)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            sb.append(i == 0 ? "" : ",").append(values[i]);
        }
        return sb.append(']').toString();
    }

    private static void field(StringBuilder sb, String name, String value)
    {
        sb.append(quote(name)).append(':').append(quote(value));
    }

    private static String quote(String text)
    {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : text.toCharArray())
        {
            if (ch == '"' || ch == '\\')
            {
                sb.append('\\').append(ch);
            }
            else if (ch < 0x20)
            {
                sb.append(String.format("\\u%04x", (int) ch));
            }
            else
            {
                sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    /**
     * Answers one /api/ request: applies the move named by the path, then
     * returns the state. A rejected move (bad amount, wrong phase) puts
     * the exception's message into the state's "message" field, which is
     * the web version of the console printing it and asking again.
     */
    private void handleApi(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            exchange.getRequestBody().readAllBytes();
            String path = exchange.getRequestURI().getPath();
            String move = path.substring("/api/".length());
            boolean post = exchange.getRequestMethod().equals("POST");
            String body;
            if (move.equals("state") || !post)
            {
                body = stateJson();
            }
            else
            {
                body = apply(move, exchange.getRequestURI().getQuery());
            }
            respond(exchange, 200, "application/json; charset=utf-8",
                body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private synchronized String apply(String move, String query)
    {
        try
        {
            switch (move)
            {
                case "deposit":
                    deposit(amountOf(query));
                    break;
                case "bet":
                    bet(amountOf(query));
                    break;
                case "hit":
                    hit();
                    break;
                case "stand":
                    stand();
                    break;
                case "next":
                    nextRound();
                    break;
                case "quit":
                    quit();
                    break;
                default:
                    message = "Unknown move: " + move;
            }
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            message = e.getMessage();
        }
        return stateJson();
    }

    /** Reads amount=N from the query; anything else is not a whole number. */
    private static int amountOf(String query)
    {
        String prefix = "amount=";
        if (query != null && query.startsWith(prefix))
        {
            try
            {
                return Integer.parseInt(query.substring(prefix.length()).trim());
            }
            catch (NumberFormatException e)
            {
                // fall through to the console's own message
            }
        }
        throw new IllegalArgumentException("Please enter a whole number.");
    }

    private static void handlePage(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            exchange.getRequestBody().readAllBytes();
            Path page = Path.of(PAGE);
            if (!Files.isRegularFile(page))
            {
                respond(exchange, 404, "text/plain; charset=utf-8",
                    (PAGE + " not found. Run from the project root, the folder holding src and web.")
                        .getBytes(StandardCharsets.UTF_8));
                return;
            }
            respond(exchange, 200, "text/html; charset=utf-8", Files.readAllBytes(page));
        }
    }

    private static void respond(HttpExchange exchange, int status, String type, byte[] body)
        throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody())
        {
            out.write(body);
        }
    }
}
