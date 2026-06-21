package com.eddie.aramlobbybot.repository;

public interface DetectionSettingsRepository {

    boolean isDetectionEnabled(String guildId, String channelId);

    void disableDetection(String guildId, String channelId);

    void enableDetection(String guildId, String channelId);
}
