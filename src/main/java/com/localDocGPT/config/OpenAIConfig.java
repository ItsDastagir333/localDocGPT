//package com.localDocGPT.config;
//
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class OpenAIConfig {
//
//    private static final Logger log = LoggerFactory.getLogger(OpenAIConfig.class);
//
//    @Value("${openai.api.key}")
//    private String openAiApiKey;
//
//    @Value("${openai.chat.model:gpt-4o-mini}")
//    private String chatModelName;
//
//    @Value("${openai.embedding.model:text-embedding-3-small}")
//    private String embeddingModelName;
//
//    @Bean
//    public EmbeddingModel embeddingModel() {
//        if (openAiApiKey == null || openAiApiKey.isBlank()) {
//            log.warn("OpenAI API key is not set (openai.api.key). Embedding calls will fail unless you set OPENAI_API_KEY.");
//        } else {
//            log.info("OpenAI embedding model configured: {} (key set: {} )", embeddingModelName, maskKey(openAiApiKey));
//        }
//        return OpenAiEmbeddingModel.builder()
//                .apiKey(openAiApiKey)
//                .modelName(embeddingModelName)
//                .build();
//    }
//
//    @Bean
//    public ChatLanguageModel chatModel() {
//        if (openAiApiKey == null || openAiApiKey.isBlank()) {
//            log.warn("OpenAI API key is not set (openai.api.key). Chat calls will fail unless you set OPENAI_API_KEY.");
//        } else {
//            log.info("OpenAI chat model configured: {} (key set: {} )", chatModelName, maskKey(openAiApiKey));
//        }
//        return OpenAiChatModel.builder()
//                .apiKey(openAiApiKey)
//                .modelName(chatModelName)
//                .build();
//    }
//
//    @PostConstruct
//    public void post() {
//        if (openAiApiKey == null || openAiApiKey.isBlank()) {
//            log.warn("OpenAI API key appears empty. Set environment variable OPENAI_API_KEY or property openai.api.key.");
//        }
//    }
//
//    private String maskKey(String key) {
//        if (key == null) return "<null>";
//        if (key.length() <= 8) return "****";
//        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
//    }
//}
