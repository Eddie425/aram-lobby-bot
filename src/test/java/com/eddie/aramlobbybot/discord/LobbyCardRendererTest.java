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
    void rendersOnlyLolAndVoiceLinkButtons() {
        Lobby lobby = lobby(LobbyStatus.OPEN, "owner", "u1");

        List<ActionRow> rows = renderer.renderActions(lobby);

        assertThat(rows.get(0).getComponents()).hasSize(2);
        assertThat(((Button) rows.get(0).getComponents().get(0)).getLabel()).isEqualTo("🎮 Join LoL");
        assertThat(((Button) rows.get(0).getComponents().get(1)).getLabel()).isEqualTo("🎤 Join Voice");
    }

    @Test
    void disablesLinkButtonsWhenLobbyIsClosed() {
        Lobby lobby = lobby(LobbyStatus.CLOSED, "owner");

        List<ActionRow> rows = renderer.renderActions(lobby);
        Button lolButton = (Button) rows.get(0).getComponents().get(0);
        Button voiceButton = (Button) rows.get(0).getComponents().get(1);

        assertThat(lolButton.isDisabled()).isTrue();
        assertThat(voiceButton.isDisabled()).isTrue();
        assertThat(lolButton.getLabel()).isEqualTo("🔒 LoL Closed");
        assertThat(voiceButton.getLabel()).isEqualTo("🔒 Voice Closed");
    }

    @Test
    void rendersOpenLobbyCounts() {
        Lobby lobby = lobby(LobbyStatus.OPEN, "owner", "u1");
        lobby.setVoiceMemberCount(2);

        assertThat(renderer.renderLobbyCard(lobby).getTitle()).isEqualTo("⚔️ ARAM Lobby | Eddie");
        assertThat(renderer.renderLobbyCard(lobby).getFields())
                .anySatisfy(field -> {
                    assertThat(field.getName()).isEqualTo("狀態");
                    assertThat(field.getValue()).isEqualTo("🟢 **OPEN**");
                })
                .anySatisfy(field -> {
                    assertThat(field.getName()).isEqualTo("戰力槽");
                    assertThat(field.getValue()).isEqualTo("`■■□□□` **2 / 5**");
                })
                .anySatisfy(field -> {
                    assertThat(field.getName()).isEqualTo("缺人");
                    assertThat(field.getValue()).isEqualTo("⚡ **3** slots");
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
