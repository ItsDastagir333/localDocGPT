package com.localDocGPT.service;

import java.io.IOException;

public interface FileIndexerService {
    void indexFolder(String folderPath) throws IOException;
}
