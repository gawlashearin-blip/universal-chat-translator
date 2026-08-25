package cn.universalchattranslator.shortcut;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortcutStateTrackerTest {
    @Test
    void triggersOncePerPressAndAgainAfterRelease() {
        ShortcutStateTracker tracker = new ShortcutStateTracker();
        Map<ShortcutAction, String> bindings = bindings();
        bindings.put(ShortcutAction.TOGGLE_PLAYER_TRANSLATION, "key.keyboard.p");
        Set<String> pressed = Set.of("key.keyboard.p");

        assertEquals(1, tracker.update(bindings, pressed::contains, false).triggered().size());
        assertTrue(tracker.update(bindings, pressed::contains, false).triggered().isEmpty());
        tracker.update(bindings, ignored -> false, false);
        assertEquals(1, tracker.update(bindings, pressed::contains, false).triggered().size());
    }

    @Test
    void screenSuppressesTriggerWithoutRetriggeringWhenClosed() {
        ShortcutStateTracker tracker = new ShortcutStateTracker();
        Map<ShortcutAction, String> bindings = bindings();
        bindings.put(ShortcutAction.OPEN_CONFIG, "key.keyboard.o");

        assertTrue(tracker.update(bindings, key -> true, true).triggered().isEmpty());
        assertTrue(tracker.update(bindings, key -> true, false).triggered().isEmpty());
        tracker.update(bindings, key -> false, false);
        assertEquals(ShortcutAction.OPEN_CONFIG,
                tracker.update(bindings, key -> true, false).triggered().getFirst());
    }

    @Test
    void duplicateBindingUsesFirstPriorityActionOnly() {
        ShortcutStateTracker tracker = new ShortcutStateTracker();
        Map<ShortcutAction, String> bindings = bindings();
        bindings.put(ShortcutAction.OPEN_CONFIG, "key.keyboard.f8");
        bindings.put(ShortcutAction.TOGGLE_PLAYER_TRANSLATION, "key.keyboard.f8");

        ShortcutStateTracker.TickResult result = tracker.update(bindings, key -> true, false);
        assertEquals(java.util.List.of(ShortcutAction.OPEN_CONFIG), result.triggered());
        assertEquals(java.util.List.of(ShortcutAction.TOGGLE_PLAYER_TRANSLATION), result.duplicates());
    }

    private static Map<ShortcutAction, String> bindings() {
        Map<ShortcutAction, String> values = new EnumMap<>(ShortcutAction.class);
        for (ShortcutAction action : ShortcutAction.values()) values.put(action, ShortcutBindings.UNBOUND);
        return values;
    }
}
