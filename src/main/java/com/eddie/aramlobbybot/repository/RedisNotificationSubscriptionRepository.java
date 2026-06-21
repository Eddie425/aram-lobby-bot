package com.eddie.aramlobbybot.repository;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisNotificationSubscriptionRepository implements NotificationSubscriptionRepository {

    private static final String KEY_PREFIX = "aram:notify:guild:";
    private static final String USERS_SUFFIX = ":users";

    private final StringRedisTemplate redisTemplate;

    public RedisNotificationSubscriptionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void subscribe(String guildId, String userId) {
        redisTemplate.opsForSet().add(keyFor(guildId), userId);
    }

    @Override
    public void unsubscribe(String guildId, String userId) {
        redisTemplate.opsForSet().remove(keyFor(guildId), userId);
    }

    @Override
    public boolean isSubscribed(String guildId, String userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(keyFor(guildId), userId));
    }

    @Override
    public Set<String> findSubscribers(String guildId) {
        Set<String> subscribers = redisTemplate.opsForSet().members(keyFor(guildId));
        return subscribers == null ? Set.of() : subscribers;
    }

    public static String keyFor(String guildId) {
        return KEY_PREFIX + guildId + USERS_SUFFIX;
    }
}
