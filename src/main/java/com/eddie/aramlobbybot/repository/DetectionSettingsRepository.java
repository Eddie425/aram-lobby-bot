package com.eddie.aramlobbybot.repository;

public interface DetectionSettingsRepository {

    boolean isDetectionEnabled(String guildId, String channelId);

    DetectionMode findDetectionMode(String guildId, String channelId);

    void setDetectionMode(String guildId, String channelId, DetectionMode detectionMode);

    void disableDetection(String guildId, String channelId);

    void enableDetection(String guildId, String channelId);
}
