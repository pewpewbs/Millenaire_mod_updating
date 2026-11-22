package org.millenaire.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

/** Reads the named LWJGL 2 settings without reusing their obsolete numeric codes. */
public final class LegacyKeyNames {
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("LCONTROL", "LEFT_CONTROL"), Map.entry("RCONTROL", "RIGHT_CONTROL"),
        Map.entry("LSHIFT", "LEFT_SHIFT"), Map.entry("RSHIFT", "RIGHT_SHIFT"),
        Map.entry("LMENU", "LEFT_ALT"), Map.entry("RMENU", "RIGHT_ALT"),
        Map.entry("LMETA", "LEFT_SUPER"), Map.entry("RMETA", "RIGHT_SUPER"),
        Map.entry("RETURN", "ENTER"), Map.entry("BACK", "BACKSPACE"),
        Map.entry("PRIOR", "PAGE_UP"), Map.entry("NEXT", "PAGE_DOWN"),
        Map.entry("CAPITAL", "CAPS_LOCK"), Map.entry("NUMLOCK", "NUM_LOCK"),
        Map.entry("SCROLL", "SCROLL_LOCK"), Map.entry("LBRACKET", "LEFT_BRACKET"),
        Map.entry("RBRACKET", "RIGHT_BRACKET"), Map.entry("GRAVE", "GRAVE_ACCENT"),
        Map.entry("APOSTROPHE", "APOSTROPHE"), Map.entry("MULTIPLY", "KP_MULTIPLY"),
        Map.entry("ADD", "KP_ADD"), Map.entry("SUBTRACT", "KP_SUBTRACT"),
        Map.entry("DIVIDE", "KP_DIVIDE"), Map.entry("DECIMAL", "KP_DECIMAL"),
        Map.entry("NUMPADENTER", "KP_ENTER"), Map.entry("NUMPADEQUALS", "KP_EQUAL"),
        Map.entry("SYSRQ", "PRINT_SCREEN"));
    private LegacyKeyNames() {}
    public static int parse(String name) {
        if (name == null || name.isBlank()) return GLFW.GLFW_KEY_UNKNOWN;
        String key = name.trim();
        if (key.startsWith("key.keyboard.")) {
            try { return InputConstants.getKey(key).getValue(); }
            catch (IllegalArgumentException exception) { return GLFW.GLFW_KEY_UNKNOWN; }
        }
        key = key.toUpperCase(Locale.ROOT);
        key = ALIASES.getOrDefault(key, key);
        if (key.matches("NUMPAD[0-9]")) key = "KP_" + key.charAt(6);
        try { return GLFW.class.getField("GLFW_KEY_" + key).getInt(null); }
        catch (ReflectiveOperationException exception) { return GLFW.GLFW_KEY_UNKNOWN; }
    }
}
