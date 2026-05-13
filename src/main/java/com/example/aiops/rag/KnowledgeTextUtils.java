package com.example.aiops.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class KnowledgeTextUtils {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,，。；;、|/\\\\()（）\\[\\]{}<>《》:：!?！？]+");

    private KnowledgeTextUtils() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public static List<String> normalizeCodes(List<String> codes) {
        Set<String> result = new LinkedHashSet<>();
        result.add(KnowledgeSearchCriteria.PUBLIC_PERMISSION);
        if (codes != null) {
            for (String code : codes) {
                if (code != null && !code.isBlank()) {
                    result.add(code.trim());
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static String joinCodes(List<String> codes) {
        return String.join(",", normalizeCodes(codes));
    }

    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed.contains(" ") ? trimmed.replace(" ", "T") : trimmed);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
    }

    public static List<String> tokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SPLIT.split(query)) {
            if (token != null) {
                String normalized = token.trim().toLowerCase();
                if (normalized.length() >= 2) {
                    tokens.add(normalized);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }
}
