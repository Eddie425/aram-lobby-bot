package com.eddie.aramlobbybot.repository;

import java.util.List;
import java.util.Optional;

import com.eddie.aramlobbybot.domain.Lobby;

public interface LobbyRepository {

    Lobby save(Lobby lobby);

    Optional<Lobby> findById(String lobbyId);

    List<Lobby> findAll();

    void deleteById(String lobbyId);
}
