import { useState, useEffect } from 'react';
import {
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Box,
  Typography,
  Button,
  IconButton,
  useMediaQuery,
  useTheme,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import {
  Add as AddIcon,
  Chat as ChatIcon,
  MenuOpen as MenuOpenIcon,
  Menu as MenuIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';

const API_BASE_URL = 'http://localhost:9090/api/chat';

function ConversationSidebar({ activeConversationId, onSelectConversation, onNewChat }) {
  const [conversations, setConversations] = useState([]);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [conversationToDelete, setConversationToDelete] = useState(null);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  useEffect(() => {
    fetchConversations();
  }, []);

  const fetchConversations = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/conversations`);
      if (response.ok) {
        const data = await response.json();
        setConversations(data);
      }
    } catch (err) {
      console.error('Failed to fetch conversations:', err);
    }
  };

  const handleNewChat = () => {
    onNewChat();
    setMobileOpen(false);
  };

  const handleSelect = (conv) => {
    onSelectConversation(conv);
    setMobileOpen(false);
  };

  const handleDeleteClick = (e, conv) => {
    e.stopPropagation();
    setConversationToDelete(conv);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!conversationToDelete) return;

    try {
      const response = await fetch(`${API_BASE_URL}/conversations/${conversationToDelete.id}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`Delete failed: ${response.status}`);
      }

      // If the deleted conversation was active, start a new chat
      if (activeConversationId === conversationToDelete.id) {
        onNewChat();
      }

      // Refresh the conversation list
      await fetchConversations();
    } catch (err) {
      console.error('Failed to delete conversation:', err);
    } finally {
      setDeleteDialogOpen(false);
      setConversationToDelete(null);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setConversationToDelete(null);
  };

  const sidebarContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: '#1e1e2e', color: '#cdd6f4' }}>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: '#313244' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="subtitle2" fontWeight={600} color="white" sx={{ fontSize: '0.8rem' }}>
            Chat History
          </Typography>
          {isMobile && (
            <IconButton size="small" onClick={() => setMobileOpen(false)} sx={{ color: '#cdd6f4' }}>
              <MenuOpenIcon />
            </IconButton>
          )}
        </Box>
        <Button
          fullWidth
          size="small"
          variant="outlined"
          startIcon={<AddIcon />}
          onClick={handleNewChat}
          sx={{
            color: '#cdd6f4',
            borderColor: '#45475a',
            textTransform: 'none',
            fontSize: '0.75rem',
            '&:hover': { borderColor: '#585b70', bgcolor: '#313244' },
          }}
        >
          New Chat
        </Button>
      </Box>

      <List sx={{ flex: 1, overflowY: 'auto', py: 0.5, px: 0.5 }}>
        {conversations.length === 0 ? (
          <Box sx={{ p: 3, textAlign: 'center', color: '#6c7086' }}>
            <Typography variant="body2">No conversations yet</Typography>
          </Box>
        ) : (
          conversations.map((conv) => (
            <ListItemButton
              key={conv.id}
              selected={activeConversationId === conv.id}
              onClick={() => handleSelect(conv)}
              sx={{
                borderRadius: 1,
                mb: 0.25,
                pr: 1,
                color: '#cdd6f4',
                '&:hover': { bgcolor: '#313244', '& .delete-btn': { opacity: 1 } },
                '&.Mui-selected': { bgcolor: '#45475a', '&:hover': { bgcolor: '#45475a' } },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    bgcolor: '#313244',
                    borderRadius: 1,
                    color: '#89b4fa',
                  }}
                >
                  <ChatIcon sx={{ fontSize: 18 }} />
                </Box>
              </ListItemIcon>
              <ListItemText
                primary={conv.title}
                primaryTypographyProps={{
                  noWrap: true,
                  fontSize: '0.8rem',
                  color: '#cdd6f4',
                }}
              />
              <IconButton
                size="small"
                className="delete-btn"
                onClick={(e) => handleDeleteClick(e, conv)}
                sx={{
                  color: '#6c7086',
                  '&:hover': { color: '#f38ba8', bgcolor: 'rgba(243, 139, 168, 0.1)' },
                  opacity: 0,
                  padding: '4px',
                  ml: 'auto',
                }}
              >
                <DeleteIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </ListItemButton>
          ))
        )}
      </List>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        PaperProps={{
          sx: { bgcolor: '#1e1e2e', color: '#cdd6f4', minWidth: 360 },
        }}
      >
        <DialogTitle sx={{ color: 'white' }}>Delete Conversation</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ color: '#a6adc8' }}>
            Are you sure you want to delete this conversation? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ pb: 2, px: 3 }}>
          <Button
            onClick={handleDeleteCancel}
            sx={{ color: '#cdd6f4', textTransform: 'none' }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            variant="contained"
            color="error"
            sx={{ textTransform: 'none' }}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );

  return (
    <>
      {isMobile && (
        <IconButton
          onClick={() => setMobileOpen(true)}
          sx={{
            position: 'fixed',
            top: 10,
            left: 10,
            zIndex: 1200,
            bgcolor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            '&:hover': { bgcolor: '#5a6fd6' },
          }}
        >
          <MenuIcon />
        </IconButton>
      )}

      {isMobile ? (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': { width: 280, bgcolor: '#1e1e2e' },
          }}
        >
          {sidebarContent}
        </Drawer>
      ) : (
        <Box sx={{ width: 280, minWidth: 280, height: '100vh', borderRight: 1, borderColor: '#313244' }}>
          {sidebarContent}
        </Box>
      )}
    </>
  );
}

export default ConversationSidebar;