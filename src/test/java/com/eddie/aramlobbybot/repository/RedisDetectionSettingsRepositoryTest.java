package com.eddie.aramlobbybot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisDetectionSettingsRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private SetOperations<String, String> sets;
    private HashOperations<String, Object, Object> hashes;
    private RedisDetectionSettingsRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        sets = setOperations;
        hashes = hashOperations;
        when(redisTemplate.opsForSet()).thenReturn(sets);
        when(redisTemplate.opsForHash()).thenReturn(hashes);
        repository = new RedisDetectionSettingsRepository(redisTemplate);
    }

    @Test
    void keyUsesExpectedNamespace() {
        assertThat(RedisDetectionSettingsRepository.disabledChannelsKeyFor("guild-1"))
                .isEqualTo("aram:settings:guild:guild-1:disabled-channels");
        assertThat(RedisDetectionSettingsRepository.channelModesKeyFor("guild-1"))
                .isEqualTo("aram:settings:guild:guild-1:channel-modes");
    }

    @Test
    void disabledChannelMeansDetectionIsNotEnabled() {
        when(sets.isMember("aram:settings:guild:guild-1:disabled-channels", "channel-1")).thenReturn(true);

        assertThat(repository.isDetectionEnabled("guild-1", "channel-1")).isFalse();
    }

    @Test
    void disableAndEnableUpdateRedisSet() {
        repository.disableDetection("guild-1", "channel-1");
        repository.enableDetection("guild-1", "channel-1");

        verify(sets).add("aram:settings:guild:guild-1:disabled-channels", "channel-1");
        verify(sets).remove("aram:settings:guild:guild-1:disabled-channels", "channel-1");
    }

    @Test
    void missingOrInvalidDetectionModeDefaultsToPrefixMode() {
        when(hashes.get("aram:settings:guild:guild-1:channel-modes", "channel-1")).thenReturn(null);
        assertThat(repository.findDetectionMode("guild-1", "channel-1")).isEqualTo(DetectionMode.PREFIX);

        when(hashes.get("aram:settings:guild:guild-1:channel-modes", "channel-1")).thenReturn("unknown");
        assertThat(repository.findDetectionMode("guild-1", "channel-1")).isEqualTo(DetectionMode.PREFIX);
    }

    @Test
    void setAndFindDetectionModeUseRedisHash() {
        repository.setDetectionMode("guild-1", "channel-1", DetectionMode.AUTO);
        verify(hashes).put("aram:settings:guild:guild-1:channel-modes", "channel-1", "AUTO");

        when(hashes.get("aram:settings:guild:guild-1:channel-modes", "channel-1")).thenReturn("AUTO");
        assertThat(repository.findDetectionMode("guild-1", "channel-1")).isEqualTo(DetectionMode.AUTO);
    }
}
