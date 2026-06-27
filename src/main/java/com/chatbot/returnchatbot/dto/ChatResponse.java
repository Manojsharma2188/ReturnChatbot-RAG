package com.chatbot.returnchatbot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatResponse {
    private String response;
    private String role;
    private Long conversationId;

    public ChatResponse(String response) {
        this.response = response;
        this.role = "assistant";
        this.conversationId = null;
    }

    public ChatResponse(String role, String content) {
        this.role = role;
        this.response = content;
        this.conversationId = null;
    }

    public ChatResponse(String response, String role, Long conversationId) {
        this.response = response;
        this.role = role;
        this.conversationId = conversationId;
    }
}