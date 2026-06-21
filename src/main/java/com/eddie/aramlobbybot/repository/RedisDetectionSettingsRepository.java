package com.eddie.aramlobbybot.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDetectionSettingsRepository implements DetectionSettingsRepository {

    private static final String KEY_PREFIX = "aram:settings:guild:";
    private static final String DISABLED_CHANNELS_SUFFIX = ":disabled-channels";

    private final StringRedisTemplate redisTemplate;

    public RedisDetectionSettingsRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDetectionEnabled(String guildId, String channelId) {
        return !Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(keyFor(guildId), channelId));
    }

    @Override
    public void disableDetection(String guildId, String channelId) {
        redisTemplate.opsForSet().add(keyFor(guildId), channelId);
    }

    @Override
    public void enableDetection(String guildId, String channelId) {
        redisTemplate.opsForSet().remove(keyFor(guildId), channelId);
    }

    public static String keyFor(String guildId) {
        return KEY_PREFIX + guildId + DISABLED_CHANNELS_SUFFIX;
    }
}
