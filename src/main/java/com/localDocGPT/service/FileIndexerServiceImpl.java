package com.localDocGPT.service;

import com.localDocGPT.model.EmbeddingEntity;
import com.localDocGPT.repository.EmbeddingRepository;
import com.localDocGPT.utils.FileReaderUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class FileIndexerServiceImpl implements FileIndexerService {

    private final EmbeddingRepository repository;
    private final EmbeddingModel embeddingModel;
    private final org.springframework.core.env.Environment environment;

    public FileIndexerServiceImpl(EmbeddingRepository repository, EmbeddingModel embeddingModel, org.springframework.core.env.Environment environment) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
        this.environment = environment;
    }

    @Override
    public void indexFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid folder path: " + folderPath);
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt") || name.endsWith(".md"));
        if (files == null || files.length == 0) {
            System.out.println("No text or markdown files found in: " + folderPath);
            return;
        }

        for (File file : files) {
            String content = FileReaderUtil.readFile(file);

            // Simple chunking for large files
            int chunkSize = 1000; // characters per chunk
            for (int start = 0; start < content.length(); start += chunkSize) {
                int end = Math.min(start + chunkSize, content.length());
                String chunk = content.substring(start, end);

                try {
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
                    String embeddingStr;
                    if (useDev) {
                        java.util.List<Float> vec = com.localDocGPT.utils.LocalEmbeddingUtil.embed(chunk, 256);
                        embeddingStr = vec.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","));
                    } else {
                        dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(chunk).content();
                        if (embedding == null || embedding.vectorAsList() == null) {
                            System.err.println("Warning: received empty embedding for chunk in file " + file.getName());
                            continue;
                        }
                        embeddingStr = embedding.vectorAsList().stream()
                                .map(Object::toString)
                                .collect(java.util.stream.Collectors.joining(","));
                    }

                    EmbeddingEntity entity = new EmbeddingEntity();
                    entity.setContentSnippet(chunk);
                    entity.setEmbeddingVector(embeddingStr);
                    entity.setFileName(file.getName() + "_part_" + (start / chunkSize + 1));
                    repository.save(entity);
                } catch (Exception e) {
                    System.err.println("❌ Error embedding chunk for file " + file.getName() + ": " + e.getMessage());
                    if (e.getClass().getName().contains("OpenAiHttpException") ||
                            (e.getCause() != null && e.getCause().getClass().getName().contains("OpenAiHttpException"))) {
                        System.err.println("   ↳ OpenAI API error detected during chunk embedding. Check OPENAI_API_KEY or quota.");
                    }
                    // continue with next chunk/file
                    continue;
                }
            }

            System.out.println("✅ Indexed: " + file.getName());
        }

        System.out.println("✅ Folder indexing completed!");
    }
}
