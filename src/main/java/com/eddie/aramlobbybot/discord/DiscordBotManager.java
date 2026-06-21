package com.eddie.aramlobbybot.discord;

import java.util.Optional;

import com.eddie.aramlobbybot.config.DiscordProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DiscordBotManager {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotManager.class);

    private final DiscordProperties discordProperties;
    private final AramDiscordListener aramDiscordListener;
    private JDA jda;

    public DiscordBotManager(DiscordProperties discordProperties, AramDiscordListener aramDiscordListener) {
        this.discordProperties = discordProperties;
        this.aramDiscordListener = aramDiscordListener;
    }

    @PostConstruct
    void start() {
        if (!StringUtils.hasText(discordProperties.botToken())) {
            log.warn("DISCORD_BOT_TOKEN is empty; JDA will not be started.");
            return;
        }

        jda = JDABuilder.createDefault(discordProperties.botToken())
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .addEventListeners(aramDiscordListener)
                .build();

        registerSlashCommands(jda);
        log.info("Discord bot startup requested.");
    }

    @PreDestroy
    void stop() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public Optional<JDA> getJda() {
        return Optional.ofNullable(jda);
    }

    private void registerSlashCommands(JDA jda) {
        jda.updateCommands()
                .addCommands(Commands.slash("aram", "Manage ARAM lobbies")
                        .addSubcommands(
                                new SubcommandData("list", "Show active ARAM lobbies"),
                                new SubcommandData("close", "Close your latest ARAM lobby")
                        ))
                .queue(
                        ignored -> log.info("Registered /aram slash commands."),
                        ex -> log.warn("Failed to register /aram slash commands.", ex)
                );
    }
}
