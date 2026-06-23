package com.eddie.aramlobbybot.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDetectionSettingsRepository implements DetectionSettingsRepository {

    private static final String KEY_PREFIX = "aram:settings:guild:";
    private static final String DISABLED_CHANNELS_SUFFIX = ":disabled-channels";
    private static final String CHANNEL_MODES_SUFFIX = ":channel-modes";

    private final StringRedisTemplate redisTemplate;

    public RedisDetectionSettingsRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDetectionEnabled(String guildId, String channelId) {
        return !Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(disabledChannelsKeyFor(guildId), channelId));
    }

    @Override
    public DetectionMode findDetectionMode(String guildId, String channelId) {
        Object value = redisTemplate.opsForHash().get(channelModesKeyFor(guildId), channelId);
        if (value == null) {
            return DetectionMode.PREFIX;
        }
        try {
            return DetectionMode.valueOf(value.toString());
        } catch (IllegalArgumentException ex) {
            return DetectionMode.PREFIX;
        }
    }

    @Override
    public void setDetectionMode(String guildId, String channelId, DetectionMode detectionMode) {
        redisTemplate.opsForHash().put(channelModesKeyFor(guildId), channelId, detectionMode.name());
    }

    @Override
    public void disableDetection(String guildId, String channelId) {
        redisTemplate.opsForSet().add(disabledChannelsKeyFor(guildId), channelId);
    }

    @Override
    public void enableDetection(String guildId, String channelId) {
        redisTemplate.opsForSet().remove(disabledChannelsKeyFor(guildId), channelId);
    }

    public static String disabledChannelsKeyFor(String guildId) {
        return KEY_PREFIX + guildId + DISABLED_CHANNELS_SUFFIX;
    }

    public static String channelModesKeyFor(String guildId) {
        return KEY_PREFIX + guildId + CHANNEL_MODES_SUFFIX;
    }
}
