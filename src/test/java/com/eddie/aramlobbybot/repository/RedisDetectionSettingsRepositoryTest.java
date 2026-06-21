package com.eddie.aramlobbybot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisDetectionSettingsRepositoryTest {

    @Test
    void keyUsesExpectedNamespace() {
        assertThat(RedisDetectionSettingsRepository.keyFor("guild-1"))
                .isEqualTo("aram:settings:guild:guild-1:disabled-channels");
    }

    @Test
    void disabledChannelMeansDetectionIsNotEnabled() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);
        when(sets.isMember("aram:settings:guild:guild-1:disabled-channels", "channel-1")).thenReturn(true);

        RedisDetectionSettingsRepository repository = new RedisDetectionSettingsRepository(redisTemplate);

        assertThat(repository.isDetectionEnabled("guild-1", "channel-1")).isFalse();
    }

    @Test
    void disableAndEnableUpdateRedisSet() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);

        RedisDetectionSettingsRepository repository = new RedisDetectionSettingsRepository(redisTemplate);
        repository.disableDetection("guild-1", "channel-1");
        repository.enableDetection("guild-1", "channel-1");

        verify(sets).add("aram:settings:guild:guild-1:disabled-channels", "channel-1");
        verify(sets).remove("aram:settings:guild:guild-1:disabled-channels", "channel-1");
    }
}
