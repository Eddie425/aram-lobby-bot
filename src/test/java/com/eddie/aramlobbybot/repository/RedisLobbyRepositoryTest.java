package com.eddie.aramlobbybot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;

import com.eddie.aramlobbybot.config.AramProperties;
import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.domain.LobbyStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisLobbyRepositoryTest {

    @Test
    void keyUsesExpectedNamespace() {
        assertThat(RedisLobbyRepository.keyFor("lobby-1")).isEqualTo("aram:lobby:lobby-1");
    }

    @Test
    void savesLobbyJsonWithConfiguredTtlAndReadsItBack() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Duration ttl = Duration.ofHours(4);
        RedisLobbyRepository repository = new RedisLobbyRepository(
                redisTemplate,
                objectMapper,
                new AramProperties(new AramProperties.Lobby(ttl), new AramProperties.Cleanup(Duration.ofMinutes(10), Duration.ofMinutes(5)))
        );
        Lobby lobby = lobby();

        repository.save(lobby);

        verify(values).set(eq("aram:lobby:lobby-1"), anyString(), eq(ttl));

        String json = objectMapper.writeValueAsString(lobby);
        when(values.get("aram:lobby:lobby-1")).thenReturn(json);

        assertThat(repository.findById("lobby-1"))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.getLobbyId()).isEqualTo("lobby-1");
                    assertThat(saved.getStatus()).isEqualTo(LobbyStatus.OPEN);
                    assertThat(saved.getJoinedUsers()).containsExactly("owner");
                });
    }

    private Lobby lobby() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId("lobby-1");
        lobby.setOwnerUserId("owner");
        lobby.setOwnerDisplayName("Eddie");
        lobby.setSourceMessageId("message-1");
        lobby.setTextChannelId("text-1");
        lobby.setRiotJoinLink("https://gg.riotgames.com/LOL?joinCode=abc");
        lobby.setVoiceChannelId("voice-1");
        lobby.setVoiceChannelName("ARAM-Eddie");
        lobby.setVoiceInviteLink("https://discord.gg/abc");
        lobby.setJoinedUsers(new LinkedHashSet<>(java.util.List.of("owner")));
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setCreatedAt(Instant.parse("2026-06-21T09:00:00Z"));
        lobby.setVoiceEmptySince(Instant.parse("2026-06-21T09:00:00Z"));
        return lobby;
    }
}
