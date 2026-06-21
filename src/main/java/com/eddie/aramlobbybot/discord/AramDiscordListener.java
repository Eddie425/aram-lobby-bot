package com.eddie.aramlobbybot.discord;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.repository.DetectionSettingsRepository;
import com.eddie.aramlobbybot.repository.NotificationSubscriptionRepository;
import com.eddie.aramlobbybot.service.LobbyService;
import com.eddie.aramlobbybot.service.LobbyService.CreateLobbyCommand;
import com.eddie.aramlobbybot.service.LolInviteLinkDetector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.Permission;
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
    private final DetectionSettingsRepository detectionSettingsRepository;
    private final NotificationSubscriptionRepository notificationSubscriptionRepository;

    public AramDiscordListener(
            LolInviteLinkDetector linkDetector,
            LobbyService lobbyService,
            LobbyCardRenderer lobbyCardRenderer,
            DiscordLobbyMessageUpdater messageUpdater,
            DiscordVoiceRoomFactory voiceRoomFactory,
            DetectionSettingsRepository detectionSettingsRepository,
            NotificationSubscriptionRepository notificationSubscriptionRepository
    ) {
        this.linkDetector = linkDetector;
        this.lobbyService = lobbyService;
        this.lobbyCardRenderer = lobbyCardRenderer;
        this.messageUpdater = messageUpdater;
        this.voiceRoomFactory = voiceRoomFactory;
        this.detectionSettingsRepository = detectionSettingsRepository;
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }
        if (!detectionSettingsRepository.isDetectionEnabled(event.getGuild().getId(), event.getChannel().getId())) {
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
            } else if (componentId.startsWith(LobbyButtonIds.READY_PREFIX)) {
                Lobby lobby = lobbyService.markReady(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.READY_PREFIX),
                        event.getUser().getId()
                );
                if (!lobby.getVoiceUserIds().contains(event.getUser().getId())) {
                    event.reply("你要先在這團的語音房裡，才能按 Ready。").setEphemeral(true).queue();
                    return;
                }
                event.editMessageEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .queue();
            } else if (componentId.startsWith(LobbyButtonIds.NOT_READY_PREFIX)) {
                Lobby lobby = lobbyService.markNotReady(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.NOT_READY_PREFIX),
                        event.getUser().getId()
                );
                event.editMessageEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .queue();
            } else if (componentId.startsWith(LobbyButtonIds.WAITLIST_PREFIX)) {
                Lobby lobby = lobbyService.joinWaitlist(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.WAITLIST_PREFIX),
                        event.getUser().getId()
                );
                event.editMessageEmbeds(lobbyCardRenderer.renderLobbyCard(lobby))
                        .setComponents(lobbyCardRenderer.renderActions(lobby))
                        .queue();
            } else if (componentId.startsWith(LobbyButtonIds.WAITLIST_LEAVE_PREFIX)) {
                Lobby lobby = lobbyService.leaveWaitlist(
                        LobbyButtonIds.extractLobbyId(componentId, LobbyButtonIds.WAITLIST_LEAVE_PREFIX),
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
                        int previousMissingCount = lobby.missingCount();
                        Set<String> voiceUserIds = channel.getMembers().stream()
                                .map(member -> member.getUser().getId())
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                        Lobby updated = lobbyService.updateVoicePresence(lobby.getLobbyId(), channel.getMembers().size(), voiceUserIds);
                        messageUpdater.updateCard(event.getJDA(), updated);
                        notifyVacancyIfNeeded(event, updated, previousMissingCount);
                    });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"aram".equals(event.getName())) {
            return;
        }
        if ("list".equals(event.getSubcommandName()) || "available".equals(event.getSubcommandName())) {
            event.replyEmbeds(lobbyCardRenderer.renderLobbyList(lobbyService.findOpenLobbies())).setEphemeral(true).queue();
            return;
        }
        if ("close".equals(event.getSubcommandName())) {
            closeLatestOwnedLobby(event);
            return;
        }
        if ("disable".equals(event.getSubcommandName())) {
            disableDetection(event);
            return;
        }
        if ("enable".equals(event.getSubcommandName())) {
            enableDetection(event);
            return;
        }
        if ("status".equals(event.getSubcommandName())) {
            replyStatus(event);
            return;
        }
        if ("help".equals(event.getSubcommandName())) {
            event.replyEmbeds(lobbyCardRenderer.renderCommandHelp()).setEphemeral(true).queue();
            return;
        }
        if ("notify-on".equals(event.getSubcommandName())) {
            notificationSubscriptionRepository.subscribe(event.getGuild().getId(), event.getUser().getId());
            event.reply("已訂閱 ARAM 缺人通知。當有團缺 1-2 人時我會提醒你。").setEphemeral(true).queue();
            return;
        }
        if ("notify-off".equals(event.getSubcommandName())) {
            notificationSubscriptionRepository.unsubscribe(event.getGuild().getId(), event.getUser().getId());
            event.reply("已取消 ARAM 缺人通知。").setEphemeral(true).queue();
            return;
        }
        if ("notify-status".equals(event.getSubcommandName())) {
            boolean subscribed = notificationSubscriptionRepository.isSubscribed(event.getGuild().getId(), event.getUser().getId());
            event.reply(subscribed ? "你目前有訂閱 ARAM 缺人通知。" : "你目前沒有訂閱 ARAM 缺人通知。")
                    .setEphemeral(true)
                    .queue();
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

    private void disableDetection(SlashCommandInteractionEvent event) {
        if (!hasManageChannels(event)) {
            event.reply("你需要 Manage Channels 權限才能關閉這個頻道的自動偵測。").setEphemeral(true).queue();
            return;
        }
        detectionSettingsRepository.disableDetection(event.getGuild().getId(), event.getChannel().getId());
        event.reply("已關閉這個頻道的 LoL invite link 自動偵測。需要時可用 `/aram enable` 打開。")
                .setEphemeral(true)
                .queue();
    }

    private void enableDetection(SlashCommandInteractionEvent event) {
        if (!hasManageChannels(event)) {
            event.reply("你需要 Manage Channels 權限才能開啟這個頻道的自動偵測。").setEphemeral(true).queue();
            return;
        }
        detectionSettingsRepository.enableDetection(event.getGuild().getId(), event.getChannel().getId());
        event.reply("已開啟這個頻道的 LoL invite link 自動偵測。").setEphemeral(true).queue();
    }

    private void replyStatus(SlashCommandInteractionEvent event) {
        boolean enabled = detectionSettingsRepository.isDetectionEnabled(event.getGuild().getId(), event.getChannel().getId());
        event.replyEmbeds(lobbyCardRenderer.renderBotStatus(
                        enabled,
                        lobbyService.findOpenLobbies(),
                        lobbyService.findActiveLobbies()
                ))
                .setEphemeral(true)
                .queue();
    }

    private boolean hasManageChannels(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        return member != null && member.hasPermission(Permission.MANAGE_CHANNEL);
    }

    private void notifyVacancyIfNeeded(GuildVoiceUpdateEvent event, Lobby lobby, int previousMissingCount) {
        int missingCount = lobby.missingCount();
        if (lobby.isClosed() || missingCount <= 0 || missingCount > 2) {
            return;
        }
        if (lobby.getLastNotifiedMissingCount() != null && lobby.getLastNotifiedMissingCount() == missingCount) {
            return;
        }
        if (previousMissingCount == missingCount) {
            return;
        }

        Set<String> recipients = new LinkedHashSet<>(lobby.getWaitlistUserIds());
        recipients.addAll(notificationSubscriptionRepository.findSubscribers(event.getGuild().getId()));
        recipients.removeAll(lobby.getVoiceUserIds());
        if (recipients.isEmpty()) {
            lobbyService.markMissingNotified(lobby.getLobbyId(), missingCount);
            return;
        }

        GuildMessageChannel channel = event.getGuild().getChannelById(GuildMessageChannel.class, lobby.getTextChannelId());
        if (channel == null) {
            return;
        }
        String mentions = recipients.stream()
                .map(userId -> "<@" + userId + ">")
                .collect(Collectors.joining(" "));
        channel.sendMessage(mentions + "\n⚡ `" + lobby.getVoiceChannelName() + "` 目前缺 **" + missingCount + "**，可以上車。")
                .queue(
                        ignored -> lobbyService.markMissingNotified(lobby.getLobbyId(), missingCount),
                        ex -> log.warn("Failed to send vacancy notification for lobby {}", lobby.getLobbyId(), ex)
                );
    }
}
