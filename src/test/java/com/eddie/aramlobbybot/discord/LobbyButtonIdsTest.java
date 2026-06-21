package com.eddie.aramlobbybot.discord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LobbyButtonIdsTest {

    @Test
    void extractsLobbyIdFromButtonComponentId() {
        String componentId = LobbyButtonIds.join("lobby-1");

        assertThat(LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.JOIN_PREFIX)).isEqualTo("lobby-1");
    }

    @Test
    void extractsLobbyIdFromReadyAndWaitlistButtonComponentIds() {
        assertThat(LobbyButtonIds.extractLobbyId(LobbyButtonIds.ready("lobby-1"), LobbyButtonIds.READY_PREFIX))
                .isEqualTo("lobby-1");
        assertThat(LobbyButtonIds.extractLobbyId(LobbyButtonIds.notReady("lobby-1"), LobbyButtonIds.NOT_READY_PREFIX))
                .isEqualTo("lobby-1");
        assertThat(LobbyButtonIds.extractLobbyId(LobbyButtonIds.waitlist("lobby-1"), LobbyButtonIds.WAITLIST_PREFIX))
                .isEqualTo("lobby-1");
        assertThat(LobbyButtonIds.extractLobbyId(LobbyButtonIds.leaveWaitlist("lobby-1"), LobbyButtonIds.WAITLIST_LEAVE_PREFIX))
                .isEqualTo("lobby-1");
    }
}
