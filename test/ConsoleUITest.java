import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Tests for ConsoleUI. Typed lines come from a String through a Scanner,
 * and everything printed is captured in a buffer so the tests can count
 * the re-prompt messages.
 *
 * @author Team (GenAI-drafted; see README attribution)
 * @version October 2026
 */
public class ConsoleUITest
{
    private final ByteArrayOutputStream printed = new ByteArrayOutputStream();

    /** Builds a UI that reads the given typed lines and prints to the buffer. */
    private ConsoleUI uiReading(String typed)
    {
        return new ConsoleUI(new Scanner(typed), new PrintStream(printed, true));
    }

    /** Counts how many times a message was printed. */
    private int timesPrinted(String message)
    {
        String text = printed.toString();
        int count = 0;
        int at = text.indexOf(message);
        while (at >= 0)
        {
            count++;
            at = text.indexOf(message, at + 1);
        }
        return count;
    }

    // ---- constructor ----

    /** Normal: a UI over a scanner and a stream can be built and used. */
    @Test
    public void constructorBuildsUsableUI()
    {
        ConsoleUI ui = uiReading("");
        ui.show("ready");
        assertTrue(printed.toString().startsWith("ready"));
    }

    /** Bad input: either stream missing is rejected. */
    @Test
    public void constructorRejectsNull()
    {
        assertThrows(NullPointerException.class,
            () -> new ConsoleUI(null, System.out));
        assertThrows(NullPointerException.class,
            () -> new ConsoleUI(new Scanner(""), null));
    }

    // ---- promptInt() ----

    /** Normal: a number on the first try. */
    @Test
    public void promptIntReadsNumber()
    {
        assertEquals(25, uiReading("25\n").promptInt("Bet: "));
        assertEquals(0, timesPrinted("Please enter a whole number."));
    }

    /** Bad input: a word, a blank line and a decimal are each re-prompted. */
    @Test
    public void promptIntRepromptsUntilNumber()
    {
        assertEquals(25, uiReading("ten\n\n5.5\n25\n").promptInt("Bet: "));
        assertEquals(3, timesPrinted("Please enter a whole number."));
    }

    // ---- promptAction() ----

    /** Normal: letter case does not matter. */
    @Test
    public void promptActionReadsHit()
    {
        assertEquals(Action.HIT, uiReading("HIT\n").promptAction());
    }

    /** Bad input: other words are re-prompted; spaces around s are fine. */
    @Test
    public void promptActionRepromptsUntilHitOrStand()
    {
        assertEquals(Action.STAND, uiReading("double\nsplit\n s \n").promptAction());
        assertEquals(2, timesPrinted("Type hit or stand."));
    }

    // ---- promptYesNo() ----

    /** Normal: y is yes. */
    @Test
    public void promptYesNoReadsYes()
    {
        assertTrue(uiReading("y\n").promptYesNo("Again? "));
    }

    /** Bad input: anything else is re-prompted until y or n. */
    @Test
    public void promptYesNoRepromptsUntilAnswer()
    {
        assertFalse(uiReading("maybe\nno\n").promptYesNo("Again? "));
        assertEquals(1, timesPrinted("Type y or n."));
    }

    // ---- show() ----

    /** Normal: the message and a line break. */
    @Test
    public void showPrintsLine()
    {
        uiReading("").show("hi");
        assertEquals("hi" + System.lineSeparator(), printed.toString());
    }

    /** Edge: an empty message prints a bare line break, no crash. */
    @Test
    public void showPrintsBlankLineForEmptyMessage()
    {
        uiReading("").show("");
        assertEquals(System.lineSeparator(), printed.toString());
    }

    // ---- input ending early ----

    /** Bad input: running out of lines is reported, not an obscure crash. */
    @Test
    public void promptThrowsWhenInputEnds()
    {
        ConsoleUI ui = uiReading("");
        assertThrows(IllegalStateException.class, () -> ui.promptInt("Bet: "));
    }
}
