package com.localDocGPT;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.localDocGPT.service.DocumentService;
import com.localDocGPT.config.AppConfig;

@SpringBootApplication
public class LocalDocGptApplication implements CommandLineRunner {
	
    private final DocumentService documentService;
    private final AppConfig appConfig;

    public LocalDocGptApplication(DocumentService documentService, AppConfig appConfig) {
        this.documentService = documentService;
        this.appConfig = appConfig;
    }

    public static void main(String[] args) {
        SpringApplication.run(LocalDocGptApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String documentsPath = appConfig.getFolderPath();
        if (documentsPath == null || documentsPath.isEmpty()) {
            System.err.println("❌ Documents path not configured in application.properties");
            return;
        }

        File directory = new File(documentsPath);
        if (!directory.exists() || !directory.isDirectory()) {
            // Try to create the directory for convenience in dev environments
            boolean created = false;
            try {
                created = directory.mkdirs();
            } catch (Exception ex) {
                // ignore here, will handle below
            }

            if (created) {
                System.out.println("ℹ️ Created missing documents directory: " + documentsPath + " — please add .txt or .md files to index.");
                return;
            } else {
                System.err.println("❌ Invalid documents directory and unable to create: " + documentsPath);
                return;
            }
        }

        System.out.println("📂 Processing documents from: " + documentsPath);
        File[] files = directory.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".txt") || 
            name.toLowerCase().endsWith(".md")
        );

        if (files == null || files.length == 0) {
            System.out.println("❌ No text or markdown files found in the directory");
            return;
        }

        for (File doc : files) {
            try {
                documentService.processDocuments(doc);
            } catch (Exception e) {
                System.err.println("❌ Error processing " + doc.getName() + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ Document processing completed!");
    }
}
