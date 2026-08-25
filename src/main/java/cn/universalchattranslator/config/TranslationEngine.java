package cn.universalchattranslator.config;

public enum TranslationEngine {
    GOOGLE_FREE("Google Translate (free)"),
    CUSTOM_OPENAI("Custom OpenAI-compatible API");

    private final String displayName;

    TranslationEngine(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
