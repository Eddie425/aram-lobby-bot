package com.eddie.aramlobbybot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(String botToken, String voiceCategoryName) {
}
