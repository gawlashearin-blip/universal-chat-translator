package cn.universalchattranslator.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class CustomOpenAiTranslator {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_ERROR_LENGTH = 240;

    private CustomOpenAiTranslator() {
    }

    public record Settings(
            String baseUrl,
            String model,
            String apiKey,
            String systemPrompt,
            int timeoutSeconds,
            double temperature
    ) {
    }

    public static CompletableFuture<String> translate(
            Settings settings, String text, String sourceLanguage, String targetLanguage) {
        return CompletableFuture.supplyAsync(() -> translateBlocking(settings, text, sourceLanguage, targetLanguage));
    }

    static String translateBlocking(
            Settings settings, String text, String sourceLanguage, String targetLanguage) {
        validateSettings(settings);
        String endpoint = normalizeEndpoint(settings.baseUrl());
        String requestJson = createRequestJson(settings, text, sourceLanguage, targetLanguage);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(settings.timeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(settings.timeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(settings.timeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(settings.timeoutSeconds(), TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(endpoint)
                .header("Accept", "application/json")
                .post(RequestBody.create(requestJson, JSON));
        if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + settings.apiKey().trim());
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (response.isRedirect()) {
                throw new TranslationException("The API returned a redirect; enter the final endpoint URL");
            }
            if (!response.isSuccessful()) {
                String detail = extractErrorMessage(responseBody, settings.apiKey());
                throw new TranslationException("API returned HTTP " + response.code()
                        + (detail.isEmpty() ? "" : ": " + detail));
            }
            return parseTranslation(responseBody);
        } catch (TranslationException e) {
            throw e;
        } catch (IOException e) {
            throw new TranslationException("API network error: " + safeMessage(e.getMessage(), settings.apiKey()), e);
        }
    }

    public static String normalizeEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new TranslationException("Base URL is required");
        }
        String trimmed = baseUrl.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new TranslationException("Base URL is invalid", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new TranslationException("Base URL must use HTTP or HTTPS");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null) {
            throw new TranslationException("Base URL must contain a valid host and no credentials, query, or fragment");
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.toLowerCase(Locale.ROOT).endsWith("/chat/completions")) {
            trimmed += "/chat/completions";
        }
        HttpUrl parsed = HttpUrl.parse(trimmed);
        if (parsed == null) {
            throw new TranslationException("Base URL is invalid");
        }
        return parsed.toString();
    }

    public static boolean isInsecureRemoteHttp(String baseUrl) {
        try {
            URI uri = new URI(baseUrl == null ? "" : baseUrl.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            return host != null && !host.equalsIgnoreCase("localhost")
                    && !host.equals("127.0.0.1") && !host.equals("::1") && !host.equals("[::1]");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    static String createRequestJson(
            Settings settings, String text, String sourceLanguage, String targetLanguage) {
        String prompt = settings.systemPrompt() == null || settings.systemPrompt().isBlank()
                ? cn.universalchattranslator.config.TranslatorConfig.DEFAULT_SYSTEM_PROMPT
                : settings.systemPrompt();
        prompt = prompt.replace("{source_language}", languageName(sourceLanguage))
                .replace("{target_language}", languageName(targetLanguage));

        JsonObject root = new JsonObject();
        root.addProperty("model", settings.model().trim());
        JsonArray messages = new JsonArray();
        messages.add(message("system", prompt));
        messages.add(message("user", text));
        root.add("messages", messages);
        root.addProperty("temperature", settings.temperature());
        root.addProperty("stream", false);
        return root.toString();
    }

    static String parseTranslation(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new TranslationException("API response contains no choices");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
                throw new TranslationException("API response contains no message content");
            }
            String content = message.get("content").getAsString().trim();
            if (content.isEmpty()) {
                throw new TranslationException("API returned empty translated text");
            }
            return content;
        } catch (TranslationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new TranslationException("API returned invalid JSON", e);
        }
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static void validateSettings(Settings settings) {
        if (settings == null) throw new TranslationException("Custom API settings are missing");
        normalizeEndpoint(settings.baseUrl());
        if (settings.model() == null || settings.model().isBlank()) {
            throw new TranslationException("Model is required");
        }
        if (settings.timeoutSeconds() < 1 || settings.timeoutSeconds() > 120) {
            throw new TranslationException("Timeout must be between 1 and 120 seconds");
        }
        if (!Double.isFinite(settings.temperature())
                || settings.temperature() < 0.0 || settings.temperature() > 2.0) {
            throw new TranslationException("Temperature must be between 0.0 and 2.0");
        }
    }

    private static String extractErrorMessage(String responseBody, String apiKey) {
        if (responseBody == null || responseBody.isBlank()) return "";
        String message = responseBody;
        try {
            JsonElement error = JsonParser.parseString(responseBody).getAsJsonObject().get("error");
            if (error != null && error.isJsonObject() && error.getAsJsonObject().has("message")) {
                message = error.getAsJsonObject().get("message").getAsString();
            } else if (error != null && error.isJsonPrimitive()) {
                message = error.getAsString();
            }
        } catch (RuntimeException ignored) {
            // A short, redacted plain-text response is still useful to the user.
        }
        return safeMessage(message, apiKey);
    }

    private static String safeMessage(String message, String apiKey) {
        if (message == null) return "unknown error";
        String safe = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (apiKey != null && !apiKey.isBlank()) {
            safe = safe.replace(apiKey, "***").replace(apiKey.trim(), "***");
        }
        return safe.length() <= MAX_ERROR_LENGTH ? safe : safe.substring(0, MAX_ERROR_LENGTH) + "…";
    }

    private static String languageName(String code) {
        if (code == null) return "an auto-detected language";
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "zh" -> "Simplified Chinese";
            case "en" -> "English";
            case "auto" -> "an auto-detected language";
            default -> code;
        };
    }
}
