package com.localDocGPT.controller;

import com.localDocGPT.service.EmbeddingService;
import com.localDocGPT.service.FileIndexerService;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AskController {

    private final EmbeddingService embeddingService;
    private final FileIndexerService fileIndexerService;

    public AskController(EmbeddingService embeddingService, FileIndexerService fileIndexerService) {
        this.embeddingService = embeddingService;
        this.fileIndexerService = fileIndexerService;
    }

    /**
     * Ask a question. Accepts JSON body: { "question": "..." }
     * Example:
     * POST /api/ask
     * { "question": "What is in the docs?" }
     */
    @PostMapping("/ask")
    public String askQuestion(@RequestBody Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        if (question == null || question.isBlank()) {
            return "Error: missing question in request body.";
        }
        return embeddingService.ask(question);
    }

    /**
     * Index a folder. Accepts JSON body: { "folderPath": "docs/" }
     * Example:
     * POST /api/index
     * { "folderPath": "docs/" }
     */
    @PostMapping("/index")
    public String indexFolder(@RequestBody Map<String, String> body) {
        String folderPath = body == null ? null : body.get("folderPath");
        if (folderPath == null || folderPath.isBlank()) {
            return "Error: missing folderPath in request body.";
        }
        try {
            fileIndexerService.indexFolder(folderPath);
            return "Folder indexed successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

}
