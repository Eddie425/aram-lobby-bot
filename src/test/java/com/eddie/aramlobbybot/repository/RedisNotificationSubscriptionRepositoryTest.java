package com.eddie.aramlobbybot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisNotificationSubscriptionRepositoryTest {

    @Test
    void keyUsesExpectedNamespace() {
        assertThat(RedisNotificationSubscriptionRepository.keyFor("guild-1"))
                .isEqualTo("aram:notify:guild:guild-1:users");
    }

    @Test
    void subscribeAndUnsubscribeUpdateRedisSet() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);

        RedisNotificationSubscriptionRepository repository = new RedisNotificationSubscriptionRepository(redisTemplate);
        repository.subscribe("guild-1", "user-1");
        repository.unsubscribe("guild-1", "user-1");

        verify(sets).add("aram:notify:guild:guild-1:users", "user-1");
        verify(sets).remove("aram:notify:guild:guild-1:users", "user-1");
    }

    @Test
    void isSubscribedReturnsTrueOnlyWhenRedisReportsMembership() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);
        when(sets.isMember("aram:notify:guild:guild-1:users", "user-1")).thenReturn(true);
        when(sets.isMember("aram:notify:guild:guild-1:users", "user-2")).thenReturn(false);

        RedisNotificationSubscriptionRepository repository = new RedisNotificationSubscriptionRepository(redisTemplate);

        assertThat(repository.isSubscribed("guild-1", "user-1")).isTrue();
        assertThat(repository.isSubscribed("guild-1", "user-2")).isFalse();
    }

    @Test
    void findSubscribersReturnsEmptySetWhenRedisReturnsNull() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);

        RedisNotificationSubscriptionRepository repository = new RedisNotificationSubscriptionRepository(redisTemplate);

        assertThat(repository.findSubscribers("guild-1")).isEmpty();
    }

    @Test
    void findSubscribersReturnsRedisMembers() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(sets);
        when(sets.members("aram:notify:guild:guild-1:users")).thenReturn(Set.of("user-1", "user-2"));

        RedisNotificationSubscriptionRepository repository = new RedisNotificationSubscriptionRepository(redisTemplate);

        assertThat(repository.findSubscribers("guild-1")).containsExactlyInAnyOrder("user-1", "user-2");
    }
}
