package com.eddie.aramlobbybot.discord;

import static org.assertj.core.api.Assertions.assertThat;

import com.eddie.aramlobbybot.config.DiscordProperties;
import org.junit.jupiter.api.Test;

class DiscordVoiceRoomFactoryTest {

    private final DiscordVoiceRoomFactory factory = new DiscordVoiceRoomFactory(new DiscordProperties("token", "Voice"));

    @Test
    void createsStableVoiceChannelNameFromOwnerDisplayName() {
        assertThat(factory.channelNameFor("Eddie Chang!!")).isEqualTo("ARAM-Eddie-Chang");
    }

    @Test
    void fallsBackWhenOwnerDisplayNameHasNoUsableCharacters() {
        assertThat(factory.channelNameFor("!!!")).isEqualTo("ARAM-Room");
    }
}
