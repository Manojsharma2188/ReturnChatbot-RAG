import { useState, useCallback } from 'react';
import { ThemeProvider, createTheme, CssBaseline, Box } from '@mui/material';
import ChatContainer from './components/ChatContainer';
import ConversationSidebar from './components/ConversationSidebar';

const theme = createTheme({
  palette: {
    primary: {
      main: '#667eea',
    },
    secondary: {
      main: '#764ba2',
    },
    background: {
      default: '#f5f7fa',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

function App() {
  const [activeConversation, setActiveConversation] = useState(null);
  const [sidebarRefreshKey, setSidebarRefreshKey] = useState(0);

  const handleSelectConversation = useCallback((conv) => {
    setActiveConversation(conv);
  }, []);

  const handleNewChat = useCallback(() => {
    setActiveConversation(null);
  }, []);

  const handleConversationsChanged = useCallback(() => {
    setSidebarRefreshKey((prev) => prev + 1);
  }, []);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', height: '100vh' }}>
        <ConversationSidebar
          key={sidebarRefreshKey}
          activeConversationId={activeConversation?.id}
          onSelectConversation={handleSelectConversation}
          onNewChat={handleNewChat}
        />
        <Box sx={{ flex: 1, overflow: 'hidden' }}>
          <ChatContainer
            conversation={activeConversation}
            onConversationsChanged={handleConversationsChanged}
          />
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App;