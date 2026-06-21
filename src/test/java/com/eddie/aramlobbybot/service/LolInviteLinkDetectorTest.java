package com.eddie.aramlobbybot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LolInviteLinkDetectorTest {

    private final LolInviteLinkDetector detector = new LolInviteLinkDetector();

    @Test
    void detectsRiotJoinLink() {
        assertThat(detector.findFirst("++ https://gg.riotgames.com/LOL?joinCode=abc123_DEF 缺2"))
                .contains("https://gg.riotgames.com/LOL?joinCode=abc123_DEF");
    }

    @Test
    void ignoresMessagesWithoutJoinLink() {
        assertThat(detector.findFirst("缺2 還有缺嗎")).isEmpty();
    }

    @Test
    void stripsTrailingSentencePunctuation() {
        assertThat(detector.findFirst("++ https://gg.riotgames.com/LOL?joinCode=abc123，缺2"))
                .contains("https://gg.riotgames.com/LOL?joinCode=abc123");
    }
}
