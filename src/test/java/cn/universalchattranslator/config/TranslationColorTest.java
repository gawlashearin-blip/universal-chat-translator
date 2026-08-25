package cn.universalchattranslator.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TranslationColorTest {
    @Test
    void exposesAllSixteenDistinctMinecraftColors() {
        assertEquals(16, TranslationColor.values().length);
        assertEquals(16, Arrays.stream(TranslationColor.values()).map(TranslationColor::formatting).distinct().count());
        assertEquals(TranslationColor.AQUA, new TranslatorConfig().getTranslationColor());
    }
}
