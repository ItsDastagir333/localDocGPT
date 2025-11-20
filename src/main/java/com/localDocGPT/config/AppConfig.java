package com.localDocGPT.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${localdocs.folder.path}")
    private String folderPath;

    public String getFolderPath() {
        return folderPath;
    }
}

