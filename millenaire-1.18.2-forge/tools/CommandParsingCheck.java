import java.util.List;
import java.util.Locale;
import org.millenaire.common.commands.MillCommandSupport;
import net.minecraft.commands.CommandRuntimeException;

public class CommandParsingCheck {
    public static void main(String[] args) {
        check(MillCommandSupport.parseInt("0") == 0, "zero");
        check(MillCommandSupport.parseInt("-2147483648") == Integer.MIN_VALUE, "negative reputation bound");
        check(MillCommandSupport.parseInt("2147483647") == Integer.MAX_VALUE, "positive reputation bound");
        for (String bad : List.of("", "12x", "1.5", "2147483648", "--1")) {
            try { MillCommandSupport.parseInt(bad); throw new AssertionError("Accepted " + bad); }
            catch (CommandRuntimeException expected) { }
        }
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            check(MillCommandSupport.matching(new String[]{"i"}, new String[]{"INUIT", "Norman"}).equals(List.of("INUIT")), "locale independent completion");
            check(MillCommandSupport.matching(new String[]{""}, new String[]{"all", "Player"}).size() == 2, "empty completion");
        } finally { Locale.setDefault(previous); }
        System.out.println("Command parsing checks passed");
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
