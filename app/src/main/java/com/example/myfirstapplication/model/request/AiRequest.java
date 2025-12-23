package com.example.myfirstapplication.model.request;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AiRequest {
    public String model = "deepseek-chat"; // 或其他模型名
    public List<Message> messages;

    public AiRequest(String systemPrompt, String userPrompt) {
        this.messages = new ArrayList<>();
        this.messages.add(new Message("system", systemPrompt));
        this.messages.add(new Message("user", userPrompt));
    }

    public static class Message {
        public String role;
        public String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
