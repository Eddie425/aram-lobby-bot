package com.eddie.aramlobbybot.discord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LobbyButtonIdsTest {

    @Test
    void extractsLobbyIdFromButtonComponentId() {
        String componentId = LobbyButtonIds.join("lobby-1");

        assertThat(LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.JOIN_PREFIX)).isEqualTo("lobby-1");
    }
}
