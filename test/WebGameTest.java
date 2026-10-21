import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests for WebGame: the constructor, the six moves, phase(),
 * stateJson(), start() and main().
 *
 * The deals use the same seeds as GameTest because WebGame builds its
 * Shoe from the Random exactly as Game does. Seed 1 deals 6H 9D and wins
 * on a stand; seed 2 loses on a stand; seed 3 deals 7H QS and busts on a
 * hit; seed 16 deals a player blackjack; seed 20 deals a dealer
 * blackjack. Every one was found by running the game once.
 *
 * @author Team
 * @version October 2026
 */
public class WebGameTest
{
    /** A session with $100 deposited and the first $10 bet placed. */
    private static WebGame dealt(int seed)
    {
        WebGame game = new WebGame(new Random(seed));
        game.deposit(100);
        game.bet(10);
        return game;
    }

    /** Reads the value of a top-level number or string field as text. */
    private static String fieldOf(String json, String name)
    {
        String key = "\"" + name + "\":";
        int start = json.indexOf(key) + key.length();
        int end = start;
        while (",}".indexOf(json.charAt(end)) < 0)
        {
            end++;
        }
        return json.substring(start, end).replace("\"", "");
    }

    /** Sends one request to a running server and returns the body. */
    private static String request(HttpServer server, String method, String path)
        throws IOException
    {
        URL url = new URL("http://127.0.0.1:" + server.getAddress().getPort() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        try (InputStream in = connection.getInputStream())
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---- constructor and phase() ----

    /** Normal: a new session waits for the deposit. */
    @Test
    public void constructorStartsAtDeposit()
    {
        assertEquals(WebGame.Phase.DEPOSIT, new WebGame(new Random(1)).phase());
    }

    /** Bad input: no random source. */
    @Test
    public void constructorRejectsNullRandom()
    {
        assertThrows(NullPointerException.class, () -> new WebGame(null));
    }

    // ---- deposit() ----

    /** Normal: the deposit opens the shoe and the counter's advice. */
    @Test
    public void depositOpensBankrollAndAsksForBet()
    {
        WebGame game = new WebGame(new Random(1));
        game.deposit(100);
        assertEquals(WebGame.Phase.BETTING, game.phase());
        String json = game.stateJson();
        assertEquals("100", fieldOf(json, "balance"));
        assertEquals("156", fieldOf(json, "remaining"));
        assertTrue(json.contains("\"advice\":{\"edge\":-0.0"),
            "a fresh three-deck shoe favours the dealer");
        assertTrue(json.contains("\"suggested\":1"));
    }

    /** Bad input: zero, negative, and a second deposit. */
    @Test
    public void depositRejectsBadAmountsAndRepeats()
    {
        WebGame game = new WebGame(new Random(1));
        assertThrows(IllegalArgumentException.class, () -> game.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> game.deposit(-50));
        assertEquals(WebGame.Phase.DEPOSIT, game.phase());
        game.deposit(100);
        assertThrows(IllegalStateException.class, () -> game.deposit(100));
        assertEquals("100", fieldOf(game.stateJson(), "balance"));
    }

    // ---- bet() ----

    /** Normal: the bet is taken, four cards are dealt, the hole card is hidden. */
    @Test
    public void betDealsAndHidesDealerHoleCard()
    {
        WebGame game = dealt(1);
        assertEquals(WebGame.Phase.PLAYER_TURN, game.phase());
        String json = game.stateJson();
        assertEquals("90", fieldOf(json, "balance"));
        assertEquals("10", fieldOf(json, "bet"));
        assertEquals("1", fieldOf(json, "round"));
        assertTrue(json.contains("\"player\":{\"cards\":[{\"rank\":\"6\",\"suit\":\"HEARTS\""));
        int dealer = json.indexOf("\"dealer\":{");
        String dealerJson = json.substring(dealer, json.indexOf("}]", dealer));
        assertEquals(1, dealerJson.split("\"rank\"").length - 1, "only the up card is sent");
        assertTrue(json.contains("\"holeHidden\":true"));
        assertEquals("152", fieldOf(json, "remaining"));
    }

    /** Bad input: zero, negative, more than the balance, wrong phase. */
    @Test
    public void betRejectsBadAmountsAndWrongPhase()
    {
        WebGame game = new WebGame(new Random(1));
        assertThrows(IllegalStateException.class, () -> game.bet(10));
        game.deposit(100);
        assertThrows(IllegalArgumentException.class, () -> game.bet(0));
        assertThrows(IllegalArgumentException.class, () -> game.bet(-5));
        assertThrows(IllegalArgumentException.class, () -> game.bet(500));
        assertEquals(WebGame.Phase.BETTING, game.phase());
        assertEquals("100", fieldOf(game.stateJson(), "balance"));
    }

    /** Edge: a player blackjack (seed 16) and a dealer blackjack (seed 20) settle at once. */
    @Test
    public void betSettlesBlackjacksWithoutATurn()
    {
        WebGame natural = dealt(16);
        assertEquals(WebGame.Phase.ROUND_OVER, natural.phase());
        assertEquals("BLACKJACK", fieldOf(natural.stateJson(), "outcome"));
        assertEquals("115", fieldOf(natural.stateJson(), "balance"));

        WebGame dealerNatural = dealt(20);
        assertEquals(WebGame.Phase.ROUND_OVER, dealerNatural.phase());
        assertEquals("LOSS", fieldOf(dealerNatural.stateJson(), "outcome"));
        assertEquals("90", fieldOf(dealerNatural.stateJson(), "balance"));
    }

    // ---- hit() ----

    /** Normal: a hit adds a card and the turn goes on (seed 1: 6H 9D then an ace). */
    @Test
    public void hitAddsACardAndKeepsTheTurn()
    {
        WebGame game = dealt(1);
        game.hit();
        assertEquals(WebGame.Phase.PLAYER_TURN, game.phase());
        assertTrue(game.stateJson().contains("\"total\":16,"));
        assertEquals("151", fieldOf(game.stateJson(), "remaining"));
    }

    /** Bad input: hitting outside the player's turn. */
    @Test
    public void hitRejectsWrongPhase()
    {
        WebGame game = new WebGame(new Random(1));
        assertThrows(IllegalStateException.class, game::hit);
        game.deposit(100);
        assertThrows(IllegalStateException.class, game::hit);
    }

    /** Edge: a bust (seed 3: 7H QS then a ten) ends the round and forfeits the bet. */
    @Test
    public void hitBustEndsRound()
    {
        WebGame game = dealt(3);
        game.hit();
        assertEquals(WebGame.Phase.ROUND_OVER, game.phase());
        String json = game.stateJson();
        assertEquals("BUST", fieldOf(json, "outcome"));
        assertEquals("-10", fieldOf(json, "net"));
        assertEquals("90", fieldOf(json, "balance"));
        assertEquals("1", fieldOf(json, "losses"));
    }

    // ---- stand() ----

    /** Normal: seed 1 wins on a stand and the dealer's hand is revealed. */
    @Test
    public void standPlaysDealerAndSettles()
    {
        WebGame game = dealt(1);
        game.stand();
        assertEquals(WebGame.Phase.ROUND_OVER, game.phase());
        String json = game.stateJson();
        assertEquals("WIN", fieldOf(json, "outcome"));
        assertEquals("110", fieldOf(json, "balance"));
        assertTrue(json.contains("\"holeHidden\":false"));
        assertTrue(json.contains("\"history\":[{\"round\":1,\"outcome\":\"WIN\",\"bet\":10,\"net\":10}]"));
    }

    /** Bad input: standing outside the player's turn. */
    @Test
    public void standRejectsWrongPhase()
    {
        WebGame game = dealt(1);
        game.stand();
        assertThrows(IllegalStateException.class, game::stand);
    }

    /** Edge: losing the whole bankroll ends the session (seed 2 loses on a stand). */
    @Test
    public void standThatBreaksThePlayerEndsSession()
    {
        WebGame game = new WebGame(new Random(2));
        game.deposit(10);
        game.bet(10);
        game.stand();
        assertEquals(WebGame.Phase.OVER, game.phase());
        assertEquals("0", fieldOf(game.stateJson(), "balance"));
        assertThrows(IllegalStateException.class, game::nextRound);
    }

    // ---- nextRound() ----

    /** Normal: after a round, fresh advice is computed and a bet is asked for. */
    @Test
    public void nextRoundRecomputesAdvice()
    {
        WebGame game = dealt(1);
        game.stand();
        String before = fieldOf(game.stateJson(), "edge");
        game.nextRound();
        assertEquals(WebGame.Phase.BETTING, game.phase());
        String json = game.stateJson();
        assertFalse(before.equals(fieldOf(json, "edge")), "seen cards change the edge");
        assertEquals("0", fieldOf(json, "bet"));
        assertTrue(json.contains("\"shuffled\":false"));
    }

    /** Bad input: asking for the next round before this one is over. */
    @Test
    public void nextRoundRejectsWrongPhase()
    {
        WebGame game = dealt(1);
        assertThrows(IllegalStateException.class, game::nextRound);
        assertEquals(WebGame.Phase.PLAYER_TURN, game.phase());
    }

    /** Edge: the shoe is replaced when fewer than Shoe.CUT_CARD cards remain. */
    @Test
    public void nextRoundShufflesBelowCutCard()
    {
        WebGame game = new WebGame(new Random(1));
        game.deposit(1000);
        boolean shuffled = false;
        for (int i = 0; i < 40 && game.phase() != WebGame.Phase.OVER && !shuffled; i++)
        {
            game.bet(1);
            if (game.phase() == WebGame.Phase.PLAYER_TURN)
            {
                game.stand();
            }
            game.nextRound();
            shuffled = game.stateJson().contains("\"shuffled\":true");
        }
        assertTrue(shuffled, "forty one-dollar rounds use more than one shoe");
        String json = game.stateJson();
        assertEquals("156", fieldOf(json, "remaining"));
        assertEquals("0", fieldOf(json, "seen"));
        assertTrue(json.contains("\"cardsDealt\":[]"));
        assertFalse(json.contains("\"history\":[]"), "the round history survives a shuffle");
    }

    // ---- quit() ----

    /** Normal: leaving after a round, or before a bet, ends the session. */
    @Test
    public void quitEndsSession()
    {
        WebGame afterRound = dealt(1);
        afterRound.stand();
        afterRound.quit();
        assertEquals(WebGame.Phase.OVER, afterRound.phase());

        WebGame beforeBet = new WebGame(new Random(1));
        beforeBet.deposit(100);
        beforeBet.quit();
        assertEquals(WebGame.Phase.OVER, beforeBet.phase());
        assertEquals("100", fieldOf(beforeBet.stateJson(), "balance"));
    }

    /** Bad input: leaving with a bet on the table, or before the deposit. */
    @Test
    public void quitRejectsMidRoundAndBeforeDeposit()
    {
        assertThrows(IllegalStateException.class, () -> new WebGame(new Random(1)).quit());
        WebGame game = dealt(1);
        assertThrows(IllegalStateException.class, game::quit);
        assertEquals(WebGame.Phase.PLAYER_TURN, game.phase());
        assertEquals("90", fieldOf(game.stateJson(), "balance"));
    }

    // ---- stateJson() ----

    /** Normal: a fresh session is valid JSON with empty tables. */
    @Test
    public void stateJsonOfFreshSession()
    {
        String json = new WebGame(new Random(1)).stateJson();
        assertTrue(json.startsWith("{\"phase\":\"DEPOSIT\""));
        assertTrue(json.endsWith("\"cardsDealt\":[]}"));
        assertTrue(json.contains("\"shoe\":null"));
        assertTrue(json.contains("\"advice\":null"));
        assertTrue(json.contains("\"player\":null"));
        assertTrue(json.contains("\"history\":[]"));
        assertEquals("0", fieldOf(json, "balance"));
    }

    /** Edge: the unseen-by-rank counts drop as cards are dealt. */
    @Test
    public void stateJsonCountsUnseenCardsByRank()
    {
        WebGame game = new WebGame(new Random(1));
        game.deposit(100);
        assertTrue(game.stateJson().contains("\"unseenByRank\":[0,12,12,12,12,12,12,12,12,12,48]"));
        game.bet(10);                       // 6H 9D for the player, two dealer cards
        String json = game.stateJson();
        int open = json.indexOf("\"unseenByRank\":[") + "\"unseenByRank\":[".length();
        String counts = json.substring(open, json.indexOf(']', open));
        int sum = 0;
        for (String n : counts.split(","))
        {
            sum += Integer.parseInt(n);
        }
        assertEquals(152, sum);
    }

    // ---- start() ----

    /** Normal: the server serves the page, the state, and applies moves. */
    @Test
    public void startServesPageAndMoves() throws IOException
    {
        HttpServer server = WebGame.start(new WebGame(new Random(1)), 0);
        try
        {
            assertTrue(request(server, "GET", "/").contains("<html"));
            String state = request(server, "GET", "/api/state");
            assertEquals("DEPOSIT", fieldOf(state, "phase"));

            String rejected = request(server, "POST", "/api/deposit?amount=abc");
            assertEquals("Please enter a whole number.", fieldOf(rejected, "message"));
            assertEquals("DEPOSIT", fieldOf(rejected, "phase"));

            String negative = request(server, "POST", "/api/deposit?amount=-5");
            assertEquals("Deposit must be greater than zero", fieldOf(negative, "message"));

            String deposited = request(server, "POST", "/api/deposit?amount=100");
            assertEquals("BETTING", fieldOf(deposited, "phase"));
            assertEquals("", fieldOf(deposited, "message"));

            String tooMuch = request(server, "POST", "/api/bet?amount=500");
            assertEquals("Bet of $500 exceeds balance of $100", fieldOf(tooMuch, "message"));

            String bet = request(server, "POST", "/api/bet?amount=10");
            assertEquals("PLAYER_TURN", fieldOf(bet, "phase"));
            String stood = request(server, "POST", "/api/stand");
            assertEquals("ROUND_OVER", fieldOf(stood, "phase"));
            assertEquals("110", fieldOf(stood, "balance"));

            String unknown = request(server, "POST", "/api/dance");
            assertEquals("Unknown move: dance", fieldOf(unknown, "message"));

            String fresh = request(server, "POST", "/api/restart");
            assertEquals("DEPOSIT", fieldOf(fresh, "phase"));
            assertEquals("0", fieldOf(fresh, "balance"));
        }
        finally
        {
            server.stop(0);
        }
    }

    /** Bad input: no game, and a port that cannot exist. */
    @Test
    public void startRejectsNullGameAndBadPort()
    {
        assertThrows(NullPointerException.class, () -> WebGame.start(null, 0));
        assertThrows(IllegalArgumentException.class,
            () -> WebGame.start(new WebGame(new Random(1)), -1));
    }

    // ---- main() ----

    /**
     * Normal: main starts a server on the given port and prints its
     * address. Port 0 lets the system choose, and the headless property
     * stops main opening a browser. main has no handle to stop the server
     * with, so this one lives until the test JVM exits.
     */
    @Test
    public void mainStartsServerAndPrintsAddress() throws IOException
    {
        System.setProperty("java.awt.headless", "true");
        PrintStream original = System.out;
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        System.setOut(new PrintStream(printed, true));
        try
        {
            WebGame.main(new String[] {"0"});
        }
        finally
        {
            System.setOut(original);
        }
        String out = printed.toString();
        String prefix = "Blackjack table at http://127.0.0.1:";
        assertTrue(out.startsWith(prefix), out);
        int port = Integer.parseInt(out.substring(prefix.length(), out.indexOf('/', prefix.length())));
        assertTrue(port > 0);
    }

    /** Bad input: a port that is not a number prints the usage and returns. */
    @Test
    public void mainRejectsNonNumericPort() throws IOException
    {
        PrintStream original = System.out;
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        System.setOut(new PrintStream(printed, true));
        try
        {
            WebGame.main(new String[] {"eighty"});
        }
        finally
        {
            System.setOut(original);
        }
        assertEquals("Usage: java -cp out WebGame [port]", printed.toString().trim());
    }
}
