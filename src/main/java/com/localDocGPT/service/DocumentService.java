package com.localDocGPT.service;

import com.localDocGPT.model.EmbeddingEntity;
import com.localDocGPT.repository.EmbeddingRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.core.env.Environment;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;

@Service
public class DocumentService {

    private final EmbeddingRepository repository;

    private final EmbeddingModel embeddingModel;

    private final Environment environment;

    public DocumentService(EmbeddingRepository repository, EmbeddingModel embeddingModel, Environment environment) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
        this.environment = environment;
    }

    public void processDocuments(File file) {
        try {
            // Check if file is already processed
            if (repository.existsByFileName(file.getName())) {
                System.out.println("⏭️ File already processed: " + file.getName());
                return;
            }

            // Step 1: Read content from file
            String content = Files.readString(file.toPath());

            // Step 2: Generate embedding
            String embeddingStr;
            boolean useDev = false;
            if (environment != null) {
                // explicit dev profile
                useDev = environment.acceptsProfiles("dev");
                // explicit property to force local embeddings
                String localEmbProp = environment.getProperty("local.embeddings.enabled");
                if (localEmbProp != null && localEmbProp.equalsIgnoreCase("true")) {
                    useDev = true;
                }
                // fallback to local embeddings when no OpenAI key is configured
                String propKey = environment.getProperty("openai.api.key");
                String envKey = System.getenv("OPENAI_API_KEY");
                if ((propKey == null || propKey.isBlank()) && (envKey == null || envKey.isBlank())) {
                    useDev = true;
                }
            }
            if (useDev) {
                // deterministic local embedding for dev
                java.util.List<Float> vec = com.localDocGPT.utils.LocalEmbeddingUtil.embed(content, 256);
                embeddingStr = vec.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","));
            } else {
                Response<Embedding> embeddingResponse = embeddingModel.embed(content);
                Embedding embedding = embeddingResponse == null ? null : embeddingResponse.content();
                if (embedding == null || embedding.vectorAsList() == null) {
                    throw new IllegalStateException("Received empty embedding for file: " + file.getName());
                }
                // convert list of floats to comma-separated string (no brackets)
                embeddingStr = embedding.vectorAsList().stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(","));
            }

            // Step 3: Save embedding into DB
            EmbeddingEntity entity = new EmbeddingEntity();
            entity.setFileName(file.getName());
            entity.setContentSnippet(content.length() > 300 ? content.substring(0, 300) : content);
            entity.setEmbeddingVector(embeddingStr);

            repository.save(entity);
            System.out.println("✅ Embedded and saved: " + file.getName());

        } catch (Exception e) {
            // Don't fail application startup because of a single-file embedding failure.
            // Log helpful guidance when it's an OpenAI API error (quota, invalid key, etc.).
            String msg = e.getMessage();
            System.err.println("❌ Error processing " + file.getName() + ": " + msg);

            // Detect common OpenAI client exception class by name to avoid compile coupling
            if (e.getClass().getName().contains("OpenAiHttpException") ||
                    (e.getCause() != null && e.getCause().getClass().getName().contains("OpenAiHttpException"))) {
                System.err.println("   ↳ OpenAI API error detected. Possible causes:");
                System.err.println("     - Missing/invalid OPENAI_API_KEY environment variable");
                System.err.println("     - Insufficient quota or billing issue on your OpenAI account");
                System.err.println("   Remediation: set a valid API key (env var OPENAI_API_KEY) or check your OpenAI plan/billing.");
            }

            // Skip this file and continue with next files instead of failing the whole app.
            return;
        }
    }
}
