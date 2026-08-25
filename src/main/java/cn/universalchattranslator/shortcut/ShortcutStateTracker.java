package cn.universalchattranslator.shortcut;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ShortcutStateTracker {
    private final Map<ShortcutAction, Boolean> previouslyPressed = new EnumMap<>(ShortcutAction.class);

    public TickResult update(Map<ShortcutAction, String> bindings, Predicate<String> isPressed, boolean screenOpen) {
        List<ShortcutAction> triggered = new ArrayList<>();
        List<ShortcutAction> duplicates = new ArrayList<>();
        Set<String> claimedKeys = new HashSet<>();

        for (ShortcutAction action : ShortcutAction.values()) {
            String key = ShortcutBindings.normalize(bindings.get(action));
            boolean duplicate = !ShortcutBindings.UNBOUND.equals(key) && !claimedKeys.add(key);
            if (duplicate) duplicates.add(action);

            boolean pressed = !duplicate && !ShortcutBindings.UNBOUND.equals(key) && isPressed.test(key);
            boolean previous = previouslyPressed.getOrDefault(action, false);
            previouslyPressed.put(action, pressed);
            if (pressed && !previous && !screenOpen) triggered.add(action);
        }
        return new TickResult(List.copyOf(triggered), List.copyOf(duplicates));
    }

    public record TickResult(List<ShortcutAction> triggered, List<ShortcutAction> duplicates) {
    }
}
