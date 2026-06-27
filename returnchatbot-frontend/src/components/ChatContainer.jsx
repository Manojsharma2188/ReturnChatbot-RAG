import { useState, useEffect, useRef } from 'react';
import { Box, AppBar, Toolbar, Typography, Switch, FormControlLabel, CircularProgress, Chip } from '@mui/material';
import ChatMessage from './ChatMessage';
import ChatInput from './ChatInput';

function ChatContainer({ conversation, onConversationsChanged }) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      text: 'Hello! I am your AI assistant. How can I help you today?',
      role: 'assistant',
      timestamp: new Date(),
    },
  ]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [currentConversationId, setCurrentConversationId] = useState(null);
  const [ragEnabled, setRagEnabled] = useState(true);
  const messagesEndRef = useRef(null);
  const API_BASE_URL = 'http://localhost:9090/api/chat';

  useEffect(() => {
    if (conversation) {
      setCurrentConversationId(conversation.id);
      fetchConversationMessages(conversation.id);
    } else {
      setCurrentConversationId(null);
      setMessages([
        {
          id: 1,
          text: 'Hello! I am your AI assistant. How can I help you today?',
          role: 'assistant',
          timestamp: new Date(),
        },
      ]);
    }
  }, [conversation]);

  const fetchConversationMessages = async (conversationId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/messages`);
      if (response.ok) {
        const data = await response.json();
        const loadedMessages = data.map((msg, index) => ({
          id: Date.now() + index,
          text: msg.response,
          role: msg.role,
          timestamp: new Date(),
        }));
        setMessages(loadedMessages);
      }
    } catch (err) {
      console.error('Failed to load messages:', err);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSendMessage = async (userMessage) => {
    if (!userMessage.trim()) return;

    const userMsg = {
      id: Date.now(),
      text: userMessage,
      role: 'user',
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);
    setError('');

    try {
      let endpoint, body, responseField;

      if (ragEnabled) {
        endpoint = `${API_BASE_URL}/rag`;
        body = JSON.stringify({ message: userMessage });
        responseField = 'answer';
      } else {
        endpoint = API_BASE_URL;
        body = JSON.stringify({
          prompt: userMessage,
          conversationId: currentConversationId,
        });
        responseField = 'response';
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
      });

      if (!response.ok) {
        throw new Error(`API Error: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      if (data.conversationId) {
        setCurrentConversationId(data.conversationId);
        if (onConversationsChanged) {
          onConversationsChanged();
        }
      }

      const botMessage = {
        id: Date.now() + 1,
        text: data[responseField] || 'No response received',
        role: 'assistant',
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (err) {
      console.error('Error:', err);
      setError(err.message);

      const errorMsg = {
        id: Date.now() + 1,
        text: `Error: ${err.message}. Make sure the backend server is running at ${API_BASE_URL}`,
        role: 'assistant',
        timestamp: new Date(),
        isError: true,
      };

      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', bgcolor: 'grey.50' }}>
      <AppBar position="static" sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', maxWidth: 900, mx: 'auto' }}>
            <Typography variant="h5" fontWeight={700} letterSpacing="-0.5px">
              Return Chatbot
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={ragEnabled}
                    onChange={() => setRagEnabled(!ragEnabled)}
                    sx={{
                      '& .MuiSwitch-thumb': { bgcolor: 'white' },
                      '& .MuiSwitch-track': { bgcolor: 'rgba(255,255,255,0.4)' },
                      '&.Mui-checked .MuiSwitch-thumb': { bgcolor: 'white' },
                      '&.Mui-checked + .MuiSwitch-track': { bgcolor: 'rgba(255,255,255,0.9)' },
                    }}
                  />
                }
                label={
                  <Typography variant="body2" sx={{ color: 'white', fontWeight: 600 }}>
                    RAG
                  </Typography>
                }
                sx={{ m: 0 }}
              />
              <Chip
                label={ragEnabled ? '🔍 Policy' : '💬 Free'}
                size="small"
                sx={{
                  bgcolor: ragEnabled ? 'rgba(255,255,255,0.2)' : 'rgba(255,255,255,0.15)',
                  color: 'white',
                  fontWeight: 500,
                  fontSize: '0.75rem',
                }}
              />
            </Box>
          </Box>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        {messages.map((message) => (
          <ChatMessage
            key={message.id}
            message={message.text}
            isUser={message.role === 'user'}
          />
        ))}
        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            <CircularProgress size={24} sx={{ color: '#667eea' }} />
          </Box>
        )}
        <div ref={messagesEndRef} />
      </Box>

      <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} />
    </Box>
  );
}

export default ChatContainer;