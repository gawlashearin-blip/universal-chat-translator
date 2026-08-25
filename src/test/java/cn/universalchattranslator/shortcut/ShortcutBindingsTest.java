package cn.universalchattranslator.shortcut;

import cn.universalchattranslator.config.TranslatorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortcutBindingsTest {
    @Test
    void defaultsAllFourShortcutsToUnbound() {
        TranslatorConfig config = new TranslatorConfig();
        assertEquals(ShortcutBindings.UNBOUND, config.getOpenConfigKey());
        assertEquals(ShortcutBindings.UNBOUND, config.getTogglePlayerTranslationKey());
        assertEquals(ShortcutBindings.UNBOUND, config.getToggleSystemTranslationKey());
        assertEquals(ShortcutBindings.UNBOUND, config.getToggleChineseToEnglishKey());
    }

    @Test
    void normalizesKeyboardKeysAndRejectsInvalidValues() {
        TranslatorConfig config = new TranslatorConfig();
        config.setOpenConfigKey(" KEY.KEYBOARD.F8 ");
        assertEquals("key.keyboard.f8", config.getOpenConfigKey());

        config.setTogglePlayerTranslationKey("key.mouse.left");
        config.setToggleSystemTranslationKey(null);
        assertEquals(ShortcutBindings.UNBOUND, config.getTogglePlayerTranslationKey());
        assertEquals(ShortcutBindings.UNBOUND, config.getToggleSystemTranslationKey());
    }
}
