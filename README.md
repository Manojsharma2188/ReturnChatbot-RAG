# Return Chatbot

## Section 1: What is this project? (Layman's Explanation)

Imagine you run a business and have a **returns policy PDF** (like "30-day return window, items must be unopened, refund processed within 5 business days"). Your customers keep asking the same questions:

- *"What is the return window?"*
- *"Can I return opened items?"*
- *"How long does a refund take?"*

Instead of answering manually, this chatbot reads your PDF, understands it, and answers customer questions **directly from your policy document**. It's like having an employee who has memorized your entire returns policy and can answer any question instantly.

### How it works (simple version):

1. **You upload** your returns policy PDF into the `knowledge-base/` folder
2. **The system reads** the PDF, breaks it into small chunks, and stores them in a special database (Qdrant) that understands meaning, not just keywords
3. **When a user asks** a question like "What is the return window?", the system:
   - Finds the most relevant parts of your PDF
   - Sends those parts + the question to an AI (Llama 3.2)
   - The AI answers **only from your policy**, not from general internet knowledge
4. **The answer** appears in the chat UI

### Two Chat Modes:

| Mode | What it does | When to use |
|------|-------------|-------------|
| **RAG Mode** 🔍 (default) | Answers from your PDF policy | Policy/document questions |
| **Free Chat** 💬 | General AI conversation | Casual chat, brainstorming |

You can toggle between modes with the switch in the header.
**RAG Enabled**
<img width="1920" height="1080" alt="Screenshot from 2026-06-27 17-41-03" src="https://github.com/user-attachments/assets/15534334-0eb3-43b0-80ea-d447505933fe" />

**RAG Disabled**
<img width="1920" height="1080" alt="Screenshot from 2026-06-27 17-47-15" src="https://github.com/user-attachments/assets/4bea7702-9804-4f29-beac-afce17ed2110" />

---

## Section 2: Setup Guide (For Local Development)

### Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 17+ | Backend runtime |
| Node.js | 18+ | Frontend runtime |
| PostgreSQL | 14+ | Stores conversations & messages |
| Docker | Any | Runs Qdrant (vector database) |
| Ollama | Latest | Runs AI models locally |

### Step 1: Start Required Services

```bash
# 1. PostgreSQL (conversation history)
sudo systemctl start postgresql
# Create database (one-time):
psql -U postgres -c "CREATE DATABASE chatbot_db;"

# 2. Qdrant (vector database for PDF chunks)
docker run -d -p 6333:6333 --name qdrant qdrant/qdrant

# 3. Ollama (AI models)
ollama serve
# Pull required models (one-time):
ollama pull llama3.2
ollama pull nomic-embed-text
```

### Step 2: Configure

Edit `src/main/resources/application.properties` if your defaults differ:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/chatbot_db
spring.datasource.username=postgres
spring.datasource.password=postgres123

# Ollama
spring.ai.ollama.base-url=http://localhost:11434

# Qdrant
spring.ai.vectorstore.qdrant.host=localhost
spring.ai.vectorstore.qdrant.port=6333
```

### Step 3: Add Your Policy PDF

Place your returns policy PDF in the `knowledge-base/` folder:

```bash
cp /path/to/your/returns-policy.pdf knowledge-base/
```

### Step 4: Run the Application

```bash
# Terminal 1: Backend (Spring Boot, port 9090)
./mvnw spring-boot:run

# Terminal 2: Frontend (React, port 5173)
cd returnchatbot-frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

### What happens on first run:

1. Backend starts on port 9090
2. `KnowledgeBaseLoader` reads your PDF from `knowledge-base/`
3. Extracts text, splits into 500-character chunks
4. Generates AI embeddings via Ollama (`nomic-embed-text`)
5. Stores chunks in Qdrant collection `returns-docs`
6. Server is ready — this takes ~15-60 seconds depending on PDF size
7. **Subsequent restarts** skip re-indexing (fast ~5s startup)

### Project Structure

