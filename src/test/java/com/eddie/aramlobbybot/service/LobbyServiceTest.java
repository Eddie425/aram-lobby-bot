package com.eddie.aramlobbybot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.domain.LobbyStatus;
import com.eddie.aramlobbybot.service.LobbyService.CreateLobbyCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LobbyServiceTest {

    private MutableClock clock;
    private LobbyService lobbyService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-06-21T09:00:00Z"));
        lobbyService = new LobbyService(new InMemoryLobbyRepository(), clock);
    }

    @Test
    void createsLobbyWithOwnerAsFirstJoinedUserButUsesVoiceCountForMissingPlayers() {
        Lobby lobby = createLobby("owner");

        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.OPEN);
        assertThat(lobby.joinedCount()).isEqualTo(1);
        assertThat(lobby.playerCount()).isZero();
        assertThat(lobby.missingCount()).isEqualTo(5);
        assertThat(lobby.getVoiceEmptySince()).isEqualTo(clock.instant());
    }

    @Test
    void joinLobbyIsIdempotentButDoesNotDriveFullStatus() {
        Lobby lobby = createLobby("owner");

        lobbyService.joinLobby(lobby.getLobbyId(), "u1");
        lobbyService.joinLobby(lobby.getLobbyId(), "u1");
        lobbyService.joinLobby(lobby.getLobbyId(), "u2");
        lobbyService.joinLobby(lobby.getLobbyId(), "u3");
        Lobby fullLobby = lobbyService.joinLobby(lobby.getLobbyId(), "u4");
        Lobby unchanged = lobbyService.joinLobby(lobby.getLobbyId(), "u5");

        assertThat(fullLobby.getStatus()).isEqualTo(LobbyStatus.OPEN);
        assertThat(unchanged.joinedCount()).isEqualTo(5);
        assertThat(unchanged.playerCount()).isZero();
        assertThat(unchanged.getJoinedUsers()).doesNotContain("u5");
    }

    @Test
    void voicePresenceDrivesFullAndOpenStatus() {
        Lobby lobby = createLobby("owner");

        Lobby full = lobbyService.updateVoicePresence(lobby.getLobbyId(), 5);
        assertThat(full.getStatus()).isEqualTo(LobbyStatus.FULL);

        Lobby reopened = lobbyService.updateVoicePresence(lobby.getLobbyId(), 4);

        assertThat(reopened.getStatus()).isEqualTo(LobbyStatus.OPEN);
        assertThat(reopened.playerCount()).isEqualTo(4);
        assertThat(reopened.missingCount()).isEqualTo(1);
    }

    @Test
    void cleanupRequiresVoiceRoomToRemainEmptyForGracePeriod() {
        Lobby lobby = createLobby("owner");

        assertThat(lobbyService.shouldCleanup(lobby, Duration.ofMinutes(10))).isFalse();

        clock.advance(Duration.ofMinutes(10));
        Lobby updated = lobbyService.updateVoicePresence(lobby.getLobbyId(), 0);

        assertThat(lobbyService.shouldCleanup(updated, Duration.ofMinutes(10))).isTrue();
    }

    @Test
    void nonEmptyVoiceRoomResetsCleanupTimer() {
        Lobby lobby = createLobby("owner");
        clock.advance(Duration.ofMinutes(9));

        Lobby occupied = lobbyService.updateVoicePresence(lobby.getLobbyId(), 1);
        assertThat(occupied.getVoiceEmptySince()).isNull();

        clock.advance(Duration.ofMinutes(30));
        Lobby emptyAgain = lobbyService.updateVoicePresence(lobby.getLobbyId(), 0);

        assertThat(emptyAgain.getVoiceEmptySince()).isEqualTo(clock.instant());
        assertThat(lobbyService.shouldCleanup(emptyAgain, Duration.ofMinutes(10))).isFalse();
    }

    @Test
    void closedLobbyIsExcludedFromActiveAndOpenLists() {
        Lobby lobby = createLobby("owner");

        Lobby closed = lobbyService.markClosed(lobby.getLobbyId());

        assertThat(closed.getStatus()).isEqualTo(LobbyStatus.CLOSED);
        assertThat(lobbyService.findActiveLobbies()).isEmpty();
        assertThat(lobbyService.findOpenLobbies()).isEmpty();
        assertThat(lobbyService.shouldCleanup(closed, Duration.ofMinutes(10))).isFalse();
    }

    private Lobby createLobby(String ownerUserId) {
        return lobbyService.createLobby(new CreateLobbyCommand(
                ownerUserId,
                "Eddie",
                "message-1",
                "text-1",
                "https://gg.riotgames.com/LOL?joinCode=abc",
                "voice-1",
                "ARAM-Eddie",
                "https://discord.gg/abc"
        ));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
