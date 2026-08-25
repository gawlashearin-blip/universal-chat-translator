package cn.universalchattranslator.message;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageClassifier {
    private MessageClassifier() {
    }

    public static ClassifiedMessage playerMessage(String body, String senderName, boolean ownMessage) {
        String safeBody = body == null ? "" : body.trim();
        String safeName = senderName == null || senderName.isBlank() ? "玩家" : senderName;
        return new ClassifiedMessage(MessageKind.PLAYER, safeBody, safeName, ownMessage);
    }

    public static ClassifiedMessage systemMessage(
            String formattedMessage, Collection<String> onlinePlayerNames, String ownPlayerName) {
        String message = formattedMessage == null ? "" : formattedMessage.trim();
        ClassifiedMessage onlinePlayer = onlinePlayerNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(name -> matchPluginPlayerMessage(message, name, ownPlayerName))
                .filter(result -> result != null)
                .findFirst()
                .orElse(null);
        if (onlinePlayer != null) return onlinePlayer;

        ClassifiedMessage strongFormat = matchStrongPluginPlayerMessage(message, ownPlayerName);
        return strongFormat != null ? strongFormat
                : new ClassifiedMessage(MessageKind.SYSTEM, message, "系统", false);
    }

    private static ClassifiedMessage matchPluginPlayerMessage(String message, String playerName, String ownName) {
        String quoted = Pattern.quote(playerName);
        // Server chat often has level numbers, icons and rank components before the name.
        // Only search before the first chat separator and require an exact online profile name.
        Pattern decoratedSpeaker = Pattern.compile(
                "^\\s*[^:：\\r\\n]{0,64}?(?<![a-zA-Z0-9_])(" + quoted
                        + ")(?![a-zA-Z0-9_])\\s*(?::|：|>+|»|›)\\s*(.+)$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = decoratedSpeaker.matcher(message);
        if (!matcher.matches()) return null;

        boolean own = ownName != null && playerName.equalsIgnoreCase(ownName);
        return new ClassifiedMessage(MessageKind.PLAYER, matcher.group(2).trim(), playerName, own);
    }

    private static ClassifiedMessage matchStrongPluginPlayerMessage(String message, String ownName) {
        Pattern angle = Pattern.compile(
                "^\\s*(?:\\[[^]]+]\\s*)*<\\s*([a-zA-Z0-9_]{1,32})\\s*>\\s*(.+)$");
        Pattern ranked = Pattern.compile(
                "^\\s*(?:\\[[^]]+]\\s*)+([a-zA-Z0-9_]{1,32})\\s*(?::|：|>+|»|›)\\s*(.+)$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = angle.matcher(message);
        if (!matcher.matches()) matcher = ranked.matcher(message);
        if (!matcher.matches()) return null;

        String name = matcher.group(1);
        boolean own = ownName != null && name.equalsIgnoreCase(ownName);
        return new ClassifiedMessage(MessageKind.PLAYER, matcher.group(2).trim(), name, own);
    }

    public static String normalizeForComparison(String value) {
        if (value == null) return "";
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
