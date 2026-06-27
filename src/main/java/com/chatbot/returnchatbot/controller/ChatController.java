package com.chatbot.returnchatbot.controller;

import com.chatbot.returnchatbot.dto.ChatRequest;
import com.chatbot.returnchatbot.dto.ChatResponse;
import com.chatbot.returnchatbot.dto.ConversationSummary;
import com.chatbot.returnchatbot.dto.RagRequest;
import com.chatbot.returnchatbot.dto.RagResponse;
import com.chatbot.returnchatbot.service.ChatService;
import com.chatbot.returnchatbot.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final RagService ragService;

    public ChatController(ChatService chatService, RagService ragService) {
        this.chatService = chatService;
        this.ragService = ragService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request.getConversationId(), request.getPrompt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rag")
    public ResponseEntity<RagResponse> ragChat(@RequestBody RagRequest request) {
        log.info("RAG chat request received: {}", request.getMessage());
        String answer = ragService.ask(request.getMessage());

        // Persist the conversation and messages to PostgreSQL
        Long conversationId = chatService.saveRagConversation(request.getMessage(), answer);
        return ResponseEntity.ok(new RagResponse(answer, conversationId));
    }

    @PostMapping("/new")
    public ResponseEntity<ConversationSummary> createNewConversation() {
        return ResponseEntity.ok(chatService.createNewConversation());
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummary>> getConversations() {
        return ResponseEntity.ok(chatService.getAllConversations());
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatResponse>> getConversationMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessagesByConversationId(id));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        log.info("Delete conversation request: {}", id);
        chatService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
