package cn.universalchattranslator;

import cn.universalchattranslator.config.TranslationEngine;
import cn.universalchattranslator.config.TranslatorConfig;
import cn.universalchattranslator.message.ClassifiedMessage;
import cn.universalchattranslator.message.MessageClassifier;
import cn.universalchattranslator.message.MessageKind;
import cn.universalchattranslator.message.PendingOwnMessageTracker;
import cn.universalchattranslator.message.TranslationReleasePolicy;
import cn.universalchattranslator.shortcut.ShortcutManager;
import cn.universalchattranslator.util.CustomOpenAiTranslator;
import cn.universalchattranslator.util.GoogleTranslator;
import cn.universalchattranslator.util.OrderedAsyncBuffer;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UniversalChatTranslatorClient implements ClientModInitializer {
    private static final Duration GOOGLE_TIMEOUT = Duration.ofSeconds(30);

    private final OrderedAsyncBuffer<TranslationRequest> incomingOrder = new OrderedAsyncBuffer<>();
    private final OrderedAsyncBuffer<OutgoingResult> outgoingOrder = new OrderedAsyncBuffer<>();
    private final PendingOwnMessageTracker ownMessageTracker = new PendingOwnMessageTracker();
    private final ConcurrentLinkedQueue<Runnable> receivedMessageQueue = new ConcurrentLinkedQueue<>();
    private final ShortcutManager shortcutManager = new ShortcutManager();
    private boolean sendingTranslatedMessage;

    @Override
    public void onInitializeClient() {
        TranslatorConfig.init();
        setupMessageListeners();
        UniversalChatTranslator.LOGGER.info("Universal Chat Translator loaded");
    }

    private void setupMessageListeners() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            receivedMessageQueue.add(() -> onPlayerChatDisplayed(message, signedMessage, sender, params, timestamp));
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            receivedMessageQueue.add(() -> onGameMessageDisplayed(message, overlay));
            return true;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Runnable pending;
            while ((pending = receivedMessageQueue.poll()) != null) pending.run();
            shortcutManager.tick(client);
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (sendingTranslatedMessage) return true;
            TranslatorConfig config = TranslatorConfig.get();
            if (!config.isEnabled() || !config.isChineseToEnglish()) return true;
            queueOutgoingMessage(message);
            return false;
        });
    }

    private void onPlayerChatDisplayed(Text displayed, SignedMessage signedMessage, GameProfile sender,
                                       MessageType.Parameters params, Instant timestamp) {
        TranslatorConfig config = TranslatorConfig.get();
        if (!config.isEnabled() || !config.isTranslatePlayerMessages()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (sender != null) {
            boolean own = client.player != null && client.player.getUuid().equals(sender.id());
            if (own) return;

            String body = signedMessage == null ? displayed.getString() : signedMessage.getSignedContent();
            queueIncoming(MessageClassifier.playerMessage(body, sender.name(), false));
            return;
        }

        // Fabric explicitly permits a null profile for CHAT events. The channel still represents
        // a player chat message, so try to recover its speaker/body instead of silently dropping it.
        String ownName = client.player == null ? null : client.player.getGameProfile().name();
        ClassifiedMessage recovered = MessageClassifier.systemMessage(
                displayed.getString(), onlinePlayerNames(client), ownName);
        if (recovered.kind() == MessageKind.PLAYER) {
            if (!recovered.ownMessage()) queueIncoming(recovered);
        } else {
            queueIncoming(MessageClassifier.playerMessage(displayed.getString(), "玩家", false));
        }
    }

    private void onGameMessageDisplayed(Text message, boolean overlay) {
        if (overlay) return;
        TranslatorConfig config = TranslatorConfig.get();
        if (!config.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        String ownName = client.player == null ? null : client.player.getGameProfile().name();
        ClassifiedMessage classified = MessageClassifier.systemMessage(
                message.getString(), onlinePlayerNames(client), ownName);

        if (classified.kind() == MessageKind.PLAYER) {
            if (classified.ownMessage()) {
                ownMessageTracker.consumeIfTracked(classified.body());
                return;
            }
        }
        if (!classified.enabledBy(
                config.isTranslatePlayerMessages(), config.isTranslateSystemMessages())) return;
        queueIncoming(classified);
    }

    private Collection<String> onlinePlayerNames(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return List.of();
        return client.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().name())
                .toList();
    }

    private void queueIncoming(ClassifiedMessage classified) {
        String original = classified.body();
        if (original.isBlank() || containsChinese(original)) return;

        TranslationRequest request = new TranslationRequest(classified, original);
        CompletableFuture<TranslationRequest> future = translateAsync(original, "auto", "zh")
                .thenApply(request::withTranslation);
        incomingOrder.submit(future, requestTimeout(), outcome ->
                MinecraftClient.getInstance().execute(() -> releaseIncoming(request, outcome)));
    }

    private void releaseIncoming(TranslationRequest request,
                                 OrderedAsyncBuffer.Outcome<TranslationRequest> outcome) {
        TranslatorConfig config = TranslatorConfig.get();
        if (!TranslationReleasePolicy.displayIncoming(request.message(), config.isEnabled(),
                config.isTranslatePlayerMessages(), config.isTranslateSystemMessages())) return;
        if (!outcome.succeeded()) {
            showError(outcome.error());
            return;
        }
        String translated = outcome.value().translated();
        if (isTranslationValid(request.original(), translated)) {
            addChatMessage(labeledMessage(request.message().senderName(), translated));
        }
    }

    private void queueOutgoingMessage(String original) {
        CompletableFuture<OutgoingResult> future = containsChinese(original)
                ? translateAsync(original, "zh", "en")
                    .thenApply(value -> new OutgoingResult(original, value, true))
                : CompletableFuture.completedFuture(new OutgoingResult(original, original, false));

        outgoingOrder.submit(future, requestTimeout(), outcome ->
                MinecraftClient.getInstance().execute(() -> releaseOutgoing(original, outcome)));
    }

    private void releaseOutgoing(String original, OrderedAsyncBuffer.Outcome<OutgoingResult> outcome) {
        if (!outcome.succeeded()) {
            showError(outcome.error());
            trackAndSend(original);
            return;
        }

        OutgoingResult result = outcome.value();
        TranslatorConfig config = TranslatorConfig.get();
        if (!TranslationReleasePolicy.useTranslatedOutgoing(
                result.translated(), config.isEnabled(), config.isChineseToEnglish())) {
            trackAndSend(result.original());
            return;
        }
        if (!result.translated()) {
            trackAndSend(result.output());
            return;
        }
        if (!isTranslationValid(result.original(), result.output())) {
            showError(new IllegalStateException("翻译接口返回了无效译文"));
            trackAndSend(result.original());
            return;
        }

        addChatMessage(labeledMessage(currentPlayerName(), result.original()));
        trackAndSend(result.output());
    }

    private void trackAndSend(String message) {
        ownMessageTracker.track(message);
        sendChatMessage(message);
    }

    private CompletableFuture<String> translateAsync(String text, String source, String target) {
        TranslatorConfig config = TranslatorConfig.get();
        if (config.getTranslationEngine() == TranslationEngine.GOOGLE_FREE) {
            return GoogleTranslator.translate(text, target);
        }
        return CustomOpenAiTranslator.translate(settingsFrom(config), text, source, target);
    }

    private Duration requestTimeout() {
        TranslatorConfig config = TranslatorConfig.get();
        if (config.getTranslationEngine() == TranslationEngine.CUSTOM_OPENAI) {
            return Duration.ofSeconds(Math.max(1, Math.min(120, config.getCustomTimeoutSeconds())) + 1L);
        }
        return GOOGLE_TIMEOUT;
    }

    public static CustomOpenAiTranslator.Settings settingsFrom(TranslatorConfig config) {
        return new CustomOpenAiTranslator.Settings(
                config.getCustomBaseUrl(), config.getCustomModel(), config.getCustomApiKey(),
                config.getCustomSystemPrompt(), config.getCustomTimeoutSeconds(), config.getCustomTemperature());
    }

    private void sendChatMessage(String message) {
        sendingTranslatedMessage = true;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) client.getNetworkHandler().sendChatMessage(message);
        } finally {
            sendingTranslatedMessage = false;
        }
    }

    private String currentPlayerName() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? "自己" : client.player.getGameProfile().name();
    }

    private Text labeledMessage(String label, String body) {
        TranslatorConfig config = TranslatorConfig.get();
        MutableText prefix = Text.literal("[" + label + "] ").formatted(Formatting.GOLD);
        return prefix.append(Text.literal(body).formatted(config.getTranslationColor().formatting()));
    }

    private void showError(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current.getClass() == RuntimeException.class)
                && current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        if (message == null || message.isBlank()) message = "未知错误";
        UniversalChatTranslator.LOGGER.warn("Translation request failed: {}", message);
        addChatMessage(Text.literal("[翻译失败] " + message).formatted(Formatting.RED));
    }

    private void addChatMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) client.inGameHud.getChatHud().addMessage(message);
    }

    private boolean containsChinese(String value) {
        for (char character : value.toCharArray()) {
            if (Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN) return true;
        }
        return false;
    }

    private boolean isTranslationValid(String original, String translated) {
        return translated != null && !translated.isBlank() && !translated.equals(original)
                && !translated.matches("[\\p{Punct}\\d\\s]+");
    }

    private record TranslationRequest(ClassifiedMessage message, String original, String translated) {
        TranslationRequest(ClassifiedMessage message, String original) {
            this(message, original, null);
        }

        TranslationRequest withTranslation(String value) {
            return new TranslationRequest(message, original, value);
        }
    }

    private record OutgoingResult(String original, String output, boolean translated) {
    }
}
