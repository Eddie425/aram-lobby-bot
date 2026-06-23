package com.eddie.aramlobbybot.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eddie.aramlobbybot.config.AramProperties;
import com.eddie.aramlobbybot.repository.DetectionMode;
import com.eddie.aramlobbybot.repository.DetectionSettingsRepository;
import com.eddie.aramlobbybot.repository.NotificationSubscriptionRepository;
import com.eddie.aramlobbybot.service.LobbyService;
import com.eddie.aramlobbybot.service.LolInviteLinkDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AramDiscordListenerTest {

    private DetectionSettingsRepository detectionSettingsRepository;
    private AramDiscordListener listener;

    @BeforeEach
    void setUp() {
        detectionSettingsRepository = mock(DetectionSettingsRepository.class);
        listener = new AramDiscordListener(
                mock(LolInviteLinkDetector.class),
                mock(LobbyService.class),
                mock(LobbyCardRenderer.class),
                mock(DiscordLobbyMessageUpdater.class),
                mock(DiscordVoiceRoomFactory.class),
                detectionSettingsRepository,
                mock(NotificationSubscriptionRepository.class),
                new AramProperties(null, null, new AramProperties.Detection("/al"))
        );
    }

    @Test
    void prefixModeRequiresConfiguredTriggerPrefix() {
        when(detectionSettingsRepository.isDetectionEnabled("guild-1", "channel-1")).thenReturn(true);
        when(detectionSettingsRepository.findDetectionMode("guild-1", "channel-1")).thenReturn(DetectionMode.PREFIX);

        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "https://gg.riotgames.com/LOL?joinCode=abc"))
                .isFalse();
        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "/alive https://gg.riotgames.com/LOL?joinCode=abc"))
                .isFalse();
        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "/al https://gg.riotgames.com/LOL?joinCode=abc"))
                .isTrue();
        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "  /al https://gg.riotgames.com/LOL?joinCode=abc"))
                .isTrue();
    }

    @Test
    void autoModeAcceptsInviteLinksWithoutPrefix() {
        when(detectionSettingsRepository.isDetectionEnabled("guild-1", "channel-1")).thenReturn(true);
        when(detectionSettingsRepository.findDetectionMode("guild-1", "channel-1")).thenReturn(DetectionMode.AUTO);

        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "https://gg.riotgames.com/LOL?joinCode=abc"))
                .isTrue();
    }

    @Test
    void disabledDetectionRejectsAllMessages() {
        when(detectionSettingsRepository.isDetectionEnabled("guild-1", "channel-1")).thenReturn(false);

        assertThat(listener.shouldDetectInviteLink("guild-1", "channel-1", "/al https://gg.riotgames.com/LOL?joinCode=abc"))
                .isFalse();
    }
}
