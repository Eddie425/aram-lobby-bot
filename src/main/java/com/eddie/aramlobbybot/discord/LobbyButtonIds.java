package com.eddie.aramlobbybot.discord;

public final class LobbyButtonIds {

    public static final String JOIN_PREFIX = "aram_join:";
    public static final String LEAVE_PREFIX = "aram_leave:";

    private LobbyButtonIds() {
    }

    public static String join(String lobbyId) {
        return JOIN_PREFIX + lobbyId;
    }

    public static String leave(String lobbyId) {
        return LEAVE_PREFIX + lobbyId;
    }

    public static String extractLobbyId(String componentId, String prefix) {
        if (!componentId.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected component id: " + componentId);
        }
        return componentId.substring(prefix.length());
    }
}
