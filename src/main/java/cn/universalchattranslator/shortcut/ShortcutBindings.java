package cn.universalchattranslator.shortcut;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ShortcutBindings {
    public static final String UNBOUND = "key.keyboard.unknown";
    private static final Pattern KEYBOARD_KEY = Pattern.compile("key\\.keyboard\\.[a-z0-9._-]+");

    private ShortcutBindings() {
    }

    public static String normalize(String value) {
        if (value == null) return UNBOUND;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return KEYBOARD_KEY.matcher(normalized).matches() ? normalized : UNBOUND;
    }
}
