// Service Implementation Examples for Common Database Operations

package com.chatbot.returnchatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chatbot.returnchatbot.entity.*;
import com.chatbot.returnchatbot.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationHistoryService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ConversationContextRepository contextRepository;
    @Autowired
    private ChatAnalyticsRepository analyticsRepository;
    @Autowired
    private UserMemoryRepository userMemoryRepository;
    @Autowired
    private BotResponseRepository botResponseRepository;

    // ==================== VIEW PREVIOUS CONVERSATIONS ====================

    /**
     * Get all active conversations for a user
     * Use Case: View previous conversations
     */
    @Transactional(readOnly = true)
    public Page<Conversation> getUserConversations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return conversationRepository.findActiveConversationsByUser(userId, pageable);
    }

    /**
     * Search conversations by keyword
     * Use Case: View previous conversations + Search
     */
    @Transactional(readOnly = true)
    public List<Conversation> searchConversations(Long userId, String keyword) {
        return conversationRepository.searchConversationsByKeyword(userId, keyword);
    }

    /**
     * Get conversations created in a date range
     * Use Case: View conversations by date
     */
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Conversation> conversations = conversationRepository.findByUserIdAndCreatedAtAfter(userId, startDate);
        return conversations.stream()
            .filter(c -> c.getCreatedAt().isBefore(endDate))
            .toList();
    }

    // ==================== CONTINUE OLD CHATS ====================

    /**
     * Resume a conversation by loading full context
     * Use Case: Continue old chats
     */
    @Transactional
    public ConversationResumeData resumeConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Update last accessed time
        conversation.setLastAccessedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Get last 10 messages for context
        List<Message> recentMessages = messageRepository.findLatestMessagesByConversation(conversationId, 10);

        // Get conversation context
        Optional<ConversationContext> context = contextRepository.findByConversationId(conversationId);

        // Get user memory
        User user = conversation.getUser();
        Optional<UserMemory> userMemory = userMemoryRepository.findByUserId(user.getUserId());

        return new ConversationResumeData(conversation, recentMessages, context, userMemory);
    }

    /**
     * Get full conversation history
     * Use Case: Continue old chats + View previous conversations
     */
    @Transactional(readOnly = true)
    public List<Message> getFullConversationHistory(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Create new conversation
     * Use Case: Start new chat / Continue pattern
     */
    @Transactional
    public Conversation createConversation(Long userId, String title) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title != null ? title : "Conversation - " + LocalDateTime.now());
        conversation.setTotalMessages(0);

        return conversationRepository.save(conversation);
    }

    /**
     * Add message to conversation
     * Use Case: Continue old chats + Build memory
     */
    @Transactional
    public Message addMessage(Long conversationId, Long userId, String content, Message.SenderType senderType) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setUser(user);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setTokensUsed(estimateTokens(content));

        Message savedMessage = messageRepository.save(message);

        // Update conversation message count
        conversation.setTotalMessages(conversation.getTotalMessages() + 1);
        conversationRepository.save(conversation);

        return savedMessage;
    }

    // ==================== SEARCH CHAT HISTORY ====================

    /**
     * Full-text search across all messages for a user
     * Use Case: Search chat history
     */
    @Transactional(readOnly = true)
    public Page<Message> searchChatHistory(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.searchMessagesByKeyword(userId, keyword, pageable);
    }

    /**
     * Search messages in a specific conversation
     * Use Case: Search within conversation
     */
    @Transactional(readOnly = true)
    public List<Message> searchConversationMessages(Long conversationId, String keyword) {
        List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return allMessages.stream()
            .filter(m -> m.getContent().toLowerCase().contains(keyword.toLowerCase()))
            .toList();
    }

    /**
     * Get messages by date range
     * Use Case: Search/filter messages
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesByDateRange(Long conversationId, LocalDateTime startDate, LocalDateTime endDate) {
        return messageRepository.findMessagesBetweenDates(conversationId, startDate, endDate);
    }

    // ==================== BUILD MEMORY LATER ====================

    /**
     * Update or create user memory after conversation
     * Use Case: Build memory later
     */
    @Transactional
    public UserMemory updateUserMemory(Long userId, String memoryData, String learnedPatterns) {
        Optional<UserMemory> existingMemory = userMemoryRepository.findByUserId(userId);

        UserMemory memory = existingMemory.orElseGet(() -> {
            UserMemory newMemory = new UserMemory();
            newMemory.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
            return newMemory;
        });

        memory.setMemoryData(memoryData);
        memory.setLearnedPatterns(learnedPatterns);

        return userMemoryRepository.save(memory);
    }

    /**
     * Generate and save conversation context/summary
     * Use Case: Build memory later
     */
    @Transactional
    public ConversationContext generateContextSummary(Long conversationId, String summary, String keyTopics, String preferences) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Optional<ConversationContext> existingContext = contextRepository.findByConversationId(conversationId);

        ConversationContext context = existingContext.orElseGet(() -> {
            ConversationContext newContext = new ConversationContext();
            newContext.setConversation(conversation);
            newContext.setUser(conversation.getUser());
            return newContext;
        });

        context.setConversationSummary(summary);
        context.setKeyTopics(keyTopics);
        context.setUserPreferences(preferences);

        return contextRepository.save(context);
    }

    // ==================== ENABLE ANALYTICS ====================

    /**
     * Log analytics event
     * Use Case: Enable analytics
     */
    @Transactional
    public ChatAnalytics logAnalyticsEvent(Long conversationId, Long userId, Long messageId,
                                          ChatAnalytics.EventType eventType, Integer responseTimeMs,
                                          Integer totalTokens, String sentiment) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ChatAnalytics analytics = new ChatAnalytics();
        analytics.setConversation(conversation);
        analytics.setUser(user);
        if (messageId != null) {
            analytics.setMessage(messageRepository.findById(messageId).orElse(null));
        }
        analytics.setEventType(eventType);
        analytics.setResponseTimeMs(responseTimeMs);
        analytics.setTotalTokens(totalTokens);
        analytics.setSentiment(sentiment);

        return analyticsRepository.save(analytics);
    }

    /**
     * Get analytics for a conversation
     * Use Case: Enable analytics
     */
    @Transactional(readOnly = true)
    public List<ChatAnalytics> getConversationAnalytics(Long conversationId) {
        return analyticsRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    /**
     * Get user analytics summary
     * Use Case: Enable analytics - Dashboard
     */
    @Transactional(readOnly = true)
    public AnalyticsSummary getUserAnalyticsSummary(Long userId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        Double avgResponseTime = analyticsRepository.getAverageResponseTimeByUser(userId, sevenDaysAgo);
        Double avgSatisfaction = analyticsRepository.getAverageUserSatisfactionByUser(userId, sevenDaysAgo);

        List<ChatAnalytics> recentAnalytics = analyticsRepository.findByUserIdAndCreatedAtAfter(userId, sevenDaysAgo);

        return new AnalyticsSummary(
            userId,
            avgResponseTime != null ? avgResponseTime : 0.0,
            avgSatisfaction != null ? avgSatisfaction : 0.0,
            recentAnalytics.size(),
            recentAnalytics
        );
    }

    /**
     * Record user satisfaction/feedback
     * Use Case: Enable analytics - User feedback
     */
    @Transactional
    public ChatAnalytics recordUserFeedback(Long analyticsId, Integer satisfactionRating) {
        ChatAnalytics analytics = analyticsRepository.findById(analyticsId)
            .orElseThrow(() -> new RuntimeException("Analytics record not found"));

        if (satisfactionRating >= 1 && satisfactionRating <= 5) {
            analytics.setUserSatisfaction(satisfactionRating);
            return analyticsRepository.save(analytics);
        }

        throw new IllegalArgumentException("Satisfaction rating must be between 1 and 5");
    }

    /**
     * Get conversation statistics
     * Use Case: Enable analytics
     */
    @Transactional(readOnly = true)
    public ConversationStats getConversationStats(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        long messageCount = messageRepository.countByConversationId(conversationId);
        Long totalTokens = analyticsRepository.getTotalTokensByConversation(conversationId);

        List<ChatAnalytics> analytics = analyticsRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
        Double avgResponseTime = analytics.stream()
            .mapToInt(a -> a.getResponseTimeMs() != null ? a.getResponseTimeMs() : 0)
            .average()
            .orElse(0.0);

        return new ConversationStats(
            conversationId,
            messageCount,
            totalTokens != null ? totalTokens : 0L,
            avgResponseTime,
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
    }

    // ==================== HELPER METHODS ====================

    private int estimateTokens(String content) {
        // Rough estimation: 1 token ≈ 4 characters
        return Math.max(1, content.length() / 4);
    }

    // ==================== DTO CLASSES ====================

    public static class ConversationResumeData {
        public Conversation conversation;
        public List<Message> recentMessages;
        public Optional<ConversationContext> context;
        public Optional<UserMemory> userMemory;

        public ConversationResumeData(Conversation conversation, List<Message> recentMessages,
                                     Optional<ConversationContext> context, Optional<UserMemory> userMemory) {
            this.conversation = conversation;
            this.recentMessages = recentMessages;
            this.context = context;
            this.userMemory = userMemory;
        }
    }

    public static class AnalyticsSummary {
        public Long userId;
        public Double avgResponseTimeMs;
        public Double avgSatisfactionRating;
        public Integer totalEvents;
        public List<ChatAnalytics> recentAnalytics;

        public AnalyticsSummary(Long userId, Double avgResponseTimeMs, Double avgSatisfactionRating,
                               Integer totalEvents, List<ChatAnalytics> recentAnalytics) {
            this.userId = userId;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.avgSatisfactionRating = avgSatisfactionRating;
            this.totalEvents = totalEvents;
            this.recentAnalytics = recentAnalytics;
        }
    }

    public static class ConversationStats {
        public Long conversationId;
        public Long messageCount;
        public Long totalTokens;
        public Double avgResponseTimeMs;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        public ConversationStats(Long conversationId, Long messageCount, Long totalTokens,
                                Double avgResponseTimeMs, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.conversationId = conversationId;
            this.messageCount = messageCount;
            this.totalTokens = totalTokens;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}

