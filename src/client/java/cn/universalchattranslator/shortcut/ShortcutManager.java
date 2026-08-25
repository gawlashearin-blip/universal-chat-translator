package cn.universalchattranslator.shortcut;

import cn.universalchattranslator.UniversalChatTranslator;
import cn.universalchattranslator.config.CustomConfigScreen;
import cn.universalchattranslator.config.TranslatorConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ShortcutManager {
    private final ShortcutStateTracker stateTracker = new ShortcutStateTracker();
    private final Set<ShortcutAction> warnedDuplicates = new HashSet<>();

    public void tick(MinecraftClient client) {
        TranslatorConfig config = TranslatorConfig.get();
        Map<ShortcutAction, String> bindings = bindingsFrom(config);
        ShortcutStateTracker.TickResult result = stateTracker.update(
                bindings, key -> isPressed(client, key), client.currentScreen != null);

        warnedDuplicates.retainAll(result.duplicates());
        for (ShortcutAction duplicate : result.duplicates()) {
            if (warnedDuplicates.add(duplicate)) {
                UniversalChatTranslator.LOGGER.warn(
                        "Ignoring duplicate shortcut binding for {}", duplicate.name());
            }
        }
        for (ShortcutAction action : result.triggered()) execute(client, config, action);
    }

    private Map<ShortcutAction, String> bindingsFrom(TranslatorConfig config) {
        Map<ShortcutAction, String> bindings = new EnumMap<>(ShortcutAction.class);
        bindings.put(ShortcutAction.OPEN_CONFIG, config.getOpenConfigKey());
        bindings.put(ShortcutAction.TOGGLE_PLAYER_TRANSLATION, config.getTogglePlayerTranslationKey());
        bindings.put(ShortcutAction.TOGGLE_SYSTEM_TRANSLATION, config.getToggleSystemTranslationKey());
        bindings.put(ShortcutAction.TOGGLE_CHINESE_TO_ENGLISH, config.getToggleChineseToEnglishKey());
        return bindings;
    }

    private boolean isPressed(MinecraftClient client, String translationKey) {
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
            return key.getCategory() == InputUtil.Type.KEYSYM
                    && key != InputUtil.UNKNOWN_KEY
                    && InputUtil.isKeyPressed(client.getWindow(), key.getCode());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void execute(MinecraftClient client, TranslatorConfig config, ShortcutAction action) {
        switch (action) {
            case OPEN_CONFIG -> client.setScreen(CustomConfigScreen.create(null));
            case TOGGLE_PLAYER_TRANSLATION -> {
                config.setTranslatePlayerMessages(!config.isTranslatePlayerMessages());
                saveAndNotify(client, "player", config.isTranslatePlayerMessages());
            }
            case TOGGLE_SYSTEM_TRANSLATION -> {
                config.setTranslateSystemMessages(!config.isTranslateSystemMessages());
                saveAndNotify(client, "system", config.isTranslateSystemMessages());
            }
            case TOGGLE_CHINESE_TO_ENGLISH -> {
                config.setChineseToEnglish(!config.isChineseToEnglish());
                saveAndNotify(client, "outgoing", config.isChineseToEnglish());
            }
        }
    }

    private void saveAndNotify(MinecraftClient client, String feature, boolean enabled) {
        try {
            AutoConfig.getConfigHolder(TranslatorConfig.class).save();
        } catch (RuntimeException error) {
            UniversalChatTranslator.LOGGER.warn("Could not save shortcut toggle state: {}", error.getMessage());
        }
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(
                    "text.universal-chat-translator.shortcut.status",
                    Text.translatable("text.universal-chat-translator.shortcut.feature." + feature),
                    Text.translatable("text.universal-chat-translator.shortcut.state."
                            + (enabled ? "enabled" : "disabled"))), true);
        }
    }
}
