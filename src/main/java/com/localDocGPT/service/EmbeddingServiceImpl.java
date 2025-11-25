package com.localDocGPT.service;

import com.localDocGPT.model.EmbeddingEntity;
import com.localDocGPT.repository.EmbeddingRepository;
import com.localDocGPT.utils.SimilarityUtil;
import com.localDocGPT.service.LLMProviderService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingRepository repository;
    private final LLMProviderService llmProvider;

    public EmbeddingServiceImpl(EmbeddingRepository repository,
                                LLMProviderService llmProvider) {
        this.repository = repository;
        this.llmProvider = llmProvider;
    }

    @Override
    public String ask(String question) {
        List<Float> questionVector;
        try {
            questionVector = llmProvider.embed(question);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while creating embedding: " + e.getMessage();
        }

        List<EmbeddingEntity> allDocs = repository.findAll();

        if (allDocs.isEmpty()) {
            return "No documents have been indexed yet. Please upload or index files first.";
        }

        List<EmbeddingEntity> topMatches = allDocs.stream()
                .sorted(Comparator.comparingDouble(doc ->
                        -SimilarityUtil.cosineSimilarity(
                                questionVector,
                                SimilarityUtil.parseEmbedding(doc.getEmbeddingVector())
                        )))
                .limit(3)
                .collect(Collectors.toList());

        String context = topMatches.stream()
                .map(EmbeddingEntity::getContentSnippet)
                .collect(Collectors.joining("\n---\n"));

        String prompt = """
                You are a helpful assistant trained to answer based on document context.
                Use the context below to answer the question truthfully and clearly.

                Context:
                %s

                Question: %s

                Answer:
                """.formatted(context, question);

        try {
            return llmProvider.generate(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while generating the answer: " + e.getMessage();
        }
    }
}
