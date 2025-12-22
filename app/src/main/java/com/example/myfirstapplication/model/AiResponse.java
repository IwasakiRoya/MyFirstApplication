package com.example.myfirstapplication.model;

import java.util.List;

public class AiResponse {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String content;
    }

    // 方便获取内容的辅助方法
    public String getAnswer() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).message.content;
        }
        return "AI 没有返回内容";
    }
}