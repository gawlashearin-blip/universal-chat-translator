package cn.universalchattranslator.message;

public final class TranslationReleasePolicy {
    private TranslationReleasePolicy() {
    }

    public static boolean displayIncoming(
            ClassifiedMessage message, boolean modEnabled,
            boolean playerTranslationEnabled, boolean systemTranslationEnabled) {
        return modEnabled && message.enabledBy(playerTranslationEnabled, systemTranslationEnabled);
    }

    public static boolean useTranslatedOutgoing(
            boolean requestWasTranslated, boolean modEnabled, boolean outgoingTranslationEnabled) {
        return !requestWasTranslated || (modEnabled && outgoingTranslationEnabled);
    }
}
