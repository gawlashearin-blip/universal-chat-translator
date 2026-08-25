package cn.universalchattranslator.message;

public record ClassifiedMessage(MessageKind kind, String body, String senderName, boolean ownMessage) {
    public boolean enabledBy(boolean translatePlayerMessages, boolean translateSystemMessages) {
        return kind == MessageKind.PLAYER ? translatePlayerMessages : translateSystemMessages;
    }
}
