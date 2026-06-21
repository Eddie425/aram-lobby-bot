package com.eddie.aramlobbybot.discord;

import com.eddie.aramlobbybot.domain.Lobby;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DiscordLobbyMessageUpdater {

    private static final Logger log = LoggerFactory.getLogger(DiscordLobbyMessageUpdater.class);

    private final LobbyCardRenderer lobbyCardRenderer;

    public DiscordLobbyMessageUpdater(LobbyCardRenderer lobbyCardRenderer) {
        this.lobbyCardRenderer = lobbyCardRenderer;
    }

    public void updateCard(JDA jda, Lobby lobby) {
        if (!StringUtils.hasText(lobby.getTextChannelId()) || !StringUtils.hasText(lobby.getCardMessageId())) {
            return;
        }
        TextChannel channel = jda.getTextChannelById(lobby.getTextChannelId());
        if (channel == null) {
            log.warn("Cannot update lobby card {}; text channel {} not found", lobby.getLobbyId(), lobby.getTextChannelId());
            return;
        }
        channel.editMessageEmbedsById(lobby.getCardMessageId(), lobbyCardRenderer.renderLobbyCard(lobby))
                .setComponents(lobbyCardRenderer.renderActions(lobby))
                .queue(
                        ignored -> { },
                        ex -> log.warn("Failed to update lobby card {} for lobby {}", lobby.getCardMessageId(), lobby.getLobbyId(), ex)
                );
    }
}
