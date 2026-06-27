package com.chatbot.returnchatbot.service;

import com.chatbot.returnchatbot.config.RagConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Service that implements the Retrieval-Augmented Generation (RAG) flow.
 *
 * Uses direct REST API calls to Qdrant (HTTP) and Ollama since
 * Qdrant is accessed via HTTP REST API on port 6333 (not gRPC).
 *
 * Flow:
 * 1. Generate embedding for question via Ollama
 * 2. Search Qdrant for top 5 relevant vectors
 * 3. Build context from retrieved chunks
 * 4. Send context + question to llama3.2
 * 5. Return the answer
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final WebClient ollamaWebClient;
    private final WebClient qdrantWebClient;
    private final Gson gson = new Gson();

    @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${spring.ai.ollama.chat.model:llama3.2}")
    private String chatModel;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:returns-docs}")
    private String collectionName;

    private static final String PROMPT_TEMPLATE = """
            You are a Returns Assistant.

            Answer ONLY using provided context.

            If answer is not found in context,
            say:
            "I could not find this information in the returns knowledge base."

            Context:
            %s

            Question:
            %s
            """;

    public RagService(RagConfig ragConfig, WebClient.Builder webClientBuilder) {
        this.ollamaWebClient = webClientBuilder.baseUrl(
                ragConfig.getOllamaBaseUrl()).build();
        this.qdrantWebClient = webClientBuilder.baseUrl(
                "http://" + ragConfig.getQdrantHost() + ":" + ragConfig.getQdrantPort()).build();
    }

    public String ask(String question) {
        log.info("RAG query received: {}", question);

        try {
            // Step 1: Generate embedding for the question using Ollama
            log.debug("Generating embedding for question");
            float[] questionEmbedding = generateEmbedding(question);

            // Step 2: Search Qdrant for similar documents
            log.debug("Searching Qdrant vector store");
            String context = searchQdrant(questionEmbedding);

            if (context.isEmpty()) {
                log.warn("No context found for question: {}", question);
                return "I could not find this information in the returns knowledge base.";
            }

            log.debug("Context length: {} characters", context.length());

            // Step 3: Build prompt and send to Ollama chat
            String prompt = String.format(PROMPT_TEMPLATE, context, question);
            String answer = callOllamaChat(prompt);

            log.info("RAG answer generated ({} chars)", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("Error processing RAG query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process RAG query: " + e.getMessage(), e);
        }
    }

    private float[] generateEmbedding(String text) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", embeddingModel);
        requestBody.addProperty("prompt", text);

        String response = ollamaWebClient.post()
                .uri("/api/embeddings")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) {
            throw new RuntimeException("No response from Ollama embedding API");
        }

        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray embeddingArray = jsonResponse.getAsJsonArray("embedding");

        float[] embedding = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            embedding[i] = embeddingArray.get(i).getAsFloat();
        }

        return embedding;
    }

    private String searchQdrant(float[] embedding) {
        // Build Qdrant search request body according to REST API:
        // POST /collections/{name}/points/search
        JsonArray vectorArray = new JsonArray();
        for (float v : embedding) {
            vectorArray.add(v);
        }

        JsonObject searchRequest = new JsonObject();
        searchRequest.add("vector", vectorArray);
        searchRequest.addProperty("limit", 5);
        searchRequest.addProperty("with_payload", true);

        String response = qdrantWebClient.post()
                .uri("/collections/{collection}/points/search", collectionName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(searchRequest.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) {
            return "";
        }

        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        
        // Qdrant v1.x search response: result is a JSON array of points directly
        JsonArray points = null;
        if (jsonResponse.has("result") && jsonResponse.get("result").isJsonArray()) {
            points = jsonResponse.getAsJsonArray("result");
        }
        
        if (points == null || points.size() == 0) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            JsonObject point = points.get(i).getAsJsonObject();
            JsonObject payload = point.getAsJsonObject("payload");
            if (payload != null && payload.has("text")) {
                if (contextBuilder.length() > 0) {
                    contextBuilder.append("\n\n---\n\n");
                }
                contextBuilder.append(payload.get("text").getAsString());
            }
        }

        return contextBuilder.toString();
    }

    private String callOllamaChat(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", chatModel);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);

        String response = ollamaWebClient.post()
                .uri("/api/generate")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) {
            return "I could not find this information in the returns knowledge base.";
        }

        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        return jsonResponse.get("response").getAsString();
    }
}