package com.eddie.aramlobbybot.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Lobby {

    public static final int MAX_PLAYERS = 5;

    private String lobbyId;
    private String ownerUserId;
    private String ownerDisplayName;
    private String sourceMessageId;
    private String cardMessageId;
    private String textChannelId;
    private String riotJoinLink;
    private String voiceChannelId;
    private String voiceChannelName;
    private String voiceInviteLink;
    private Set<String> joinedUsers = new LinkedHashSet<>();
    private int voiceMemberCount;
    private LobbyStatus status;
    private Instant createdAt;
    private Instant voiceEmptySince;
    private Instant closedAt;

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public String getCardMessageId() {
        return cardMessageId;
    }

    public void setCardMessageId(String cardMessageId) {
        this.cardMessageId = cardMessageId;
    }

    public String getTextChannelId() {
        return textChannelId;
    }

    public void setTextChannelId(String textChannelId) {
        this.textChannelId = textChannelId;
    }

    public String getRiotJoinLink() {
        return riotJoinLink;
    }

    public void setRiotJoinLink(String riotJoinLink) {
        this.riotJoinLink = riotJoinLink;
    }

    public String getVoiceChannelId() {
        return voiceChannelId;
    }

    public void setVoiceChannelId(String voiceChannelId) {
        this.voiceChannelId = voiceChannelId;
    }

    public String getVoiceChannelName() {
        return voiceChannelName;
    }

    public void setVoiceChannelName(String voiceChannelName) {
        this.voiceChannelName = voiceChannelName;
    }

    public String getVoiceInviteLink() {
        return voiceInviteLink;
    }

    public void setVoiceInviteLink(String voiceInviteLink) {
        this.voiceInviteLink = voiceInviteLink;
    }

    public Set<String> getJoinedUsers() {
        return joinedUsers;
    }

    public void setJoinedUsers(Set<String> joinedUsers) {
        this.joinedUsers = joinedUsers == null ? new LinkedHashSet<>() : new LinkedHashSet<>(joinedUsers);
    }

    public int getVoiceMemberCount() {
        return voiceMemberCount;
    }

    public void setVoiceMemberCount(int voiceMemberCount) {
        this.voiceMemberCount = voiceMemberCount;
    }

    public LobbyStatus getStatus() {
        return status;
    }

    public void setStatus(LobbyStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getVoiceEmptySince() {
        return voiceEmptySince;
    }

    public void setVoiceEmptySince(Instant voiceEmptySince) {
        this.voiceEmptySince = voiceEmptySince;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public int joinedCount() {
        return joinedUsers.size();
    }

    public int playerCount() {
        return Math.min(MAX_PLAYERS, Math.max(0, voiceMemberCount));
    }

    public int missingCount() {
        return Math.max(0, MAX_PLAYERS - playerCount());
    }

    @JsonIgnore
    public boolean isClosed() {
        return status == LobbyStatus.CLOSED;
    }
}
