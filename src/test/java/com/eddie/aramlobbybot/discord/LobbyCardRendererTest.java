package com.eddie.aramlobbybot.discord;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.domain.LobbyStatus;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.junit.jupiter.api.Test;

class LobbyCardRendererTest {

    private final LobbyCardRenderer renderer = new LobbyCardRenderer();

    @Test
    void disablesJoinLobbyButtonWhenLobbyIsFull() {
        Lobby lobby = lobby(LobbyStatus.FULL, "owner", "u1", "u2", "u3", "u4");

        List<ActionRow> rows = renderer.renderActions(lobby);
        Button joinLobbyButton = (Button) rows.get(0).getComponents().get(2);

        assertThat(joinLobbyButton.isDisabled()).isTrue();
    }

    @Test
    void rendersOpenLobbyCounts() {
        Lobby lobby = lobby(LobbyStatus.OPEN, "owner", "u1");

        assertThat(renderer.renderLobbyCard(lobby).getFields())
                .anySatisfy(field -> {
                    assertThat(field.getName()).isEqualTo("狀態");
                    assertThat(field.getValue()).isEqualTo("2 / 5");
                })
                .anySatisfy(field -> {
                    assertThat(field.getName()).isEqualTo("缺");
                    assertThat(field.getValue()).isEqualTo("3 人");
                });
    }

    private Lobby lobby(LobbyStatus status, String... users) {
        Lobby lobby = new Lobby();
        lobby.setLobbyId("lobby-1");
        lobby.setOwnerUserId("owner");
        lobby.setOwnerDisplayName("Eddie");
        lobby.setRiotJoinLink("https://gg.riotgames.com/LOL?joinCode=abc");
        lobby.setVoiceInviteLink("https://discord.gg/abc");
        lobby.setVoiceChannelName("ARAM-Eddie");
        lobby.setJoinedUsers(new LinkedHashSet<>(List.of(users)));
        lobby.setVoiceMemberCount(0);
        lobby.setStatus(status);
        lobby.setCreatedAt(Instant.parse("2026-06-21T09:00:00Z"));
        return lobby;
    }
}
