# Return Chatbot - React Frontend

A beautiful ChatGPT-style UI for the Return Chatbot built with React and Vite.

## Features

- **ChatGPT-Style UI**: Modern, sleek interface with gradient styling
- **Real-time Chat**: Send messages and receive responses from the Spring Boot backend
- **Auto-scrolling**: Automatically scrolls to the latest message
- **Typing Indicator**: Shows when the bot is thinking
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile devices
- **Auto-expanding Textarea**: Input field expands as you type
- **Error Handling**: Gracefully handles connection errors

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- The Spring Boot backend running on port 9090

## Installation

```bash
cd returnchatbot-frontend
npm install
```

## Running the Development Server

```bash
npm run dev
```

The application will start on `http://localhost:5173` (or another port if 5173 is busy).

## Building for Production

```bash
npm run build
```

The built files will be in the `dist` directory.

## Connecting to the Backend

The frontend is configured to connect to the backend at:
```
http://localhost:9090/api/chat
```

### Backend Configuration

Make sure your Spring Boot backend is running with:
- Server port: 9090
- Chat endpoint: POST `/api/chat`
- Expected request body: `{ "prompt": "user message" }`
- Expected response: `{ "response": "bot response" }`

### Running the Backend

From the parent directory:
```bash
cd ..
java -jar target/Returnchatbot-0.0.1-SNAPSHOT.jar --server.port=9090
```

Or if using Maven:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

## Project Structure

```
src/
├── components/
│   ├── ChatContainer.jsx     # Main chat container component
│   ├── ChatContainer.css     # Chat container styles
│   ├── ChatMessage.jsx       # Individual message component
│   ├── ChatMessage.css       # Message styles
│   ├── ChatInput.jsx         # Input field component
│   └── ChatInput.css         # Input styles
├── App.jsx                   # Main App component
├── App.css                   # App styles
├── index.css                 # Global styles
└── main.jsx                  # Entry point
```

## Component Details

### ChatContainer
- Manages the overall chat state
- Handles API communication with the backend
- Displays messages and manages auto-scrolling

### ChatMessage
- Displays individual messages
- Differentiates between user and bot messages
- Supports message animation

### ChatInput
- Expandable textarea for user input
- Send button with loading state
- Keyboard shortcuts (Enter to send, Shift+Enter for new line)

## Keyboard Shortcuts

- **Enter**: Send message
- **Shift + Enter**: New line in message input

## Styling

The frontend uses modern CSS with:
- Gradient backgrounds
- Smooth animations
- Responsive breakpoints for mobile, tablet, and desktop
- Custom scrollbar styling
- Box shadows and transitions

## Customization

### Change Backend URL

Edit `ChatContainer.jsx` line 32:
```javascript
const API_BASE_URL = 'http://localhost:9090/api/chat';
```

### Change Colors

Edit the gradient colors in component CSS files:
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### Change Header Text

Edit `ChatContainer.jsx` to change the title and subtitle.

## Troubleshooting

### Connection Refused Error
- Make sure the Spring Boot backend is running on port 9090
- Check that the backend is accessible at `http://localhost:9090/api/chat`
- The error message in the chat will help identify the issue

### CORS Issues
If you encounter CORS errors, add the following to your Spring Boot application:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                    .allowedMethods("*")
                    .allowedHeaders("*");
            }
        };
    }
}
```

## Performance Optimization

- Messages are rendered using React keys for efficient updates
- CSS animations use GPU acceleration
- Auto-scrolling uses smooth behavior for better UX
- Message list uses virtual scrolling for large conversations (can be added)

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari, Chrome Mobile)

## License

MIT

