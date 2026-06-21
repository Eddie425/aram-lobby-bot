package com.eddie.aramlobbybot.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.domain.LobbyStatus;
import com.eddie.aramlobbybot.repository.LobbyRepository;
import org.springframework.stereotype.Service;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final Clock clock;

    public LobbyService(LobbyRepository lobbyRepository, Clock clock) {
        this.lobbyRepository = lobbyRepository;
        this.clock = clock;
    }

    public synchronized Lobby createLobby(CreateLobbyCommand command) {
        Instant now = clock.instant();
        Lobby lobby = new Lobby();
        lobby.setLobbyId(UUID.randomUUID().toString());
        lobby.setOwnerUserId(command.ownerUserId());
        lobby.setOwnerDisplayName(command.ownerDisplayName());
        lobby.setSourceMessageId(command.sourceMessageId());
        lobby.setTextChannelId(command.textChannelId());
        lobby.setRiotJoinLink(command.riotJoinLink());
        lobby.setVoiceChannelId(command.voiceChannelId());
        lobby.setVoiceChannelName(command.voiceChannelName());
        lobby.setVoiceInviteLink(command.voiceInviteLink());
        lobby.setJoinedUsers(new LinkedHashSet<>(List.of(command.ownerUserId())));
        lobby.setVoiceMemberCount(0);
        lobby.setVoiceEmptySince(now);
        lobby.setStatus(LobbyStatus.OPEN);
        lobby.setCreatedAt(now);
        return lobbyRepository.save(lobby);
    }

    public synchronized Lobby attachCardMessage(String lobbyId, String cardMessageId) {
        Lobby lobby = requireLobby(lobbyId);
        lobby.setCardMessageId(cardMessageId);
        return lobbyRepository.save(lobby);
    }

    public synchronized Lobby joinLobby(String lobbyId, String userId) {
        Lobby lobby = requireLobby(lobbyId);
        if (lobby.isClosed()) {
            return lobby;
        }
        if (!lobby.getJoinedUsers().contains(userId) && lobby.joinedCount() < Lobby.MAX_PLAYERS) {
            lobby.getJoinedUsers().add(userId);
        }
        refreshStatus(lobby);
        return lobbyRepository.save(lobby);
    }

    public synchronized Lobby leaveLobby(String lobbyId, String userId) {
        Lobby lobby = requireLobby(lobbyId);
        if (lobby.isClosed()) {
            return lobby;
        }
        lobby.getJoinedUsers().remove(userId);
        refreshStatus(lobby);
        return lobbyRepository.save(lobby);
    }

    public synchronized Lobby updateVoicePresence(String lobbyId, int voiceMemberCount) {
        Lobby lobby = requireLobby(lobbyId);
        if (lobby.isClosed()) {
            return lobby;
        }
        lobby.setVoiceMemberCount(Math.max(0, voiceMemberCount));
        if (voiceMemberCount > 0) {
            lobby.setVoiceEmptySince(null);
        } else if (lobby.getVoiceEmptySince() == null) {
            lobby.setVoiceEmptySince(clock.instant());
        }
        refreshStatus(lobby);
        return lobbyRepository.save(lobby);
    }

    public synchronized Lobby markClosed(String lobbyId) {
        Lobby lobby = requireLobby(lobbyId);
        if (!lobby.isClosed()) {
            lobby.setStatus(LobbyStatus.CLOSED);
            lobby.setClosedAt(clock.instant());
            lobbyRepository.save(lobby);
        }
        return lobby;
    }

    public synchronized void deleteLobby(String lobbyId) {
        lobbyRepository.deleteById(lobbyId);
    }

    public Optional<Lobby> findById(String lobbyId) {
        return lobbyRepository.findById(lobbyId);
    }

    public List<Lobby> findActiveLobbies() {
        return lobbyRepository.findAll().stream()
                .filter(lobby -> !lobby.isClosed())
                .sorted(Comparator.comparing(Lobby::getCreatedAt).reversed())
                .toList();
    }

    public List<Lobby> findOpenLobbies() {
        return findActiveLobbies().stream()
                .filter(lobby -> lobby.getStatus() == LobbyStatus.OPEN)
                .toList();
    }

    public Optional<Lobby> findLatestOwnedActiveLobby(String ownerUserId) {
        return findActiveLobbies().stream()
                .filter(lobby -> ownerUserId.equals(lobby.getOwnerUserId()))
                .findFirst();
    }

    public boolean shouldCleanup(Lobby lobby, Duration emptyGrace) {
        if (lobby.isClosed() || lobby.getVoiceMemberCount() > 0 || lobby.getVoiceEmptySince() == null) {
            return false;
        }
        return !lobby.getVoiceEmptySince().plus(emptyGrace).isAfter(clock.instant());
    }

    private Lobby requireLobby(String lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + lobbyId));
    }

    private void refreshStatus(Lobby lobby) {
        if (lobby.playerCount() >= Lobby.MAX_PLAYERS) {
            lobby.setStatus(LobbyStatus.FULL);
        } else {
            lobby.setStatus(LobbyStatus.OPEN);
        }
    }

    public record CreateLobbyCommand(
            String ownerUserId,
            String ownerDisplayName,
            String sourceMessageId,
            String textChannelId,
            String riotJoinLink,
            String voiceChannelId,
            String voiceChannelName,
            String voiceInviteLink
    ) {
    }
}
