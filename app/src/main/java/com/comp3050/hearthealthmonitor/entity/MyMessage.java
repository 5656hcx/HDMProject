package com.comp3050.hearthealthmonitor.entity;

public class MyMessage {

    private MessageType type;
    private String title;
    private String content;
    private String summary;
    private final long timestamp;

    public enum MessageType {

        GENERAL_TIPS(1), ADVISE(2), EMERGENCY(3);

        private int importance;

        MessageType(int importance) {
            this.importance = importance;
        }

        public int getImportance() {
            return importance;
        }
    }

    public MyMessage(MessageType type, String title, String content, String summary, long timestamp) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.timestamp = timestamp;
    }

    public static MessageType importanceToType(int importance) {
        for (MessageType t : MessageType.values()) {
            if (t.importance == importance)
                return t;
        }
        return null;
    }

    public MessageType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
