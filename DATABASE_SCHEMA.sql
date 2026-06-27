-- Database Schema for Returnchatbot
-- Supports: View previous conversations, Continue old chats, Search chat history, Build memory, Enable analytics

-- 1. USERS Table
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- 2. CONVERSATIONS Table (stores chat sessions)
CREATE TABLE conversations (
    conversation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    is_archived BOOLEAN DEFAULT FALSE,
    total_messages INT DEFAULT 0,
    duration_seconds INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at),
    FULLTEXT INDEX ft_title_description (title, description)
);

-- 3. MESSAGES Table (stores individual messages)
CREATE TABLE messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sender_type ENUM('USER', 'BOT') NOT NULL,
    content LONGTEXT NOT NULL,
    tokens_used INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_edited BOOLEAN DEFAULT FALSE,
    original_content LONGTEXT,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_sender_type (sender_type),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_content (content)
);

-- 4. CONVERSATION_CONTEXT Table (for memory/context preservation)
CREATE TABLE conversation_context (
    context_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    conversation_summary TEXT,
    key_topics JSON,
    user_preferences JSON,
    context_embeddings LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id)
);

-- 5. CHAT_ANALYTICS Table (for analytics tracking)
CREATE TABLE chat_analytics (
    analytics_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message_id BIGINT,
    event_type ENUM('MESSAGE_SENT', 'MESSAGE_RECEIVED', 'CONVERSATION_STARTED', 'CONVERSATION_ENDED', 'CONVERSATION_RESUMED') NOT NULL,
    response_time_ms INT,
    sentiment VARCHAR(50),
    model_used VARCHAR(100),
    temperature FLOAT,
    tokens_input INT,
    tokens_output INT,
    total_tokens INT,
    session_duration_seconds INT,
    user_satisfaction INT COMMENT 'Rating 1-5',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSON,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE SET NULL,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
);

-- 6. SEARCH_INDEX Table (for optimized search)
CREATE TABLE search_index (
    search_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT,
    user_id BIGINT NOT NULL,
    search_keywords TEXT,
    relevance_score FLOAT,
    indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id),
    FULLTEXT INDEX ft_keywords (search_keywords)
);

-- 7. USER_MEMORY Table (for building user memory over time)
CREATE TABLE user_memory (
    memory_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    memory_data JSON,
    preferences JSON,
    interaction_history JSON,
    learned_patterns JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);

-- 8. BOT_RESPONSES Table (for tracking and improving bot responses)
CREATE TABLE bot_responses (
    response_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    response_text LONGTEXT NOT NULL,
    model_version VARCHAR(50),
    confidence_score FLOAT,
    is_relevant BOOLEAN,
    user_feedback INT COMMENT 'Rating 1-5, NULL if no feedback',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
);

-- Useful Views for Common Operations

-- View to see all conversations with message count
CREATE VIEW user_conversations_summary AS
SELECT
    c.conversation_id,
    c.user_id,
    c.title,
    c.description,
    c.created_at,
    c.updated_at,
    c.last_accessed_at,
    COUNT(m.message_id) as message_count,
    u.username
FROM conversations c
LEFT JOIN messages m ON c.conversation_id = m.conversation_id
LEFT JOIN users u ON c.user_id = u.user_id
WHERE c.is_archived = FALSE
GROUP BY c.conversation_id;

-- View for analytics dashboard
CREATE VIEW analytics_summary AS
SELECT
    DATE(ca.created_at) as date,
    ca.user_id,
    ca.event_type,
    COUNT(*) as event_count,
    AVG(ca.response_time_ms) as avg_response_time,
    AVG(ca.total_tokens) as avg_tokens,
    AVG(ca.user_satisfaction) as avg_satisfaction
FROM chat_analytics ca
GROUP BY DATE(ca.created_at), ca.user_id, ca.event_type;

-- View for search functionality
CREATE VIEW searchable_content AS
SELECT
    m.message_id,
    m.conversation_id,
    m.user_id,
    m.content,
    m.sender_type,
    m.created_at,
    c.title as conversation_title,
    u.username
FROM messages m
JOIN conversations c ON m.conversation_id = c.conversation_id
JOIN users u ON m.user_id = u.user_id
WHERE c.is_archived = FALSE;

