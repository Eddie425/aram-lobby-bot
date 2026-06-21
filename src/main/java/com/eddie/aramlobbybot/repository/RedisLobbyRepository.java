package com.eddie.aramlobbybot.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.eddie.aramlobbybot.config.AramProperties;
import com.eddie.aramlobbybot.domain.Lobby;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Repository;

@Repository
public class RedisLobbyRepository implements LobbyRepository {

    public static final String KEY_PREFIX = "aram:lobby:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisLobbyRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AramProperties aramProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = aramProperties.lobby().ttl();
    }

    @Override
    public Lobby save(Lobby lobby) {
        try {
            ValueOperations<String, String> values = redisTemplate.opsForValue();
            values.set(keyFor(lobby.getLobbyId()), objectMapper.writeValueAsString(lobby), ttl);
            return lobby;
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Failed to serialize lobby " + lobby.getLobbyId(), ex);
        }
    }

    @Override
    public Optional<Lobby> findById(String lobbyId) {
        String value = redisTemplate.opsForValue().get(keyFor(lobbyId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(readLobby(value));
    }

    @Override
    public List<Lobby> findAll() {
        List<Lobby> result = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(KEY_PREFIX + "*")
                .count(100)
                .build();
        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String value = redisTemplate.opsForValue().get(cursor.next());
                if (value != null) {
                    result.add(readLobby(value));
                }
            }
            return result;
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    @Override
    public void deleteById(String lobbyId) {
        redisTemplate.delete(keyFor(lobbyId));
    }

    public static String keyFor(String lobbyId) {
        return KEY_PREFIX + lobbyId;
    }

    private Lobby readLobby(String value) {
        try {
            return objectMapper.readValue(value, Lobby.class);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Failed to deserialize lobby", ex);
        }
    }
}
