package com.localDocGPT.service;

import java.util.List;

public interface LLMProviderService {
    List<Float> embed(String text) throws Exception;
    String generate(String prompt) throws Exception;
}
