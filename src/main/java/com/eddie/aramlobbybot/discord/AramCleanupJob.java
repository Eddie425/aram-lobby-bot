package com.eddie.aramlobbybot.discord;

import com.eddie.aramlobbybot.config.AramProperties;
import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.service.LobbyService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AramCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AramCleanupJob.class);

    private final DiscordBotManager discordBotManager;
    private final LobbyService lobbyService;
    private final DiscordLobbyMessageUpdater messageUpdater;
    private final AramProperties aramProperties;

    public AramCleanupJob(
            DiscordBotManager discordBotManager,
            LobbyService lobbyService,
            DiscordLobbyMessageUpdater messageUpdater,
            AramProperties aramProperties
    ) {
        this.discordBotManager = discordBotManager;
        this.lobbyService = lobbyService;
        this.messageUpdater = messageUpdater;
        this.aramProperties = aramProperties;
    }

    @Scheduled(fixedRateString = "#{@aramProperties.cleanup().fixedRate().toMillis()}")
    public void cleanupEmptyVoiceRooms() {
        discordBotManager.getJda().ifPresent(this::cleanupEmptyVoiceRooms);
    }

    private void cleanupEmptyVoiceRooms(JDA jda) {
        for (Lobby lobby : lobbyService.findActiveLobbies()) {
            VoiceChannel voiceChannel = jda.getVoiceChannelById(lobby.getVoiceChannelId());
            if (voiceChannel == null) {
                Lobby closed = lobbyService.markClosed(lobby.getLobbyId());
                messageUpdater.updateCard(jda, closed);
                lobbyService.deleteLobby(lobby.getLobbyId());
                continue;
            }

            Lobby updated = lobbyService.updateVoicePresence(lobby.getLobbyId(), voiceChannel.getMembers().size());
            if (!lobbyService.shouldCleanup(updated, aramProperties.cleanup().emptyGrace())) {
                continue;
            }

            voiceChannel.delete().queue(
                    ignored -> {
                        Lobby closed = lobbyService.markClosed(updated.getLobbyId());
                        messageUpdater.updateCard(jda, closed);
                        lobbyService.deleteLobby(updated.getLobbyId());
                        log.info("Cleaned up empty ARAM voice channel {} for lobby {}", updated.getVoiceChannelId(), updated.getLobbyId());
                    },
                    ex -> log.warn("Failed to cleanup voice channel {} for lobby {}", updated.getVoiceChannelId(), updated.getLobbyId(), ex)
            );
        }
    }
}
