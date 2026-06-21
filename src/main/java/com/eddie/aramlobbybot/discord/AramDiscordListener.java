package com.eddie.aramlobbybot.discord;

import java.util.ArrayList;
import java.util.List;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.service.LobbyService;
import com.eddie.aramlobbybot.service.LobbyService.CreateLobbyCommand;
import com.eddie.aramlobbybot.service.LolInviteLinkDetector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AramDiscordListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AramDiscordListener.class);

    private final LolInviteLinkDetector linkDetector;
    private final LobbyService lobbyService;
    private final LobbyCardRenderer lobbyCardRenderer;
    private final DiscordLobbyMessageUpdater messageUpdater;
    private final DiscordVoiceRoomFactory voiceRoomFactory;

    public AramDiscordListener(
            LolInviteLinkDetector linkDetector,
            LobbyService lobbyService,
            LobbyCardRenderer lobbyCardRenderer,
            DiscordLobbyMessageUpdater messageUpdater,
            DiscordVoiceRoomFactory voiceRoomFactory
    ) {
        this.linkDetector = linkDetector;
        this.lobbyService = lobbyService;
        this.lobbyCardRenderer = lobbyCardRenderer;
        this.messageUpdater = messageUpdater;
        this.voiceRoomFactory = voiceRoomFactory;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        linkDetector.findFirst(event.getMessage().getContentRaw())
                .ifPresent(link -> createLobbyFromMessage(event, link));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        try {
            if (componentId.startsWith(LobbyButtonIds.JOIN_PREFIX)) {
                Lobby lobby = lobbyService.joinLobby(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.JOIN_PREFIX),
                        event.getUser().getId()
                );
                event.editMessageEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .queue();
            } else if (componentId.startsWith(LobbyButtonIds.LEAVE_PREFIX)) {
                Lobby lobby = lobbyService.leaveLobby(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.LEAVE_PREFIX),
                        event.getUser().getId()
                );
                event.editMessageEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .queue();
            }
        } catch (IllegalArgumentException ex) {
            event.reply("找不到這個 Lobby，可能已經被清理。").setEphemeral(true).queue();
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        List<AudioChannel> changedChannels = new ArrayList<>();
        if (event.getChannelJoined() != null) {
            changedChannels.add(event.getChannelJoined());
        }
        if (event.getChannelLeft() != null) {
            changedChannels.add(event.getChannelLeft());
        }
        if (changedChannels.isEmpty()) {
            return;
        }

        for (AudioChannel channel : changedChannels) {
            lobbyService.findActiveLobbies().stream()
                    .filter(lobby -> channel.getId().equals(lobby.getVoiceChannelId()))
                    .findFirst()
                    .ifPresent(lobby -> {
                        Lobby updated = lobbyService.updateVoicePresence(lobby.getLobbyId(), channel.getMembers().size());
                        messageUpdater.updateCard(event.getJDA(), updated);
                    });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"aram".equals(event.getName())) {
            return;
        }
        if ("list".equals(event.getSubcommandName())) {
            event.replyEmbeds(lobbyCardRenderer.renderLobbyList(lobbyService.findOpenLobbies())).queue();
            return;
        }
        if ("close".equals(event.getSubcommandName())) {
            closeLatestOwnedLobby(event);
        }
    }

    private void createLobbyFromMessage(MessageReceivedEvent event, String riotJoinLink) {
        Guild guild = event.getGuild();
        Member owner = event.getMember();
        String ownerDisplayName = owner == null ? event.getAuthor().getName() : owner.getEffectiveName();

        voiceRoomFactory.createVoiceRoom(guild, ownerDisplayName)
                .thenCompose(room -> room.voiceChannel().createInvite().submit()
                        .thenApply(invite -> {
                            GuildMessageChannel channel = event.getGuildChannel();
                            return lobbyService.createLobby(new CreateLobbyCommand(
                                    event.getAuthor().getId(),
                                    ownerDisplayName,
                                    event.getMessageId(),
                                    channel.getId(),
                                    riotJoinLink,
                                    room.voiceChannel().getId(),
                                    room.voiceChannel().getName(),
                                    invite.getUrl()
                            ));
                        }))
                .thenCompose(lobby -> event.getMessage()
                        .replyEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .submit()
                        .thenApply(message -> lobbyService.attachCardMessage(lobby.getLobbyId(), message.getId())))
                .exceptionally(ex -> {
                    log.warn("Failed to create ARAM lobby for message {}", event.getMessageId(), ex);
                    event.getMessage().reply("建立 ARAM Lobby 失敗，請確認 Bot 權限與 Voice Category 設定。").queue();
                    return null;
                });
    }

    private void closeLatestOwnedLobby(SlashCommandInteractionEvent event) {
        lobbyService.findLatestOwnedActiveLobby(event.getUser().getId())
                .ifPresentOrElse(lobby -> {
                    Lobby closed = lobbyService.markClosed(lobby.getLobbyId());
                    VoiceChannel voiceChannel = event.getJDA().getVoiceChannelById(closed.getVoiceChannelId());
                    if (voiceChannel != null) {
                        voiceChannel.delete().queue(
                                ignored -> log.info("Deleted voice channel {} for manually closed lobby {}", closed.getVoiceChannelId(), closed.getLobbyId()),
                                ex -> log.warn("Failed to delete voice channel {} for lobby {}", closed.getVoiceChannelId(), closed.getLobbyId(), ex)
                        );
                    }
                    messageUpdater.updateCard(event.getJDA(), closed);
                    event.reply("已關閉你的最新 ARAM Lobby。").setEphemeral(true).queue();
                }, () -> event.reply("你目前沒有可關閉的 ARAM Lobby。").setEphemeral(true).queue());
    }
}
