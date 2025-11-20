package com.localDocGPT.controller;

import com.localDocGPT.service.EmbeddingService;
import com.localDocGPT.service.FileIndexerService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AskController {

    private final EmbeddingService embeddingService;
    private final FileIndexerService fileIndexerService;

    public AskController(EmbeddingService embeddingService, FileIndexerService fileIndexerService) {
        this.embeddingService = embeddingService;
		this.fileIndexerService = fileIndexerService;
    }

    @GetMapping("/ask")
    public String askQuestion(@RequestParam String question) {
        return embeddingService.ask(question);
    }
    
    @PostMapping("/index")
    public String indexFolder(@RequestParam String folderPath) {
        try {
            fileIndexerService.indexFolder(folderPath);
            return "Folder indexed successfully.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

}
