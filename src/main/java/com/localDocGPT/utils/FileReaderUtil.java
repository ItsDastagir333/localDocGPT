package com.localDocGPT.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileReaderUtil {

    public static String readFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ") // Clean newlines and tabs
                .trim();
    }
}
