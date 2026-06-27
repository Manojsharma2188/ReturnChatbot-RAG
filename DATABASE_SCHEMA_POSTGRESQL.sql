-- PostgreSQL Database Schema for Returnchatbot
-- Supports: View previous conversations, Continue old chats, Search chat history, Build memory, Enable analytics

-- Create schema
CREATE SCHEMA IF NOT EXISTS chatbot;
SET search_path TO chatbot;

-- ==================== USERS TABLE ====================
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT ck_user_active CHECK (is_active IN (true, false))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- ==================== CONVERSATIONS TABLE ====================
CREATE TABLE conversations (
    conversation_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP,
    is_archived BOOLEAN DEFAULT FALSE,
    total_messages INTEGER DEFAULT 0,
    duration_seconds INTEGER,
    CONSTRAINT ck_conv_archived CHECK (is_archived IN (true, false))
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at);
CREATE INDEX idx_conversations_archived ON conversations(is_archived);

-- Full-text search index for conversations
CREATE INDEX idx_conversations_ft_search ON conversations USING GIN(to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, '')));

-- ==================== MESSAGES TABLE ====================
CREATE TYPE sender_type AS ENUM ('USER', 'BOT');

CREATE TABLE messages (
    message_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    sender_type sender_type NOT NULL,
    content TEXT NOT NULL,
    tokens_used INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_edited BOOLEAN DEFAULT FALSE,
    original_content TEXT,
    CONSTRAINT ck_msg_edited CHECK (is_edited IN (true, false))
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_messages_sender_type ON messages(sender_type);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_is_edited ON messages(is_edited);

-- Full-text search index for messages
CREATE INDEX idx_messages_ft_search ON messages USING GIN(to_tsvector('english', content));

-- ==================== CONVERSATION_CONTEXT TABLE ====================
CREATE TABLE conversation_context (
    context_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL UNIQUE REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    conversation_summary TEXT,
    key_topics JSONB,
    user_preferences JSONB,
    context_embeddings BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_context_conversation_id ON conversation_context(conversation_id);
CREATE INDEX idx_context_user_id ON conversation_context(user_id);
CREATE INDEX idx_context_key_topics ON conversation_context USING GIN(key_topics);
CREATE INDEX idx_context_preferences ON conversation_context USING GIN(user_preferences);

-- ==================== CHAT_ANALYTICS TABLE ====================
CREATE TYPE event_type AS ENUM ('MESSAGE_SENT', 'MESSAGE_RECEIVED', 'CONVERSATION_STARTED', 'CONVERSATION_ENDED', 'CONVERSATION_RESUMED');

CREATE TABLE chat_analytics (
    analytics_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES messages(message_id) ON DELETE SET NULL,
    event_type event_type NOT NULL,
    response_time_ms INTEGER,
    sentiment VARCHAR(50),
    model_used VARCHAR(100),
    temperature FLOAT,
    tokens_input INTEGER,
    tokens_output INTEGER,
    total_tokens INTEGER,
    session_duration_seconds INTEGER,
    user_satisfaction INTEGER CHECK (user_satisfaction >= 1 AND user_satisfaction <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    metadata JSONB
);

CREATE INDEX idx_analytics_conversation_id ON chat_analytics(conversation_id);
CREATE INDEX idx_analytics_user_id ON chat_analytics(user_id);
CREATE INDEX idx_analytics_message_id ON chat_analytics(message_id);
CREATE INDEX idx_analytics_event_type ON chat_analytics(event_type);
CREATE INDEX idx_analytics_created_at ON chat_analytics(created_at);
CREATE INDEX idx_analytics_user_satisfaction ON chat_analytics(user_satisfaction);
CREATE INDEX idx_analytics_metadata ON chat_analytics USING GIN(metadata);

-- ==================== SEARCH_INDEX TABLE ====================
CREATE TABLE search_index (
    search_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    search_keywords TEXT,
    relevance_score FLOAT DEFAULT 0.0,
    indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_search_user_id ON search_index(user_id);
CREATE INDEX idx_search_conversation_id ON search_index(conversation_id);
CREATE INDEX idx_search_message_id ON search_index(message_id);
CREATE INDEX idx_search_keywords_ft ON search_index USING GIN(to_tsvector('english', search_keywords));

-- ==================== USER_MEMORY TABLE ====================
CREATE TABLE user_memory (
    memory_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    memory_data JSONB,
    preferences JSONB,
    interaction_history JSONB,
    learned_patterns JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_memory_user_id ON user_memory(user_id);
CREATE INDEX idx_memory_data ON user_memory USING GIN(memory_data);
CREATE INDEX idx_memory_preferences ON user_memory USING GIN(preferences);
CREATE INDEX idx_memory_patterns ON user_memory USING GIN(learned_patterns);

-- ==================== BOT_RESPONSES TABLE ====================
CREATE TABLE bot_responses (
    response_id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
    conversation_id BIGINT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    response_text TEXT NOT NULL,
    model_version VARCHAR(50),
    confidence_score FLOAT CHECK (confidence_score >= 0 AND confidence_score <= 1),
    is_relevant BOOLEAN,
    user_feedback INTEGER CHECK (user_feedback IS NULL OR (user_feedback >= 1 AND user_feedback <= 5)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_responses_message_id ON bot_responses(message_id);
CREATE INDEX idx_responses_conversation_id ON bot_responses(conversation_id);
CREATE INDEX idx_responses_created_at ON bot_responses(created_at);
CREATE INDEX idx_responses_confidence ON bot_responses(confidence_score);

-- ==================== VIEWS ====================

-- View: User Conversations Summary
CREATE OR REPLACE VIEW user_conversations_summary AS
SELECT
    c.conversation_id,
    c.user_id,
    c.title,
    c.description,
    c.created_at,
    c.updated_at,
    c.last_accessed_at,
    COALESCE(COUNT(m.message_id), 0)::INTEGER as message_count,
    u.username,
    c.is_archived
FROM conversations c
LEFT JOIN messages m ON c.conversation_id = m.conversation_id
LEFT JOIN users u ON c.user_id = u.user_id
WHERE c.is_archived = FALSE
GROUP BY c.conversation_id, u.user_id, u.username;

-- View: Analytics Summary (Daily Aggregation)
CREATE OR REPLACE VIEW analytics_summary AS
SELECT
    DATE(ca.created_at) as date,
    ca.user_id,
    ca.event_type,
    COUNT(*) as event_count,
    AVG(ca.response_time_ms) as avg_response_time,
    AVG(ca.total_tokens) as avg_tokens,
    AVG(CASE WHEN ca.user_satisfaction IS NOT NULL THEN ca.user_satisfaction ELSE NULL END) as avg_satisfaction
FROM chat_analytics ca
GROUP BY DATE(ca.created_at), ca.user_id, ca.event_type;

-- View: Searchable Content
CREATE OR REPLACE VIEW searchable_content AS
SELECT
    m.message_id,
    m.conversation_id,
    m.user_id,
    m.content,
    m.sender_type,
    m.created_at,
    c.title as conversation_title,
    u.username,
    c.is_archived
FROM messages m
JOIN conversations c ON m.conversation_id = c.conversation_id
JOIN users u ON m.user_id = u.user_id
WHERE c.is_archived = FALSE;

-- ==================== FUNCTIONS ====================

-- Function: Update conversation updated_at timestamp
CREATE OR REPLACE FUNCTION update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations SET updated_at = CURRENT_TIMESTAMP
    WHERE conversation_id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Update conversation timestamp when message added
CREATE TRIGGER trg_message_updated_conversation
AFTER INSERT ON messages
FOR EACH ROW
EXECUTE FUNCTION update_conversation_timestamp();

-- Function: Update total_messages count
CREATE OR REPLACE FUNCTION update_message_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations
    SET total_messages = (SELECT COUNT(*) FROM messages WHERE conversation_id = NEW.conversation_id)
    WHERE conversation_id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Update message count
CREATE TRIGGER trg_message_count
AFTER INSERT ON messages
FOR EACH ROW
EXECUTE FUNCTION update_message_count();

-- Function: Update user_memory timestamp
CREATE OR REPLACE FUNCTION update_user_memory_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Update user_memory timestamp
CREATE TRIGGER trg_user_memory_timestamp
BEFORE UPDATE ON user_memory
FOR EACH ROW
EXECUTE FUNCTION update_user_memory_timestamp();

-- Function: Update conversation_context timestamp
CREATE OR REPLACE FUNCTION update_context_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Update conversation_context timestamp
CREATE TRIGGER trg_context_timestamp
BEFORE UPDATE ON conversation_context
FOR EACH ROW
EXECUTE FUNCTION update_context_timestamp();

-- ==================== SAMPLE DATA (OPTIONAL) ====================
-- Uncomment to populate with sample data for testing

/*
-- Insert sample users
INSERT INTO users (username, email, is_active) VALUES
('john_doe', 'john@example.com', true),
('jane_smith', 'jane@example.com', true),
('bob_wilson', 'bob@example.com', true);

-- Insert sample conversations
INSERT INTO conversations (user_id, title, description, total_messages)
SELECT user_id, 'Sample Conversation 1', 'Testing conversation features', 0
FROM users WHERE username = 'john_doe'
UNION ALL
SELECT user_id, 'Sample Conversation 2', 'Another test conversation', 0
FROM users WHERE username = 'jane_smith';

-- Insert sample messages
INSERT INTO messages (conversation_id, user_id, sender_type, content, tokens_used)
SELECT c.conversation_id, u.user_id, 'USER', 'Hello, how can I help?', 5
FROM conversations c
JOIN users u ON c.user_id = u.user_id
WHERE u.username = 'john_doe'
LIMIT 1;

INSERT INTO messages (conversation_id, user_id, sender_type, content, tokens_used)
SELECT c.conversation_id, u.user_id, 'BOT', 'Hi! I am here to assist you.', 8
FROM conversations c
JOIN users u ON c.user_id = u.user_id
WHERE u.username = 'john_doe'
LIMIT 1;
*/

-- ==================== SUMMARY ====================
-- Created tables:
-- 1. users - User accounts
-- 2. conversations - Chat sessions
-- 3. messages - Individual messages
-- 4. conversation_context - Context and memory for conversations
-- 5. chat_analytics - Interaction analytics
-- 6. search_index - Search optimization
-- 7. user_memory - Long-term user memory
-- 8. bot_responses - Bot response tracking
--
-- Created ENUMs:
-- - sender_type (USER, BOT)
-- - event_type (MESSAGE_SENT, MESSAGE_RECEIVED, CONVERSATION_STARTED, CONVERSATION_ENDED, CONVERSATION_RESUMED)
--
-- Created Views:
-- 1. user_conversations_summary
-- 2. analytics_summary
-- 3. searchable_content
--
-- Created Triggers:
-- - Automatic timestamp updates
-- - Automatic message count updates
-- - Conversation timestamp synchronization
--
-- Full-text search enabled on:
-- - conversations.title and description
-- - messages.content
-- - search_index.search_keywords

