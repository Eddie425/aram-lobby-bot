package com.eddie.aramlobbybot.discord;

public final class LobbyButtonIds {

    public static final String JOIN_PREFIX = "aram_join:";
    public static final String LEAVE_PREFIX = "aram_leave:";
    public static final String READY_PREFIX = "aram_ready:";
    public static final String NOT_READY_PREFIX = "aram_not_ready:";
    public static final String WAITLIST_PREFIX = "aram_waitlist:";
    public static final String WAITLIST_LEAVE_PREFIX = "aram_waitlist_leave:";

    private LobbyButtonIds() {
    }

    public static String join(String lobbyId) {
        return JOIN_PREFIX + lobbyId;
    }

    public static String leave(String lobbyId) {
        return LEAVE_PREFIX + lobbyId;
    }

    public static String ready(String lobbyId) {
        return READY_PREFIX + lobbyId;
    }

    public static String notReady(String lobbyId) {
        return NOT_READY_PREFIX + lobbyId;
    }

    public static String waitlist(String lobbyId) {
        return WAITLIST_PREFIX + lobbyId;
    }

    public static String leaveWaitlist(String lobbyId) {
        return WAITLIST_LEAVE_PREFIX + lobbyId;
    }

    public static String extractLobbyId(String componentId, String prefix) {
        if (!componentId.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected component id: " + componentId);
        }
        return componentId.substring(prefix.length());
    }
}
