package com.localDocGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService implements LLMProviderService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model:text-embedding-004}")
    private String embeddingModel;

    @Value("${gemini.chat.model:gemini-1.5-flash}")
    private String chatModel;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";

    /**
     * ---------------------------------------------------------------
     *  TEXT EMBEDDING
     * ---------------------------------------------------------------
     */
    @Override
    public List<Float> embed(String text) throws Exception {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing");
        }

        String url = BASE_URL + embeddingModel + ":embedContent?key=" + apiKey;

        // Build request JSON
        ObjectNode body = mapper.createObjectNode();
        body.put("model", embeddingModel);

        ObjectNode content = body.putObject("content");
        content.putArray("parts").addObject().put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String json = mapper.writeValueAsString(body);

        log.debug("Sending embedding request to {} with payload length {}", url, text.length());

        ResponseEntity<String> response;
        String responseBody = null;
        int status = -1;

        try {
            response = restTemplate.postForEntity(
                    url, new HttpEntity<>(json, headers), String.class);
            status = response.getStatusCodeValue();
            responseBody = response.getBody();
        } catch (HttpStatusCodeException ex) {
            status = ex.getRawStatusCode();
            responseBody = ex.getResponseBodyAsString();
            log.error("Embedding API error ({}): {}", status, responseBody);

            // Fallback to local embeddings
            return com.localDocGPT.utils.LocalEmbeddingUtil.embed(text, 256);
        }

        if (status != 200 || responseBody == null) {
            log.error("Embedding failed with status {} body {}", status, responseBody);
            return com.localDocGPT.utils.LocalEmbeddingUtil.embed(text, 256);
        }

        JsonNode root = mapper.readTree(responseBody);

        JsonNode vec = root.at("/embedding/value");

        if (vec.isMissingNode()) {
            throw new RuntimeException("❌ Embedding vector not found. Response: " + responseBody);
        }

        List<Float> embedding = new ArrayList<>();
        for (JsonNode n : vec) embedding.add((float) n.asDouble());

        return embedding;
    }

    /**
     * ---------------------------------------------------------------
     *  TEXT GENERATION (CHAT)
     * ---------------------------------------------------------------
     */
    @Override
    public String generate(String prompt) throws Exception {

        String url = BASE_URL + chatModel + ":generateContent?key=" + apiKey;

        ObjectNode body = mapper.createObjectNode();
        body.put("model", chatModel);

        ObjectNode message = body.putArray("contents").addObject();
        message.put("role", "user");
        message.putArray("parts").addObject().put("text", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String json = mapper.writeValueAsString(body);

        log.debug("Sending chat request to {} promptSize={}", url, prompt.length());

        ResponseEntity<String> response;
        String responseBody;

        try {
            response = restTemplate.postForEntity(
                    url, new HttpEntity<>(json, headers), String.class);
            responseBody = response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Chat request failed: {}", ex.getResponseBodyAsString());
            throw ex;
        }

        JsonNode root = mapper.readTree(responseBody);

        JsonNode text = root.at("/candidates/0/content/parts/0/text");

        if (text.isMissingNode()) {
            return responseBody;
        }

        return text.asText();
    }
}
