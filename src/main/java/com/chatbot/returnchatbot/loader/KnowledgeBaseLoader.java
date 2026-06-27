package com.chatbot.returnchatbot.loader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Startup component that reads PDFs from the knowledge-base directory,
 * extracts text, splits into chunks, generates embeddings via Ollama,
 * and stores them in Qdrant via REST API.
 */
@Component
public class KnowledgeBaseLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final WebClient qdrantWebClient;
    private final WebClient ollamaWebClient;
    private final Gson gson = new Gson();
    private final String knowledgeBasePath;
    private final String collectionName;
    private final String embeddingModel;

    public KnowledgeBaseLoader(WebClient qdrantWebClient,
                               WebClient ollamaWebClient,
                               @Value("${app.knowledge-base.path:knowledge-base}") String knowledgeBasePath,
                               @Value("${spring.ai.vectorstore.qdrant.collection-name:returns-docs}") String collectionName,
                               @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}") String embeddingModel) {
        this.qdrantWebClient = qdrantWebClient;
        this.ollamaWebClient = ollamaWebClient;
        this.knowledgeBasePath = knowledgeBasePath;
        this.collectionName = collectionName;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        try {
            Path kbDir = Paths.get(knowledgeBasePath);
            if (!Files.exists(kbDir)) {
                log.warn("Knowledge base directory does not exist: {}. Skipping PDF loading.", kbDir.toAbsolutePath());
                return;
            }

            List<Path> pdfFiles;
            try (Stream<Path> files = Files.list(kbDir)) {
                pdfFiles = files
                        .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                        .collect(Collectors.toList());
            }

            if (pdfFiles.isEmpty()) {
                log.info("No PDF files found in knowledge base directory: {}", kbDir.toAbsolutePath());
                return;
            }

            log.info("Found {} PDF(s) in knowledge base. Starting ingestion...", pdfFiles.size());

            // Ensure collection exists
            ensureCollectionExists();

            // Check if collection already has documents — skip re-indexing if so
            int existingCount = countExistingPoints();
            if (existingCount > 0) {
                log.info("Collection '{}' already has {} points. Skipping re-indexing.", collectionName, existingCount);
                log.info("To force re-index, delete the collection via Qdrant UI (http://localhost:6333/dashboard) or run: curl -X DELETE http://localhost:6333/collections/{}", collectionName);
                return;
            }

            for (Path pdfFile : pdfFiles) {
                try {
                    log.info("Processing PDF: {}", pdfFile.getFileName());
                    String text = extractTextFromPdf(pdfFile);
                    if (text == null || text.isBlank()) {
                        log.warn("No text extracted from {}", pdfFile.getFileName());
                        continue;
                    }

                    String fileName = pdfFile.getFileName().toString();
                    processTextInChunks(text, fileName);

                } catch (Exception e) {
                    log.error("Failed to process PDF {}: {}", pdfFile.getFileName(), e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("Error accessing knowledge base directory: {}", e.getMessage(), e);
        }
    }

    /**
     * Counts existing points in the collection to determine if re-indexing is needed.
     */
    private int countExistingPoints() {
        try {
            String response = qdrantWebClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                if (json.has("result")) {
                    JsonObject result = json.getAsJsonObject("result");
                    if (result.has("points_count")) {
                        return result.get("points_count").getAsInt();
                    }
                    // Qdrant API may return it differently under config
                    if (result.has("config") && result.getAsJsonObject("config").has("params")) {
                        // fallback: try to determine from status
                        return -1; // unknown, proceed
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not check collection point count: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Processes text by splitting into chunks, generating embeddings, and upserting
     * to Qdrant sequentially to avoid OOM from parallel threads.
     */
    private void processTextInChunks(String text, String sourceFileName) {
        int totalChars = text.length();
        int advance = CHUNK_SIZE - CHUNK_OVERLAP;
        int estimatedChunks = Math.max(1, totalChars / advance) + 1;
        log.info("Total text length: {} characters, estimated ~{} chunks", totalChars, estimatedChunks);

        int chunkIndex = 0;
        int successCount = 0;
        int startPos = 0;

        while (startPos < text.length()) {
            int endPos = Math.min(startPos + CHUNK_SIZE, text.length());

            // Try to break at a sentence boundary for cleaner chunks
            if (endPos < text.length()) {
                int breakPoint = text.lastIndexOf(". ", endPos);
                if (breakPoint > startPos) {
                    endPos = breakPoint + 1;
                } else {
                    breakPoint = text.lastIndexOf('\n', endPos);
                    if (breakPoint > startPos) {
                        endPos = breakPoint + 1;
                    }
                }
            }

            String chunkText = text.substring(startPos, endPos).trim();
            if (!chunkText.isEmpty()) {
                try {
                    long startTime = System.currentTimeMillis();
                    float[] embedding = generateEmbedding(chunkText);
                    upsertToQdrant(new DocumentChunk(chunkText, sourceFileName, chunkIndex), embedding);
                    successCount++;
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("Stored chunk {} of ~{} from {} ({} chars, took {}ms)",
                            successCount, estimatedChunks, sourceFileName, chunkText.length(), elapsed);
                } catch (Exception e) {
                    log.error("Failed to process chunk {} from {}: {}", chunkIndex, sourceFileName, e.getMessage());
                }
                chunkIndex++;
            }

            startPos = endPos - CHUNK_OVERLAP;
            if (startPos < 0) startPos = 0;
        }

        log.info("Successfully stored {} chunks from {}", successCount, sourceFileName);
    }

    private String extractTextFromPdf(Path pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
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

    private void ensureCollectionExists() {
        try {
            // Check if collection exists
            String response = qdrantWebClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                if (jsonResponse.has("result") && jsonResponse.get("result").isJsonObject()) {
                    log.info("Collection '{}' already exists", collectionName);
                    return;
                }
            }
        } catch (Exception e) {
            log.info("Collection '{}' does not exist. Creating...", collectionName);
        }

        // Create collection with appropriate vector size for nomic-embed-text (768)
        JsonObject config = new JsonObject();
        config.addProperty("size", 768);
        config.addProperty("distance", "Cosine");

        JsonObject vectors = new JsonObject();
        vectors.add("vectors", config);

        JsonObject requestBody = new JsonObject();
        requestBody.add("vectors", config);

        try {
            qdrantWebClient.put()
                    .uri("/collections/{name}", collectionName)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Created collection '{}'", collectionName);
        } catch (Exception e) {
            log.warn("Could not create collection '{}': {}", collectionName, e.getMessage());
        }
    }

    private void upsertToQdrant(DocumentChunk chunk, float[] embedding) {
        // Build payload
        JsonObject payload = new JsonObject();
        payload.addProperty("text", chunk.text);
        payload.addProperty("source", chunk.source);
        payload.addProperty("chunk", chunk.index);

        // Build vector
        JsonArray vectorArray = new JsonArray();
        for (float v : embedding) {
            vectorArray.add(v);
        }

        // Build point
        JsonObject point = new JsonObject();
        point.addProperty("id", UUID.randomUUID().toString());
        point.add("vector", vectorArray);
        point.add("payload", payload);

        // Build batch
        JsonArray points = new JsonArray();
        points.add(point);

        JsonObject batch = new JsonObject();
        batch.add("points", points);

        // Upsert
        qdrantWebClient.put()
                .uri("/collections/{name}/points", collectionName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(batch.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Internal record for document chunks.
     */
    private static class DocumentChunk {
        final String text;
        final String source;
        final int index;

        DocumentChunk(String text, String source, int index) {
            this.text = text;
            this.source = source;
            this.index = index;
        }
    }
}