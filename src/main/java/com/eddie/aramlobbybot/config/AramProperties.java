package com.eddie.aramlobbybot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aram")
public record AramProperties(Lobby lobby, Cleanup cleanup) {

    public record Lobby(Duration ttl) {
    }

    public record Cleanup(Duration emptyGrace, Duration fixedRate) {
    }
}
