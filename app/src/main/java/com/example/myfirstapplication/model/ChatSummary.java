package com.example.myfirstapplication.model;

public class ChatSummary {
    private String name;
    private String lastMessage;
    private String time;
    private int avatarResId;

    public ChatSummary(String name, String lastMessage, String time, int avatarResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
    }

    // Getter & Setter ...
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getAvatarResId() { return avatarResId; }
}