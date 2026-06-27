package com.chatbot.returnchatbot.service;

import com.chatbot.returnchatbot.dto.ChatResponse;
import com.chatbot.returnchatbot.dto.ConversationSummary;
import com.chatbot.returnchatbot.entity.Conversation;
import com.chatbot.returnchatbot.entity.Message;
import com.chatbot.returnchatbot.repository.ConversationRepository;
import com.chatbot.returnchatbot.repository.MessageRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final WebClient webClient;
    private final Gson gson = new Gson();
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatService(WebClient.Builder webClientBuilder,
                       @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
                       ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.webClient = webClientBuilder.baseUrl(ollamaBaseUrl).build();
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ConversationSummary createNewConversation() {
        Conversation conv = new Conversation("Chat with AI - " + LocalDateTime.now());
        conv = conversationRepository.save(conv);
        return new ConversationSummary(conv.getId(), conv.getTitle());
    }

    @Transactional
    public ChatResponse chat(Long conversationId, String prompt) {
        Conversation conversation;
        if (conversationId == null) {
            // Use the first user prompt as the conversation title (truncate if too long)
            String title = prompt.length() > 100 ? prompt.substring(0, 97) + "..." : prompt;
            conversation = new Conversation(title);
            conversation = conversationRepository.save(conversation);
        } else {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        }

        // Save the user's message
        Message userMessage = new Message(conversation, "user", prompt);
        messageRepository.save(userMessage);

        // Call Ollama
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama3.2");
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);

        Mono<String> response = webClient.post()
                .uri("/api/generate")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class);

        String result = response.block();

        String botResponse = "No response received from Ollama";
        if (result != null) {
            JsonObject jsonResponse = gson.fromJson(result, JsonObject.class);
            botResponse = jsonResponse.get("response").getAsString();
        }

        // Save the bot's response
        Message botMessage = new Message(conversation, "assistant", botResponse);
        messageRepository.save(botMessage);

        return new ChatResponse(botResponse, "assistant", conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getAllConversations() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(c -> new ConversationSummary(c.getId(), c.getTitle()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> new ChatResponse(m.getRole(), m.getContent()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long saveRagConversation(String userMessage, String botResponse) {
        // Use the first user message as the conversation title
        String title = userMessage.length() > 100 ? userMessage.substring(0, 97) + "..." : userMessage;
        Conversation conversation = new Conversation(title);
        conversation = conversationRepository.save(conversation);

        // Save user message
        Message userMsg = new Message(conversation, "user", userMessage);
        messageRepository.save(userMsg);

        // Save assistant response
        Message botMsg = new Message(conversation, "assistant", botResponse);
        messageRepository.save(botMsg);

        log.info("Saved RAG conversation {} with message and response", conversation.getId());
        return conversation.getId();
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        // Delete all messages first, then the conversation
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
        log.info("Deleted conversation {} and its messages", conversationId);
    }
}