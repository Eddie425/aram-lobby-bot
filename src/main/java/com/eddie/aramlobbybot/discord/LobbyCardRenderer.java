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
                .setTitle("⚔️ ARAM Lobby | " + safe(lobby.getOwnerDisplayName()))
                .setColor(colorFor(lobby.getStatus()))
                .addField("狀態", statusText(lobby), true)
                .addField("戰力槽", progressText(lobby), true)
                .addField("缺人", missingText(lobby), true)
                .addField("語音房", "🎧 `" + safe(lobby.getVoiceChannelName()) + "`", true)
                .addField("語音人數", "👥 **" + lobby.getVoiceMemberCount() + "**", true)
                .addField("開團時間", "🕘 `" + TIME_FORMATTER.format(lobby.getCreatedAt()) + "`", true)
                .build();
    }

    public List<ActionRow> renderActions(Lobby lobby) {
        boolean closed = lobby.getStatus() == LobbyStatus.CLOSED;
        return List.of(ActionRow.of(
                Button.link(lobby.getRiotJoinLink(), closed ? "🔒 LoL Closed" : "🎮 Join LoL").withDisabled(closed),
                Button.link(lobby.getVoiceInviteLink(), closed ? "🔒 Voice Closed" : "🎤 Join Voice").withDisabled(closed)
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
                    "Room " + index++ + " | " + safe(lobby.getOwnerDisplayName()),
                    progressText(lobby) + "\n🎧 `" + safe(lobby.getVoiceChannelName()) + "`，缺 **" + lobby.missingCount() + "** 人",
                    false
            );
        }
        return embedBuilder.build();
    }

    private String statusText(Lobby lobby) {
        return switch (lobby.getStatus()) {
            case OPEN -> "🟢 **OPEN**";
            case FULL -> "🎉 **FULL**";
            case CLOSED -> "🔴 **CLOSED**";
        };
    }

    private Color colorFor(LobbyStatus status) {
        return switch (status) {
            case OPEN -> new Color(0x2F80ED);
            case FULL -> new Color(0x27AE60);
            case CLOSED -> new Color(0xD72638);
        };
    }

    private String progressText(Lobby lobby) {
        return "`" + "■".repeat(lobby.playerCount()) + "□".repeat(lobby.missingCount()) + "` **"
                + lobby.playerCount() + " / " + Lobby.MAX_PLAYERS + "**";
    }

    private String missingText(Lobby lobby) {
        if (lobby.getStatus() == LobbyStatus.CLOSED) {
            return "🔴 **已關閉**";
        }
        if (lobby.missingCount() == 0) {
            return "✅ **滿團**";
        }
        return "⚡ **" + lobby.missingCount() + "** slots";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