```
Returnchatbot/
│
├── pom.xml                                    # Maven build
├── knowledge-base/                            # 📄 Put your PDFs here
│   └── Sample-Returns-Policy.pdf
│
├── src/main/java/com/chatbot/returnchatbot/
│   ├── ReturnchatbotApplication.java          # Entry point
│   ├── config/
│   │   ├── CorsConfig.java                   # CORS for frontend
│   │   └── RagConfig.java                    # WebClient beans
│   ├── controller/
│   │   └── ChatController.java               # REST API endpoints
│   ├── dto/                                   # Request/Response objects
│   ├── entity/
│   │   ├── Conversation.java                 # JPA: conversations table
│   │   └── Message.java                      # JPA: messages table
│   ├── repository/                            # Database access
│   ├── service/
│   │   ├── ChatService.java                  # Plain chat logic
│   │   └── RagService.java                   # RAG pipeline logic
│   └── loader/
│       └── KnowledgeBaseLoader.java          # PDF → Qdrant ingestion
│
├── src/main/resources/
│   └── application.properties                # All configuration
│
└── returnchatbot-frontend/                   # React UI (Vite + MUI)
    └── src/
        ├── App.jsx                           # Main app with theme
        └── components/
            ├── ChatContainer.jsx             # Chat area + RAG toggle
            ├── ChatInput.jsx                 # Message input
            ├── ChatMessage.jsx               # Message bubble
            └── ConversationSidebar.jsx       # History sidebar
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, Vite, Material UI (MUI) |
| Backend | Spring Boot 3.2.5, Java 17 |
| Database | PostgreSQL (conversations & messages) |
| Vector DB | Qdrant (PDF chunk embeddings) |
| AI Models | Ollama — Llama 3.2 (chat), Nomic-embed-text (embeddings) |
| PDF Parsing | Apache PDFBox |

---

## Section 3: Architecture & Code Flow

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Frontend (React + MUI, port 5173)                │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐   │
│  │ ChatContainer│─▶│  ChatInput   │  │ ConversationSidebar    │   │
│  └──────┬───────┘  └──────────────┘  └───────────┬────────────┘   │
│         │ HTTP POST /api/chat/*                    │ GET /api/chat/*│
└─────────┼──────────────────────────────────────────┼────────────────┘
          │                                          │
          ▼                                          ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   Backend (Spring Boot, port 9090)                    │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  ChatController                                              │    │
│  │                                                              │    │
│  │  POST /api/chat        → ChatService (plain chat)            │    │
│  │  POST /api/chat/rag    → RagService (RAG-based chat)         │    │
│  │  POST /api/chat/new    → create new conversation             │    │
│  │  GET  /api/chat/conversations         → list conversations   │    │
│  │  GET  /api/chat/conversations/{id}/messages  → get messages  │    │
│  └───────┬──────────────────────────────────┬───────────────────┘    │
│          │                                  │                        │
│          ▼                                  ▼                        │
│  ┌────────────┐                    ┌──────────────────┐              │
│  │ ChatService│                    │   RagService     │              │
│  │            │                    │                  │              │
│  │- DB ops    │                    │- embedding gen   │              │
│  │- Ollama    │                    │- Qdrant search   │              │
│  │  chat call │                    │- Ollama chat call│              │
│  └─────┬──────┘                    └────────┬─────────┘              │
│        │                                    │                        │
│        ▼                                    ▼                        │
│  ┌────────────┐                    ┌──────────────────┐              │
│  │ PostgreSQL │                    │  Qdrant (vector  │              │
│  │  (port 5432)│                    │   DB, port 6333) │              │
│  │            │                    │                  │              │
│  │- conversations                  │- stores embeddings│             │
│  │- messages   │                    │- returns relevant│             │
│  └────────────┘                    │  chunks by        │              │
│                                    │  cosine similarity│              │
│                                    └────────┬─────────┘              │
│                                             │                        │
│  Both services call:                        │                        │
│  ┌──────────────────────────────────────────┼──────────────┐         │
│  │  Ollama (port 11434)                    │              │         │
│  │                                         ▼              │         │
│  │  llama3.2 (chat model) ─────────────────────────────   │         │
│  │  nomic-embed-text (embedding model)                     │         │
│  └────────────────────────────────────────────────────────┘         │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  KnowledgeBaseLoader (startup runner)                        │   │
│  │  1. Reads PDFs from knowledge-base/                          │   │
│  2. Extracts text via PDFBox                                    │   │
│  3. Splits into 500-char chunks                                 │   │
│  4. Generates embeddings via Ollama                             │   │
│  5. Stores in Qdrant collection                                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

### API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/chat` | Plain chat (Free Chat mode) |
| POST | `/api/chat/rag` | RAG-powered chat (Policy mode) |
| POST | `/api/chat/new` | Create new conversation |
| GET | `/api/chat/conversations` | List all conversations |
| GET | `/api/chat/conversations/{id}/messages` | Get messages for a conversation |
| DELETE | `/api/chat/conversations/{id}` | Delete a conversation |

