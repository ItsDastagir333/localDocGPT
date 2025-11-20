package com.localDocGPT.service;

import com.localDocGPT.model.EmbeddingEntity;
import com.localDocGPT.repository.EmbeddingRepository;
import com.localDocGPT.utils.SimilarityUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingRepository repository;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;

    public EmbeddingServiceImpl(EmbeddingRepository repository,
                                EmbeddingModel embeddingModel,
                                ChatLanguageModel chatModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    @Override
    public String ask(String question) {
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        List<Float> questionVector = questionEmbedding.vectorAsList();

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
            return chatModel.generate(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while generating the answer: " + e.getMessage();
        }
    }
}
