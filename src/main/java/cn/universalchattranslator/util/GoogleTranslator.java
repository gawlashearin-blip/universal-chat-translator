package cn.universalchattranslator.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class GoogleTranslator {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private GoogleTranslator() {
    }

    public static CompletableFuture<String> translate(String text, String targetLanguage) {
        return CompletableFuture.supplyAsync(() -> {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                    + targetLanguage + "&dt=t&q=" + encodedText;
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new TranslationException("Google Translate returned HTTP " + response.code());
                }
                if (response.body() == null) {
                    throw new TranslationException("Google Translate returned an empty response");
                }
                JsonArray sentences = JsonParser.parseString(response.body().string())
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
            } catch (RuntimeException e) {
                throw new TranslationException("Google Translate returned an invalid response", e);
            }
        });
    }
}
