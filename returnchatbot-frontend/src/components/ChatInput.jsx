import { useState, useRef, useEffect } from 'react';
import { Box, IconButton, TextField, CircularProgress } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';

function ChatInput({ onSendMessage, isLoading }) {
  const [input, setInput] = useState('');
  const inputRef = useRef(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (input.trim() && !isLoading) {
      onSendMessage(input);
      setInput('');
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      sx={{
        borderTop: '1px solid',
        borderColor: 'divider',
        bgcolor: 'background.paper',
        p: 2,
      }}
    >
      <Box sx={{ display: 'flex', gap: 1, maxWidth: 900, mx: 'auto' }}>
        <TextField
          fullWidth
          multiline
          maxRows={4}
          inputRef={inputRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Message the AI chatbot..."
          disabled={isLoading}
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: 3,
              bgcolor: 'grey.50',
            },
          }}
        />
        <IconButton
          type="submit"
          disabled={isLoading || !input.trim()}
          sx={{
            bgcolor: '#667eea',
            color: 'white',
            borderRadius: 2,
            width: 48,
            height: 48,
            '&:hover': { bgcolor: '#5a6fd6' },
            '&.Mui-disabled': { bgcolor: 'grey.300', color: 'grey.500' },
          }}
        >
          {isLoading ? (
            <CircularProgress size={20} color="inherit" />
          ) : (
            <SendIcon />
          )}
        </IconButton>
      </Box>
    </Box>
  );
}

export default ChatInput;