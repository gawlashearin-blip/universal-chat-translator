package cn.universalchattranslator.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class GoogleTranslator {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private GoogleTranslator() {
    }

    public static CompletableFuture<String> translate(String text, String targetLanguage) {
        return CompletableFuture.supplyAsync(() -> {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                    + targetLanguage + "&dt=t&q=" + encodedText;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = CLIENT.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new TranslationException("Google Translate returned HTTP " + response.statusCode());
                }
                if (response.body() == null || response.body().isBlank()) {
                    throw new TranslationException("Google Translate returned an empty response");
                }
                JsonArray sentences = JsonParser.parseString(response.body())
                        .getAsJsonArray().get(0).getAsJsonArray();
                StringBuilder translated = new StringBuilder();
                for (int i = 0; i < sentences.size(); i++) {
                    translated.append(sentences.get(i).getAsJsonArray().get(0).getAsString());
                }
                if (translated.isEmpty()) {
                    throw new TranslationException("Google Translate returned no translated text");
                }
                return translated.toString();
            } catch (TranslationException e) {
                throw e;
            } catch (IOException e) {
                throw new TranslationException("Google Translate network error", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TranslationException("Google Translate request was interrupted", e);
            } catch (RuntimeException e) {
                throw new TranslationException("Google Translate returned an invalid response", e);
            }
        });
    }
}
