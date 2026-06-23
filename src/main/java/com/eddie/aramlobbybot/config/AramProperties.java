package com.eddie.aramlobbybot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "aram")
public record AramProperties(Lobby lobby, Cleanup cleanup, Detection detection) {

    @ConstructorBinding
    public AramProperties {
        if (detection == null) {
            detection = new Detection("/al");
        }
    }

    public AramProperties(Lobby lobby, Cleanup cleanup) {
        this(lobby, cleanup, new Detection("/al"));
    }

    public record Lobby(Duration ttl) {
    }

    public record Cleanup(Duration emptyGrace, Duration fixedRate) {
    }

    public record Detection(String triggerPrefix) {

        public Detection {
            if (!StringUtils.hasText(triggerPrefix)) {
                triggerPrefix = "/al";
            }
        }
    }
}