#### `POST /api/chat` — Plain Chat (Free Chat mode)
- **Request:** `{ "prompt": "Hello", "conversationId": null }`
- **Response:** `{ "response": "...", "role": "assistant", "conversationId": 1 }`
- **Flow:** Saves to PostgreSQL → Calls Ollama directly → Returns generic AI answer

#### `POST /api/chat/rag` — RAG Chat (Policy mode)
- **Request:** `{ "message": "What is the return window?" }`
- **Response:** `{ "answer": "30 days from purchase...", "conversationId": 1 }`
- **Flow:**
  1. Generate embedding of question via Ollama (`nomic-embed-text`)
  2. Search Qdrant for top 5 most similar chunks
  3. Build prompt with retrieved context
  4. Send to Ollama (`llama3.2`) with instruction: *"Answer ONLY using provided context"*
  5. Save conversation + messages to PostgreSQL (both user query and bot answer)
  6. Return answer with `conversationId` for history tracking

#### `POST /api/chat/new` — New Conversation
- **Response:** `{ "id": 1, "title": "Chat with AI - ..." }`

#### `GET /api/chat/conversations` — List Conversations
- **Response:** `[ { "id": 1, "title": "What is the return window?" } ]`

#### `GET /api/chat/conversations/{id}/messages` — Get Messages
- **Response:** `[ { "response": "...", "role": "user" }, ... ]`

#### `DELETE /api/chat/conversations/{id}` — Delete Conversation
- **Response:** `204 No Content`
- **Flow:** Deletes all messages for the conversation first, then deletes the conversation record

### RAG Flow (Detailed)

```
User: "What is the return window?"
       │
       ▼
ChatContainer.jsx ──POST /api/chat/rag──▶ ChatController.ragChat()
       │                                         │
       │                                   RagService.ask("What is the return window?")
       │                                         │
       │                                   1. generateEmbedding(question)
       │                                         │
       │                                   POST http://localhost:11434/api/embeddings
       │                                   Model: nomic-embed-text
       │                                   Returns: [0.123, -0.456, ...] (768 floats)
       │                                         │
       │                                   2. searchQdrant(embedding)
       │                                         │
       │                                   POST http://localhost:6333/collections/returns-docs/points/search
       │                                   Returns: Top 5 most similar text chunks
       │                                         │
       │                                   3. Build prompt:
       │                                      "Context: [chunk 1]...[chunk 5]
       │                                       Question: What is the return window?
       │                                       Answer ONLY using provided context."
       │                                         │
       │                                   4. callOllamaChat(prompt)
       │                                         │
       │                                   POST http://localhost:11434/api/generate
       │                                   Model: llama3.2
       │                                         │
       │                                   5. ChatService.saveRagConversation()
       │                                      → INSERT into conversations table
       │                                      → INSERT into messages table (user)
       │                                      → INSERT into messages table (assistant)
       │                                         │
       │◀────────── "30 days from purchase" ◀─────┘
       │
       ▼
  Display in chat UI
```

### Database Schema

**PostgreSQL — `conversations` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-generated |
| title | VARCHAR(255) | Conversation title (first user message) |
| created_at | TIMESTAMP | When created |
| updated_at | TIMESTAMP | Last activity |

**PostgreSQL — `messages` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-generated |
| conversation_id | BIGINT (FK) | References conversations.id |
| role | VARCHAR(20) | "user" or "assistant" |
| content | TEXT | Message text |
| created_at | TIMESTAMP | When sent |

**Qdrant — `returns-docs` collection:**
| Field | Description |
|-------|-------------|
| Vector | 768 floats (nomic-embed-text embedding) |
| Distance | Cosine similarity |
| Payload.text | The actual text chunk from PDF |
| Payload.source | Source PDF filename |
| Payload.chunk | Chunk index number |

### Key Concepts

**What is RAG (Retrieval-Augmented Generation)?**
LLMs only know what they were trained on. To answer questions about your specific documents, you need to:
1. **Retrieve** relevant text chunks from your documents (Qdrant stores vector embeddings)
2. **Augment** the LLM prompt with that context
3. **Generate** an answer based on the provided context

**Why Qdrant instead of just searching the PDF text?**
Qdrant uses vector embeddings — it finds chunks by **meaning** not by keywords. For example, "What's the deadline for returns?" will find chunks about "30-day return window" even though they use different words.

**Why two chat modes?**
- **RAG Mode** is for policy questions — answers are grounded in your PDF
- **Free Chat** is for general conversation — uses the AI's built-in knowledge
