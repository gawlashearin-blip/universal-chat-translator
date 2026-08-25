package cn.universalchattranslator.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CustomOpenAiTranslatorTest {
    private static final String PROMPT = "Translate {source_language} to {target_language}; output only translation.";

    @Test
    void normalizesBaseAndCompleteEndpointUrls() {
        assertEquals("https://example.com/v1/chat/completions",
                CustomOpenAiTranslator.normalizeEndpoint("https://example.com/v1"));
        assertEquals("https://example.com/v1/chat/completions",
                CustomOpenAiTranslator.normalizeEndpoint("https://example.com/v1/"));
        assertEquals("http://localhost:11434/v1/chat/completions",
                CustomOpenAiTranslator.normalizeEndpoint("http://localhost:11434/v1/chat/completions"));
    }

    @Test
    void rejectsInvalidUrls() {
        assertThrows(TranslationException.class, () -> CustomOpenAiTranslator.normalizeEndpoint(""));
        assertThrows(TranslationException.class, () -> CustomOpenAiTranslator.normalizeEndpoint("file:///tmp/api"));
        assertThrows(TranslationException.class, () -> CustomOpenAiTranslator.normalizeEndpoint("https://key@example.com/v1"));
        assertThrows(TranslationException.class, () -> CustomOpenAiTranslator.normalizeEndpoint("https://example.com/v1?key=value"));
        assertThrows(TranslationException.class, () -> CustomOpenAiTranslator.normalizeEndpoint("not a url"));
    }

    @Test
    void detectsOnlyRemotePlainHttp() {
        assertTrue(CustomOpenAiTranslator.isInsecureRemoteHttp("http://example.com/v1"));
        assertFalse(CustomOpenAiTranslator.isInsecureRemoteHttp("https://example.com/v1"));
        assertFalse(CustomOpenAiTranslator.isInsecureRemoteHttp("http://localhost:11434/v1"));
        assertFalse(CustomOpenAiTranslator.isInsecureRemoteHttp("http://127.0.0.1:1234/v1"));
        assertFalse(CustomOpenAiTranslator.isInsecureRemoteHttp("http://[::1]:1234/v1"));
    }

    @Test
    void sendsStandardChatCompletionsRequestAndParsesResponse() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(success("你好，欢迎来到服务器！"));
            var settings = settings(server, "secret-key", 2);

            String result = CustomOpenAiTranslator.translate(
                    settings, "Hello, welcome to the server!", "en", "zh").get(3, TimeUnit.SECONDS);

            assertEquals("你好，欢迎来到服务器！", result);
            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("POST", request.getMethod());
            assertEquals("/v1/chat/completions", request.getPath());
            assertEquals("Bearer secret-key", request.getHeader("Authorization"));
            JsonObject json = JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
            assertEquals("test-model", json.get("model").getAsString());
            assertEquals(0.25, json.get("temperature").getAsDouble());
            assertFalse(json.get("stream").getAsBoolean());
            assertEquals("Translate English to Simplified Chinese; output only translation.",
                    json.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
            assertEquals("Hello, welcome to the server!",
                    json.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
        }
    }

    @Test
    void omitsAuthorizationWhenApiKeyIsBlank() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(success("ok"));
            CustomOpenAiTranslator.translate(settings(server, "", 2), "hello", "auto", "zh")
                    .get(3, TimeUnit.SECONDS);
            assertNull(server.takeRequest(1, TimeUnit.SECONDS).getHeader("Authorization"));
        }
    }

    @Test
    void rejectsRedirectWithoutFollowingIt() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location", "/other"));
            ExecutionException error = assertThrows(ExecutionException.class, () ->
                    CustomOpenAiTranslator.translate(settings(server, "key", 2), "hello", "en", "zh").get());
            assertTrue(error.getCause().getMessage().contains("redirect"));
            assertEquals(1, server.getRequestCount());
        }
    }

    @Test
    void reportsProtocolFailures() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("not-json").setHeader("Content-Type", "application/json"));
            assertFailureContains(server, "invalid JSON");

            server.enqueue(new MockResponse().setBody("{\"choices\":[]}"));
            assertFailureContains(server, "no choices");

            server.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{}}]}"));
            assertFailureContains(server, "no message content");

            server.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"content\":\"  \"}}]}"));
            assertFailureContains(server, "empty translated text");
        }
    }

    @Test
    void redactsApiKeyFromHttpErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            String key = "very-secret-key";
            server.enqueue(new MockResponse().setResponseCode(401)
                    .setBody("{\"error\":{\"message\":\"invalid " + key + "\"}}"));
            ExecutionException error = assertThrows(ExecutionException.class, () ->
                    CustomOpenAiTranslator.translate(settings(server, key, 2), "hello", "en", "zh").get());
            assertTrue(error.getCause().getMessage().contains("HTTP 401"));
            assertFalse(error.getCause().getMessage().contains(key));
            assertTrue(error.getCause().getMessage().contains("***"));
        }
    }

    @Test
    void reportsRateLimitAndServerErrors() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(429)
                    .setBody("{\"error\":{\"message\":\"rate limited\"}}"));
            assertFailureContains(server, "HTTP 429: rate limited");
            server.enqueue(new MockResponse().setResponseCode(500)
                    .setBody("{\"error\":{\"message\":\"provider unavailable\"}}"));
            assertFailureContains(server, "HTTP 500: provider unavailable");
        }
    }

    @Test
    void timesOutUnresponsiveServer() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            ExecutionException error = assertThrows(ExecutionException.class, () ->
                    CustomOpenAiTranslator.translate(settings(server, "", 1), "hello", "en", "zh").get());
            assertTrue(error.getCause().getMessage().contains("network error"));
        }
    }

    private static CustomOpenAiTranslator.Settings settings(MockWebServer server, String key, int timeout) {
        return new CustomOpenAiTranslator.Settings(
                server.url("/v1").toString(), "test-model", key, PROMPT, timeout, 0.25);
    }

    private static MockResponse success(String text) {
        return new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":"
                        + JsonParser.parseString("\"" + text + "\"") + "}}]}");
    }

    private static void assertFailureContains(MockWebServer server, String expected) {
        ExecutionException error = assertThrows(ExecutionException.class, () ->
                CustomOpenAiTranslator.translate(settings(server, "", 2), "hello", "en", "zh").get());
        assertTrue(error.getCause().getMessage().contains(expected), error.getCause().getMessage());
    }
}
