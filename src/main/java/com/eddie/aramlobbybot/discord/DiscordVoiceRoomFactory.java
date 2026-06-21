package com.eddie.aramlobbybot.discord;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import com.eddie.aramlobbybot.config.DiscordProperties;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.stereotype.Component;

@Component
public class DiscordVoiceRoomFactory {

    private static final int MAX_CHANNEL_NAME_LENGTH = 90;

    private final DiscordProperties discordProperties;

    public DiscordVoiceRoomFactory(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    public CompletableFuture<VoiceRoom> createVoiceRoom(Guild guild, String ownerDisplayName) {
        return findOrCreateCategory(guild)
                .thenCompose(category -> category.createVoiceChannel(channelNameFor(ownerDisplayName)).submit())
                .thenApply(VoiceRoom::new);
    }

    private CompletableFuture<Category> findOrCreateCategory(Guild guild) {
        return guild.getCategoriesByName(discordProperties.voiceCategoryName(), true).stream()
                .min(Comparator.comparing(Category::getPositionRaw))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> guild.createCategory(discordProperties.voiceCategoryName()).submit());
    }

    String channelNameFor(String ownerDisplayName) {
        String normalizedName = ownerDisplayName == null ? "Room" : ownerDisplayName.trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^\\p{L}\\p{N}_-]", "");
        if (normalizedName.isBlank()) {
            normalizedName = "Room";
        }
        String channelName = "ARAM-" + normalizedName;
        if (channelName.length() <= MAX_CHANNEL_NAME_LENGTH) {
            return channelName;
        }
        return channelName.substring(0, MAX_CHANNEL_NAME_LENGTH);
    }

    public record VoiceRoom(VoiceChannel voiceChannel) {
    }
}
