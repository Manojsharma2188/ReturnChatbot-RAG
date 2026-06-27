// JPA Entity Classes for Database Schema
// These entities map to the database tables for ORM integration

package com.chatbot.returnchatbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

// ==================== USER ENTITY ====================
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Conversation> conversations;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserMemory userMemory;

    // Constructors, getters, setters...
    public User() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public List<Conversation> getConversations() { return conversations; }
    public void setConversations(List<Conversation> conversations) { this.conversations = conversations; }
    public UserMemory getUserMemory() { return userMemory; }
    public void setUserMemory(UserMemory userMemory) { this.userMemory = userMemory; }
}

// ==================== CONVERSATION ENTITY ====================
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_updated_at", columnList = "updated_at")
})
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    private Boolean isArchived = false;

    @Column(nullable = false)
    private Integer totalMessages = 0;

    private Integer durationSeconds;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Message> messages;

    @OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ConversationContext conversationContext;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatAnalytics> analytics;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean archived) { isArchived = archived; }
    public Integer getTotalMessages() { return totalMessages; }
    public void setTotalMessages(Integer totalMessages) { this.totalMessages = totalMessages; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public ConversationContext getConversationContext() { return conversationContext; }
    public void setConversationContext(ConversationContext conversationContext) { this.conversationContext = conversationContext; }
    public List<ChatAnalytics> getAnalytics() { return analytics; }
    public void setAnalytics(List<ChatAnalytics> analytics) { this.analytics = analytics; }
}

// ==================== MESSAGE ENTITY ====================
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_sender_type", columnList = "sender_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    private Integer tokensUsed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isEdited = false;

    @Column(columnDefinition = "LONGTEXT")
    private String originalContent;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BotResponse> botResponses;

    public enum SenderType {
        USER, BOT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public SenderType getSenderType() { return senderType; }
    public void setSenderType(SenderType senderType) { this.senderType = senderType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean edited) { isEdited = edited; }
    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
    public List<BotResponse> getBotResponses() { return botResponses; }
    public void setBotResponses(List<BotResponse> botResponses) { this.botResponses = botResponses; }
}

// ==================== CONVERSATION_CONTEXT ENTITY ====================
@Entity
@Table(name = "conversation_context", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class ConversationContext {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contextId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String conversationSummary;

    @Column(columnDefinition = "JSON")
    private String keyTopics;

    @Column(columnDefinition = "JSON")
    private String userPreferences;

    @Column(columnDefinition = "LONGBLOB")
    private byte[] contextEmbeddings;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getContextId() { return contextId; }
    public void setContextId(Long contextId) { this.contextId = contextId; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getConversationSummary() { return conversationSummary; }
    public void setConversationSummary(String conversationSummary) { this.conversationSummary = conversationSummary; }
    public String getKeyTopics() { return keyTopics; }
    public void setKeyTopics(String keyTopics) { this.keyTopics = keyTopics; }
    public String getUserPreferences() { return userPreferences; }
    public void setUserPreferences(String userPreferences) { this.userPreferences = userPreferences; }
    public byte[] getContextEmbeddings() { return contextEmbeddings; }
    public void setContextEmbeddings(byte[] contextEmbeddings) { this.contextEmbeddings = contextEmbeddings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

// ==================== CHAT_ANALYTICS ENTITY ====================
@Entity
@Table(name = "chat_analytics", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ChatAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analyticsId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private Integer responseTimeMs;
    private String sentiment;
    private String modelUsed;
    private Float temperature;
    private Integer tokensInput;
    private Integer tokensOutput;
    private Integer totalTokens;
    private Integer sessionDurationSeconds;
    private Integer userSatisfaction;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "JSON")
    private String metadata;

    public enum EventType {
        MESSAGE_SENT, MESSAGE_RECEIVED, CONVERSATION_STARTED, CONVERSATION_ENDED, CONVERSATION_RESUMED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(Long analyticsId) { this.analyticsId = analyticsId; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public Float getTemperature() { return temperature; }
    public void setTemperature(Float temperature) { this.temperature = temperature; }
    public Integer getTokensInput() { return tokensInput; }
    public void setTokensInput(Integer tokensInput) { this.tokensInput = tokensInput; }
    public Integer getTokensOutput() { return tokensOutput; }
    public void setTokensOutput(Integer tokensOutput) { this.tokensOutput = tokensOutput; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Integer getSessionDurationSeconds() { return sessionDurationSeconds; }
    public void setSessionDurationSeconds(Integer sessionDurationSeconds) { this.sessionDurationSeconds = sessionDurationSeconds; }
    public Integer getUserSatisfaction() { return userSatisfaction; }
    public void setUserSatisfaction(Integer userSatisfaction) { this.userSatisfaction = userSatisfaction; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

// ==================== USER_MEMORY ENTITY ====================
@Entity
@Table(name = "user_memory")
public class UserMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memoryId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "JSON")
    private String memoryData;

    @Column(columnDefinition = "JSON")
    private String preferences;

    @Column(columnDefinition = "JSON")
    private String interactionHistory;

    @Column(columnDefinition = "JSON")
    private String learnedPatterns;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getMemoryId() { return memoryId; }
    public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMemoryData() { return memoryData; }
    public void setMemoryData(String memoryData) { this.memoryData = memoryData; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public String getInteractionHistory() { return interactionHistory; }
    public void setInteractionHistory(String interactionHistory) { this.interactionHistory = interactionHistory; }
    public String getLearnedPatterns() { return learnedPatterns; }
    public void setLearnedPatterns(String learnedPatterns) { this.learnedPatterns = learnedPatterns; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

// ==================== BOT_RESPONSE ENTITY ====================
@Entity
@Table(name = "bot_responses", indexes = {
    @Index(name = "idx_message_id", columnList = "message_id"),
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class BotResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long responseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String responseText;

    @Column(length = 50)
    private String modelVersion;

    private Float confidenceScore;
    private Boolean isRelevant;
    private Integer userFeedback;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Float getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Float confidenceScore) { this.confidenceScore = confidenceScore; }
    public Boolean getIsRelevant() { return isRelevant; }
    public void setIsRelevant(Boolean relevant) { isRelevant = relevant; }
    public Integer getUserFeedback() { return userFeedback; }
    public void setUserFeedback(Integer userFeedback) { this.userFeedback = userFeedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

