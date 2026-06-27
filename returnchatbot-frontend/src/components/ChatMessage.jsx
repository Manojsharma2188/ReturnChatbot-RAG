import { Paper, Typography, Box } from '@mui/material';
import { SmartToy, Person } from '@mui/icons-material';

function ChatMessage({ message, isUser }) {
  return (
    <Box sx={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      px: 2,
      py: 0.5,
    }}>
      <Box sx={{
        display: 'flex',
        gap: 1,
        maxWidth: '75%',
        flexDirection: isUser ? 'row-reverse' : 'row',
        alignItems: 'flex-start',
      }}>
        <Box sx={{
          mt: 0.5,
          color: isUser ? '#667eea' : '#764ba2',
        }}>
          {isUser ? <Person /> : <SmartToy />}
        </Box>
        <Paper
          elevation={1}
          sx={{
            p: 2,
            borderRadius: 2,
            bgcolor: isUser ? '#667eea' : 'white',
            color: isUser ? 'white' : 'text.primary',
            borderBottomRightRadius: isUser ? 0 : 2,
            borderBottomLeftRadius: isUser ? 2 : 0,
            wordBreak: 'break-word',
          }}
        >
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
            {message}
          </Typography>
        </Paper>
      </Box>
    </Box>
  );
}

export default ChatMessage;