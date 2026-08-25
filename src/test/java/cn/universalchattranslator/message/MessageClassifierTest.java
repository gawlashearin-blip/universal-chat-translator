package cn.universalchattranslator.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageClassifierTest {
    private static final List<String> ONLINE = List.of("Steve", "Alex", "LongPlayerName", "yeonghokim05");

    @Test
    void recognizesSupportedPluginSpeakerFormats() {
        assertPlayer("<Steve> hello", "Steve", "hello");
        assertPlayer("[VIP] Steve: hello", "Steve", "hello");
        assertPlayer("Steve > hello", "Steve", "hello");
        assertPlayer("Steve：hello", "Steve", "hello");
        assertPlayer("[Admin] LongPlayerName » hello", "LongPlayerName", "hello");
        assertPlayer("1⚒ yeonghokim05: MONa", "yeonghokim05", "MONa");
        assertPlayer("10 ⚔ [VIP] yeonghokim05: where are you", "yeonghokim05", "where are you");
    }

    @Test
    void recognizesStrongPluginFormatWhenServerUsesANickname() {
        ClassifiedMessage angle = MessageClassifier.systemMessage("<Nickname> hello", ONLINE, "Alex");
        assertEquals(MessageKind.PLAYER, angle.kind());
        assertEquals("Nickname", angle.senderName());
        assertEquals("hello", angle.body());

        ClassifiedMessage ranked = MessageClassifier.systemMessage("[VIP] Nickname: hello", ONLINE, "Alex");
        assertEquals(MessageKind.PLAYER, ranked.kind());
        assertEquals("Nickname", ranked.senderName());
    }

    @Test
    void onlyTreatsOnlineNameInSpeakerPositionAsPlayerMessage() {
        ClassifiedMessage announcement = MessageClassifier.systemMessage(
                "Server announcement: Steve won a prize", ONLINE, "Alex");
        assertEquals(MessageKind.SYSTEM, announcement.kind());
        assertEquals("系统", announcement.senderName());

        ClassifiedMessage offline = MessageClassifier.systemMessage("Herobrine: hello", ONLINE, "Alex");
        assertEquals(MessageKind.SYSTEM, offline.kind());
    }

    @Test
    void identifiesOwnPluginMessage() {
        ClassifiedMessage result = MessageClassifier.systemMessage("[VIP] Alex: hello", ONLINE, "Alex");
        assertTrue(result.ownMessage());
    }

    @Test
    void honorsAllFourPlayerAndSystemToggleCombinations() {
        ClassifiedMessage player = MessageClassifier.playerMessage("hello", "Steve", false);
        ClassifiedMessage system = MessageClassifier.systemMessage("Server restarting", ONLINE, "Alex");

        assertFalse(player.enabledBy(false, false));
        assertFalse(system.enabledBy(false, false));
        assertTrue(player.enabledBy(true, false));
        assertFalse(system.enabledBy(true, false));
        assertFalse(player.enabledBy(false, true));
        assertTrue(system.enabledBy(false, true));
        assertTrue(player.enabledBy(true, true));
        assertTrue(system.enabledBy(true, true));
    }

    private static void assertPlayer(String input, String name, String body) {
        ClassifiedMessage result = MessageClassifier.systemMessage(input, ONLINE, "Alex");
        assertEquals(MessageKind.PLAYER, result.kind());
        assertEquals(name, result.senderName());
        assertEquals(body, result.body());
    }
}
