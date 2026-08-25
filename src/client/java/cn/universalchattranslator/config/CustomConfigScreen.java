package cn.universalchattranslator.config;

import cn.universalchattranslator.util.CustomOpenAiTranslator;
import cn.universalchattranslator.util.TranslationException;
import cn.universalchattranslator.shortcut.ShortcutBindings;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CustomConfigScreen {
    private CustomConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TranslatorConfig config = TranslatorConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.universal-chat-translator.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("text.universal-chat-translator.category.general"));

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.universal-chat-translator.option.enabled"), config.isEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(config::setEnabled)
                .build());
        EnumListEntry<TranslationEngine> engine = entries.startEnumSelector(
                        Text.translatable("text.universal-chat-translator.option.engine"),
                        TranslationEngine.class, config.getTranslationEngine())
                .setDefaultValue(TranslationEngine.GOOGLE_FREE)
                .setEnumNameProvider(value -> Text.translatable(
                        "text.universal-chat-translator.engine." + value.name().toLowerCase()))
                .setSaveConsumer(config::setTranslationEngine)
                .build();
        general.addEntry(engine);
        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.universal-chat-translator.option.chineseToEnglish"),
                        config.isChineseToEnglish())
                .setDefaultValue(true)
                .setSaveConsumer(config::setChineseToEnglish)
                .build());
        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.universal-chat-translator.option.translatePlayerMessages"),
                        config.isTranslatePlayerMessages())
                .setDefaultValue(true)
                .setSaveConsumer(config::setTranslatePlayerMessages)
                .build());
        general.addEntry(entries.startBooleanToggle(
                        Text.translatable("text.universal-chat-translator.option.translateSystemMessages"),
                        config.isTranslateSystemMessages())
                .setDefaultValue(false)
                .setSaveConsumer(config::setTranslateSystemMessages)
                .build());
        general.addEntry(entries.startEnumSelector(
                        Text.translatable("text.universal-chat-translator.option.translationColor"),
                        TranslationColor.class, config.getTranslationColor())
                .setDefaultValue(TranslationColor.AQUA)
                .setEnumNameProvider(value -> Text.translatable(
                        "text.universal-chat-translator.color." + value.name().toLowerCase()))
                .setSaveConsumer(config::setTranslationColor)
                .build());

        SubCategoryBuilder shortcuts = entries.startSubCategory(
                Text.translatable("text.universal-chat-translator.category.shortcuts"));
        List<KeyCodeEntry> shortcutEntries = new ArrayList<>();
        Function<InputUtil.Key, Optional<Text>> duplicateError = candidate -> {
            if (candidate == InputUtil.UNKNOWN_KEY) return Optional.empty();
            long matches = shortcutEntries.stream()
                    .filter(entry -> entry.getValue().getKeyCode().equals(candidate))
                    .count();
            return matches > 1
                    ? Optional.of(Text.translatable("text.universal-chat-translator.error.duplicateKey"))
                    : Optional.empty();
        };

        KeyCodeEntry openConfigKey = entries.startKeyCodeField(
                        Text.translatable("text.universal-chat-translator.shortcut.openConfig"),
                        parseKeyboardKey(config.getOpenConfigKey()))
                .setDefaultValue(InputUtil.UNKNOWN_KEY)
                .setAllowModifiers(false)
                .setAllowMouse(false)
                .setErrorSupplier(duplicateError)
                .setKeySaveConsumer(key -> config.setOpenConfigKey(key.getTranslationKey()))
                .build();
        shortcutEntries.add(openConfigKey);
        shortcuts.add(openConfigKey);

        KeyCodeEntry playerKey = entries.startKeyCodeField(
                        Text.translatable("text.universal-chat-translator.shortcut.togglePlayer"),
                        parseKeyboardKey(config.getTogglePlayerTranslationKey()))
                .setDefaultValue(InputUtil.UNKNOWN_KEY)
                .setAllowModifiers(false)
                .setAllowMouse(false)
                .setErrorSupplier(duplicateError)
                .setKeySaveConsumer(key -> config.setTogglePlayerTranslationKey(key.getTranslationKey()))
                .build();
        shortcutEntries.add(playerKey);
        shortcuts.add(playerKey);

        KeyCodeEntry systemKey = entries.startKeyCodeField(
                        Text.translatable("text.universal-chat-translator.shortcut.toggleSystem"),
                        parseKeyboardKey(config.getToggleSystemTranslationKey()))
                .setDefaultValue(InputUtil.UNKNOWN_KEY)
                .setAllowModifiers(false)
                .setAllowMouse(false)
                .setErrorSupplier(duplicateError)
                .setKeySaveConsumer(key -> config.setToggleSystemTranslationKey(key.getTranslationKey()))
                .build();
        shortcutEntries.add(systemKey);
        shortcuts.add(systemKey);

        KeyCodeEntry outgoingKey = entries.startKeyCodeField(
                        Text.translatable("text.universal-chat-translator.shortcut.toggleOutgoing"),
                        parseKeyboardKey(config.getToggleChineseToEnglishKey()))
                .setDefaultValue(InputUtil.UNKNOWN_KEY)
                .setAllowModifiers(false)
                .setAllowMouse(false)
                .setErrorSupplier(duplicateError)
                .setKeySaveConsumer(key -> config.setToggleChineseToEnglishKey(key.getTranslationKey()))
                .build();
        shortcutEntries.add(outgoingKey);
        shortcuts.add(outgoingKey);
        general.addEntry(shortcuts.build());

        SubCategoryBuilder custom = entries.startSubCategory(
                Text.translatable("text.universal-chat-translator.category.custom"));

        StringListEntry baseUrl = entries.startStrField(
                        Text.translatable("text.universal-chat-translator.option.baseUrl"), config.getCustomBaseUrl())
                .setDefaultValue("")
                .setSaveConsumer(config::setCustomBaseUrl)
                .setErrorSupplier(value -> engine.getValue() == TranslationEngine.CUSTOM_OPENAI
                        ? validateBaseUrl(value) : Optional.empty())
                .setTooltipSupplier(value -> CustomOpenAiTranslator.isInsecureRemoteHttp(value)
                        ? Optional.of(new Text[]{Text.translatable("text.universal-chat-translator.warning.http")
                        .formatted(Formatting.RED)})
                        : Optional.of(new Text[]{Text.translatable("text.universal-chat-translator.option.baseUrl.tooltip")}))
                .build();
        custom.add(baseUrl);

        StringListEntry model = entries.startStrField(
                        Text.translatable("text.universal-chat-translator.option.model"), config.getCustomModel())
                .setDefaultValue("")
                .setSaveConsumer(config::setCustomModel)
                .setErrorSupplier(value -> engine.getValue() == TranslationEngine.CUSTOM_OPENAI
                        && (value == null || value.isBlank())
                        ? Optional.of(Text.translatable("text.universal-chat-translator.error.required"))
                        : Optional.empty())
                .build();
        custom.add(model);

        StringListEntry apiKey = entries.startStrField(
                        Text.translatable("text.universal-chat-translator.option.apiKey"), config.getCustomApiKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.universal-chat-translator.option.apiKey.tooltip")
                        .formatted(Formatting.YELLOW))
                .setSaveConsumer(config::setCustomApiKey)
                .build();
        custom.add(apiKey);

        StringListEntry systemPrompt = entries.startStrField(
                        Text.translatable("text.universal-chat-translator.option.systemPrompt"),
                        config.getCustomSystemPrompt())
                .setDefaultValue(TranslatorConfig.DEFAULT_SYSTEM_PROMPT)
                .setTooltip(Text.translatable("text.universal-chat-translator.option.systemPrompt.tooltip"))
                .setSaveConsumer(config::setCustomSystemPrompt)
                .build();
        custom.add(systemPrompt);

        IntegerListEntry timeout = entries.startIntField(
                        Text.translatable("text.universal-chat-translator.option.timeout"),
                        config.getCustomTimeoutSeconds())
                .setDefaultValue(30)
                .setMin(1)
                .setMax(120)
                .setSaveConsumer(config::setCustomTimeoutSeconds)
                .build();
        custom.add(timeout);

        DoubleListEntry temperature = entries.startDoubleField(
                        Text.translatable("text.universal-chat-translator.option.temperature"),
                        config.getCustomTemperature())
                .setDefaultValue(0.0)
                .setMin(0.0)
                .setMax(2.0)
                .setSaveConsumer(config::setCustomTemperature)
                .build();
        custom.add(temperature);

        general.addEntry(custom.build());
        general.addEntry(new ConnectionTestEntry(
                Text.translatable("text.universal-chat-translator.test.label"),
                () -> new CustomOpenAiTranslator.Settings(
                        baseUrl.getValue(), model.getValue(), apiKey.getValue(), systemPrompt.getValue(),
                        timeout.getValue(), temperature.getValue())));

        builder.setSavingRunnable(() -> AutoConfig.getConfigHolder(TranslatorConfig.class).save());
        return builder.build();
    }

    private static Optional<Text> validateBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(Text.translatable("text.universal-chat-translator.error.required"));
        }
        try {
            CustomOpenAiTranslator.normalizeEndpoint(value);
            return Optional.empty();
        } catch (TranslationException e) {
            return Optional.of(Text.literal(e.getMessage()));
        }
    }

    private static InputUtil.Key parseKeyboardKey(String translationKey) {
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(ShortcutBindings.normalize(translationKey));
            return key.getCategory() == InputUtil.Type.KEYSYM ? key : InputUtil.UNKNOWN_KEY;
        } catch (RuntimeException ignored) {
            return InputUtil.UNKNOWN_KEY;
        }
    }
}
