package com.localDocGPT.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class LocalEmbeddingUtil {

    // Deterministic embedding generator for dev/testing. Not semantically accurate but stable.
    public static List<Float> embed(String text, int dimension) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<Float> vec = new ArrayList<>(dimension);
            for (int i = 0; i < dimension; i++) {
                String input = text + ":" + i;
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
                // take first 4 bytes as int
                int val = ByteBuffer.wrap(hash, 0, 4).getInt();
                // map to float between -1 and 1
                float f = val / (float) Integer.MAX_VALUE;
                vec.add(f);
            }
            // normalize
            double norm = 0.0;
            for (Float v : vec) norm += v * v;
            norm = Math.sqrt(norm);
            if (norm == 0) return vec;
            for (int i = 0; i < vec.size(); i++) vec.set(i, (float) (vec.get(i) / norm));
            return vec;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
