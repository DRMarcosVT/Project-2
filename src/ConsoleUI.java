import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

/**
 * Everything that touches the keyboard and the screen.
 *
 * Each prompt method keeps asking until it has a usable answer, so the
 * rest of the program never sees bad text: a non-number where a number
 * is expected, or a word other than hit or stand, is caught here and the
 * player is asked again.
 *
 * The Scanner and PrintStream are passed in rather than fixed to
 * System.in and System.out so the tests can feed typed lines from a
 * String and read back what was printed.
 *
 * @author Team
 * @version October 2026
 */
public class ConsoleUI
{
    private final Scanner in;      // where typed lines come from
    private final PrintStream out; // where messages go

    /**
     * Creates a UI over the given input and output.
     *
     * @param in  the source of typed lines, e.g. new Scanner(System.in)
     * @param out the destination for messages, e.g. System.out
     * @throws NullPointerException if either argument is null
     */
    public ConsoleUI(Scanner in, PrintStream out)
    {
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /**
     * Asks for a whole number and keeps asking until one is typed.
     * Anything Integer.parseInt rejects (letters, a blank line, a decimal)
     * prints "Please enter a whole number." and asks again.
     *
     * @param prompt the text shown before the cursor
     * @return the number typed
     * @throws IllegalStateException if the input ends before a number is
     *         typed (end of file)
     */
    public int promptInt(String prompt)
    {
        while (true)
        {
            String line = readLine(prompt);
            try
            {
                return Integer.parseInt(line.trim());
            }
            catch (NumberFormatException e)
            {
                out.println("Please enter a whole number.");
            }
        }
    }

    /**
     * Asks "Hit or stand?" and keeps asking until the answer is hit, h,
     * stand or s, in any letter case and with any surrounding spaces.
     * Anything else prints "Type hit or stand." and asks again.
     *
     * @return the chosen action
     * @throws IllegalStateException if the input ends first
     */
    public Action promptAction()
    {
        while (true)
        {
            String line = readLine("Hit or stand? ").trim().toLowerCase();
            if (line.equals("hit") || line.equals("h"))
            {
                return Action.HIT;
            }
            if (line.equals("stand") || line.equals("s"))
            {
                return Action.STAND;
            }
            out.println("Type hit or stand.");
        }
    }

    /**
     * Asks a yes/no question and keeps asking until the answer is y, yes,
     * n or no, in any letter case. Anything else prints "Type y or n." and
     * asks again.
     *
     * @param prompt the question shown before the cursor
     * @return true for yes, false for no
     * @throws IllegalStateException if the input ends first
     */
    public boolean promptYesNo(String prompt)
    {
        while (true)
        {
            String line = readLine(prompt).trim().toLowerCase();
            if (line.equals("y") || line.equals("yes"))
            {
                return true;
            }
            if (line.equals("n") || line.equals("no"))
            {
                return false;
            }
            out.println("Type y or n.");
        }
    }

    /**
     * Prints one line.
     *
     * @param message the text to show; an empty string prints a blank line
     */
    public void show(String message)
    {
        out.println(message);
    }

    /** Distinguishes EOF from a game-state error; preserves the public exception contract. */
    static class InputEndedException extends IllegalStateException
    {
        private static final long serialVersionUID = 1L;

        InputEndedException()
        {
            super("Input ended before the game finished");
        }
    }

    /**
     * Prints a prompt and reads the next typed line.
     *
     * @param prompt the text shown before the cursor
     * @return the line typed, without its line break
     * @throws IllegalStateException if there are no more lines to read
     */
    private String readLine(String prompt)
    {
        out.print(prompt);
        out.flush();
        if (!in.hasNextLine())
        {
            throw new InputEndedException();
        }
        return in.nextLine();
    }
}
