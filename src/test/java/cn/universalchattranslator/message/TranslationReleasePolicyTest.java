package cn.universalchattranslator.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranslationReleasePolicyTest {
    @Test
    void suppressesCompletedIncomingTranslationAfterItsToggleIsDisabled() {
        ClassifiedMessage player = MessageClassifier.playerMessage("hello", "Steve", false);
        ClassifiedMessage system = new ClassifiedMessage(MessageKind.SYSTEM, "hello", "系统", false);

        assertFalse(TranslationReleasePolicy.displayIncoming(player, true, false, true));
        assertFalse(TranslationReleasePolicy.displayIncoming(system, true, true, false));
        assertFalse(TranslationReleasePolicy.displayIncoming(player, false, true, true));
        assertTrue(TranslationReleasePolicy.displayIncoming(player, true, true, false));
    }

    @Test
    void usesOriginalForPendingOutgoingTranslationAfterToggleIsDisabled() {
        assertFalse(TranslationReleasePolicy.useTranslatedOutgoing(true, true, false));
        assertFalse(TranslationReleasePolicy.useTranslatedOutgoing(true, false, true));
        assertTrue(TranslationReleasePolicy.useTranslatedOutgoing(true, true, true));
        assertTrue(TranslationReleasePolicy.useTranslatedOutgoing(false, false, false));
    }
}
