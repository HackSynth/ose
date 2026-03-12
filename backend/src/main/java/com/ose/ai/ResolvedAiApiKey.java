package com.ose.ai;

public record ResolvedAiApiKey(
        String id,
        String value,
        String maskedValue
) {
}
