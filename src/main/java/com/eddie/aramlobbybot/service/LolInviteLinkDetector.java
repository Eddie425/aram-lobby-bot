package com.eddie.aramlobbybot.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class LolInviteLinkDetector {

    private static final Pattern JOIN_LINK_PATTERN = Pattern.compile(
            "https://gg\\.riotgames\\.com/LOL\\?joinCode=[A-Za-z0-9_-]+",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<String> findFirst(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = JOIN_LINK_PATTERN.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(stripTrailingPunctuation(matcher.group()));
    }

    private String stripTrailingPunctuation(String link) {
        String sanitized = link;
        while (!sanitized.isEmpty() && isTrailingPunctuation(sanitized.charAt(sanitized.length() - 1))) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private boolean isTrailingPunctuation(char value) {
        return value == '.' || value == ',' || value == '!' || value == '?' || value == '。' || value == '，' || value == '、';
    }
}
