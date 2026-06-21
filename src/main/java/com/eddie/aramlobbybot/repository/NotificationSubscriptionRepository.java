package com.eddie.aramlobbybot.repository;

import java.util.Set;

public interface NotificationSubscriptionRepository {

    void subscribe(String guildId, String userId);

    void unsubscribe(String guildId, String userId);

    boolean isSubscribed(String guildId, String userId);

    Set<String> findSubscribers(String guildId);
}
