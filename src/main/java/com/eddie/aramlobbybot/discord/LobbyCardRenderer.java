package com.eddie.aramlobbybot.discord;

import java.awt.Color;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.domain.LobbyStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

@Component
public class LobbyCardRenderer {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(DISPLAY_ZONE);

    public MessageEmbed renderLobbyCard(Lobby lobby) {
        return new EmbedBuilder()
                .setTitle("🎮 ARAM Lobby - " + safe(lobby.getOwnerDisplayName()))
                .setColor(colorFor(lobby.getStatus()))
                .addField("房主", safe(lobby.getOwnerDisplayName()), true)
                .addField("狀態", statusText(lobby), true)
                .addField("缺", lobby.missingCount() + " 人", true)
                .addField("語音房", safe(lobby.getVoiceChannelName()), true)
                .addField("語音人數", String.valueOf(lobby.getVoiceMemberCount()), true)
                .addField("建立時間", TIME_FORMATTER.format(lobby.getCreatedAt()), true)
                .build();
    }

    public List<ActionRow> renderActions(Lobby lobby) {
        boolean closed = lobby.getStatus() == LobbyStatus.CLOSED;
        boolean full = lobby.getStatus() == LobbyStatus.FULL;
        return List.of(ActionRow.of(
                Button.link(lobby.getRiotJoinLink(), "🎮 Join LoL"),
                Button.link(lobby.getVoiceInviteLink(), "🎤 Join Voice"),
                Button.primary(LobbyButtonIds.join(lobby.getLobbyId()), "✅ Join Lobby").withDisabled(closed || full),
                Button.danger(LobbyButtonIds.leave(lobby.getLobbyId()), "❌ Leave Lobby").withDisabled(closed)
        ));
    }

    public MessageEmbed renderLobbyList(List<Lobby> lobbies) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("🎮 Active Lobbies")
                .setColor(new Color(0x2F80ED));
        if (lobbies.isEmpty()) {
            embedBuilder.setDescription("目前沒有缺人的 ARAM Lobby。");
            return embedBuilder.build();
        }
        int index = 1;
        for (Lobby lobby : lobbies) {
            embedBuilder.addField(
                    "Room " + index++ + " - " + safe(lobby.getVoiceChannelName()),
                    lobby.playerCount() + " / " + Lobby.MAX_PLAYERS + "，缺 " + lobby.missingCount() + " 人",
                    false
            );
        }
        return embedBuilder.build();
    }

    private String statusText(Lobby lobby) {
        return switch (lobby.getStatus()) {
            case OPEN -> lobby.playerCount() + " / " + Lobby.MAX_PLAYERS;
            case FULL -> "🎉 Full";
            case CLOSED -> "Closed";
        };
    }

    private Color colorFor(LobbyStatus status) {
        return switch (status) {
            case OPEN -> new Color(0x2F80ED);
            case FULL -> new Color(0x27AE60);
            case CLOSED -> new Color(0x828282);
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
