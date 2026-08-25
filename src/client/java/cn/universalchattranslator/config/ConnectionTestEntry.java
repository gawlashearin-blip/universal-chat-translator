package cn.universalchattranslator.config;

import cn.universalchattranslator.util.CustomOpenAiTranslator;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

final class ConnectionTestEntry extends TooltipListEntry<Boolean> {
    private final ButtonWidget button;
    private final Supplier<CustomOpenAiTranslator.Settings> settingsSupplier;
    private Text status = Text.empty();
    private boolean testing;

    ConnectionTestEntry(Text fieldName, Supplier<CustomOpenAiTranslator.Settings> settingsSupplier) {
        super(fieldName, () -> Optional.empty(), false);
        this.settingsSupplier = settingsSupplier;
        this.button = ButtonWidget.builder(Text.translatable("text.universal-chat-translator.test.button"), ignored -> test())
                .dimensions(0, 0, 150, 20)
                .build();
    }

    private void test() {
        if (testing) return;
        testing = true;
        button.active = false;
        button.setMessage(Text.translatable("text.universal-chat-translator.test.testing"));
        status = Text.translatable("text.universal-chat-translator.test.testing").formatted(Formatting.YELLOW);
        long started = System.nanoTime();
        CustomOpenAiTranslator.Settings settings = settingsSupplier.get();
        CustomOpenAiTranslator.translate(settings, "Hello, welcome to the server!", "en", "zh")
                .whenComplete((translated, error) -> MinecraftClient.getInstance().execute(() -> {
                    testing = false;
                    button.active = true;
                    button.setMessage(Text.translatable("text.universal-chat-translator.test.button"));
                    if (error == null) {
                        long millis = (System.nanoTime() - started) / 1_000_000;
                        String compact = translated.replaceAll("[\\r\\n]+", " ");
                        if (compact.length() > 80) compact = compact.substring(0, 80) + "…";
                        status = Text.literal("✓ " + millis + " ms — " + compact).formatted(Formatting.GREEN);
                    } else {
                        Throwable cause = error;
                        while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
                        String message = cause.getMessage() == null ? "未知错误" : cause.getMessage();
                        if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
                            message = message.replace(settings.apiKey(), "***");
                        }
                        status = Text.literal("✕ " + message).formatted(Formatting.RED);
                    }
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && client.inGameHud != null) {
                        client.inGameHud.getChatHud().addMessage(status.copy());
                    }
                }));
    }

    @Override
    public Boolean getValue() {
        return false;
    }

    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public int getItemHeight() {
        return 40;
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTextWithShadow(client.textRenderer, getDisplayedFieldName(), x, y + 6, getPreferredTextColor());
        button.setX(x + entryWidth - 150);
        button.setY(y);
        button.render(context, mouseX, mouseY, delta);
        if (!status.getString().isEmpty()) {
            context.drawTextWithShadow(client.textRenderer, status, x, y + 25, 0xFFFFFF);
        }
    }

    @Override
    public List<? extends Element> children() {
        return List.of(button);
    }

    @Override
    public List<? extends Selectable> narratables() {
        return List.of(button);
    }
}
