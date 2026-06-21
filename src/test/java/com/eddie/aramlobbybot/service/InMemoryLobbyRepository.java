package com.eddie.aramlobbybot.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.eddie.aramlobbybot.domain.Lobby;
import com.eddie.aramlobbybot.repository.LobbyRepository;

class InMemoryLobbyRepository implements LobbyRepository {

    private final Map<String, Lobby> lobbies = new LinkedHashMap<>();

    @Override
    public Lobby save(Lobby lobby) {
        lobbies.put(lobby.getLobbyId(), lobby);
        return lobby;
    }

    @Override
    public Optional<Lobby> findById(String lobbyId) {
        return Optional.ofNullable(lobbies.get(lobbyId));
    }

    @Override
    public List<Lobby> findAll() {
        return new ArrayList<>(lobbies.values());
    }

    @Override
    public void deleteById(String lobbyId) {
        lobbies.remove(lobbyId);
    }
}
