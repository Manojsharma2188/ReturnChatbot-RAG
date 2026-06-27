package com.chatbot.returnchatbot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
public class RagResponse {
    private String answer;
    private Long conversationId;

    public RagResponse(String answer) {
        this.answer = answer;
        this.conversationId = null;
    }

    public RagResponse(String answer, Long conversationId) {
        this.answer = answer;
        this.conversationId = conversationId;
    }
}
