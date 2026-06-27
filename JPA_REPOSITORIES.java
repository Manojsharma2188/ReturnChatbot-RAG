// Spring Data JPA Repository Interfaces for Database Operations

package com.chatbot.returnchatbot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.chatbot.returnchatbot.entity.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ==================== USER REPOSITORY ====================
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByIsActive(Boolean isActive);
}

// ==================== CONVERSATION REPOSITORY ====================
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Conversation> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(Long userId, Boolean isArchived);

    @Query("SELECT c FROM Conversation c WHERE c.user.userId = :userId AND c.isArchived = false ORDER BY c.updatedAt DESC")
    Page<Conversation> findActiveConversationsByUser(@Param("userId") Long userId, Pageable pageable);

    List<Conversation> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);

    @Query("SELECT c FROM Conversation c WHERE c.user.userId = :userId AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Conversation> searchConversationsByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}

// ==================== MESSAGE REPOSITORY ====================
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findLatestMessagesByConversation(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    List<Message> findByConversationIdAndSenderType(Long conversationId, Message.SenderType senderType);

    long countByConversationId(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.user.userId = :userId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    Page<Message> searchMessagesByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.createdAt >= :startDate AND m.createdAt <= :endDate ORDER BY m.createdAt ASC")
    List<Message> findMessagesBetweenDates(@Param("conversationId") Long conversationId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

// ==================== CONVERSATION_CONTEXT REPOSITORY ====================
@Repository
public interface ConversationContextRepository extends JpaRepository<ConversationContext, Long> {
    Optional<ConversationContext> findByConversationId(Long conversationId);
    Optional<ConversationContext> findByUserId(Long userId);
}

// ==================== CHAT_ANALYTICS REPOSITORY ====================
@Repository
public interface ChatAnalyticsRepository extends JpaRepository<ChatAnalytics, Long> {
    List<ChatAnalytics> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<ChatAnalytics> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);
    List<ChatAnalytics> findByEventType(ChatAnalytics.EventType eventType);

    @Query("SELECT ca FROM ChatAnalytics ca WHERE ca.user.userId = :userId AND ca.createdAt >= :startDate ORDER BY ca.createdAt DESC")
    Page<ChatAnalytics> findAnalyticsByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT AVG(ca.responseTimeMs) FROM ChatAnalytics ca WHERE ca.user.userId = :userId AND ca.createdAt >= :startDate")
    Double getAverageResponseTimeByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(ca.userSatisfaction) FROM ChatAnalytics ca WHERE ca.user.userId = :userId AND ca.userSatisfaction IS NOT NULL AND ca.createdAt >= :startDate")
    Double getAverageUserSatisfactionByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(ca.totalTokens) FROM ChatAnalytics ca WHERE ca.conversation.conversationId = :conversationId")
    Long getTotalTokensByConversation(@Param("conversationId") Long conversationId);
}

// ==================== SEARCH_INDEX REPOSITORY ====================
@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Long> {
    @Query("SELECT si FROM SearchIndex si WHERE si.user.userId = :userId AND LOWER(si.searchKeywords) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY si.relevanceScore DESC")
    Page<SearchIndex> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    List<SearchIndex> findByConversationId(Long conversationId);
}

// ==================== USER_MEMORY REPOSITORY ====================
@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    Optional<UserMemory> findByUserId(Long userId);
}

// ==================== BOT_RESPONSE REPOSITORY ====================
@Repository
public interface BotResponseRepository extends JpaRepository<BotResponse, Long> {
    List<BotResponse> findByConversationId(Long conversationId);
    List<BotResponse> findByMessageId(Long messageId);

    @Query("SELECT br FROM BotResponse br WHERE br.conversation.conversationId = :conversationId AND br.createdAt >= :startDate ORDER BY br.createdAt DESC")
    List<BotResponse> findResponsesByConversationAfterDate(@Param("conversationId") Long conversationId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(br.confidenceScore) FROM BotResponse br WHERE br.conversation.user.userId = :userId AND br.createdAt >= :startDate")
    Double getAverageConfidenceByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(br.userFeedback) FROM BotResponse br WHERE br.userFeedback IS NOT NULL AND br.conversation.user.userId = :userId AND br.createdAt >= :startDate")
    Double getAverageUserFeedbackByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}

// ==================== SearchIndex Entity (Missing in previous file) ====================
// Add this to your JPA_ENTITIES.java file if not already present

/*
@Entity
@Table(name = "search_index", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_conversation_id", columnList = "conversation_id")
})
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long searchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String searchKeywords;

    private Float relevanceScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        indexedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(String searchKeywords) { this.searchKeywords = searchKeywords; }
    public Float getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Float relevanceScore) { this.relevanceScore = relevanceScore; }
    public LocalDateTime getIndexedAt() { return indexedAt; }
}
*/

