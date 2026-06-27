package com.chatbot.returnchatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for RAG components.
 * Uses manual REST API calls to Qdrant (HTTP) and Ollama since
 * Qdrant at port 6333 exposes HTTP REST API (not gRPC).
 */
@Configuration
public class RagConfig {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6333}")
    private int qdrantPort;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Bean
    public WebClient qdrantWebClient(WebClient.Builder webClientBuilder) {
        String qdrantUrl = "http://" + qdrantHost + ":" + qdrantPort;
        return webClientBuilder.baseUrl(qdrantUrl).build();
    }

    @Bean
    public WebClient ollamaWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(ollamaBaseUrl).build();
    }

    public String getQdrantHost() {
        return qdrantHost;
    }

    public int getQdrantPort() {
        return qdrantPort;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }
}