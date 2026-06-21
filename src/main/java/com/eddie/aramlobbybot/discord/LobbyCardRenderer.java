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
                .addField("Ready Check", readyText(lobby), true)
                .addField("候補", waitlistText(lobby), true)
                .build();
    }

    public List<ActionRow> renderActions(Lobby lobby) {
        boolean closed = lobby.getStatus() == LobbyStatus.CLOSED;
        List<Button> primaryActions = List.of(
                Button.link(lobby.getRiotJoinLink(), closed ? "🔒 LoL Closed" : "🎮 Join LoL").withDisabled(closed),
                Button.link(lobby.getVoiceInviteLink(), closed ? "🔒 Voice Closed" : "🎤 Join Voice").withDisabled(closed)
        );
        if (closed) {
            return List.of(ActionRow.of(primaryActions));
        }
        if (lobby.getStatus() == LobbyStatus.FULL) {
            return List.of(
                    ActionRow.of(primaryActions),
                    ActionRow.of(
                            Button.success(LobbyButtonIds.ready(lobby.getLobbyId()), "✅ Ready"),
                            Button.secondary(LobbyButtonIds.notReady(lobby.getLobbyId()), "⏳ Not Ready"),
                            Button.primary(LobbyButtonIds.waitlist(lobby.getLobbyId()), "📋 排候補"),
                            Button.secondary(LobbyButtonIds.leaveWaitlist(lobby.getLobbyId()), "🚪 離開候補")
                    )
            );
        }
        return List.of(ActionRow.of(primaryActions));
    }

    public MessageEmbed renderLobbyList(List<Lobby> lobbies) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("🎮 Available ARAM Lobbies")
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

    public MessageEmbed renderBotStatus(boolean detectionEnabled, List<Lobby> openLobbies, List<Lobby> activeLobbies) {
        return new EmbedBuilder()
                .setTitle("⚙️ ARAM Bot Status")
                .setColor(detectionEnabled ? new Color(0x27AE60) : new Color(0xD72638))
                .addField("自動偵測", detectionEnabled ? "🟢 **ON**" : "🔴 **OFF**", true)
                .addField("可加入 Lobby", String.valueOf(openLobbies.size()), true)
                .addField("Active Lobby", String.valueOf(activeLobbies.size()), true)
                .build();
    }

    public MessageEmbed renderCommandHelp() {
        return new EmbedBuilder()
                .setTitle("🕹️ ARAM Bot Commands")
                .setColor(new Color(0x2F80ED))
                .setDescription("""
                        `/aram list` - 看目前哪些 ARAM Lobby 還缺人
                        `/aram available` - 同 list，列出可加入的 Lobby
                        `/aram status` - 看這個頻道是否有開自動偵測
                        `/aram disable` - 關閉這個頻道的 LoL link 自動偵測
                        `/aram enable` - 開啟這個頻道的 LoL link 自動偵測
                        `/aram notify-on` - 訂閱缺 1-2 的上車通知
                        `/aram notify-off` - 取消缺人通知
                        `/aram notify-status` - 查看你的通知訂閱狀態
                        `/aram close` - 房主手動關閉自己的最新 Lobby
                        `/aram help` - 顯示這份指令清單
                        """)
                .build();
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

    private String readyText(Lobby lobby) {
        if (lobby.getStatus() != LobbyStatus.FULL) {
            return "等待滿團";
        }
        if (lobby.isReadyComplete()) {
            return "✅ **全員 Ready**";
        }
        return "⏳ **" + lobby.readyCount() + " / " + Lobby.MAX_PLAYERS + "**";
    }

    private String waitlistText(Lobby lobby) {
        if (lobby.getWaitlistUserIds().isEmpty()) {
            return "無";
        }
        return "📋 **" + lobby.getWaitlistUserIds().size() + "** 人";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
