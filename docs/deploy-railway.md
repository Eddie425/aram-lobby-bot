# Deploy on Railway

Railway is the recommended MVP host for this bot because it supports a long-running Docker service and a Redis service in the same project.

## Why Railway

- GitHub-based deployments work with this repository's Dockerfile.
- Redis can be added as a project service.
- Service variables can store the Discord bot token safely.
- The bot does not need an HTTP port or public web endpoint.

## Steps

1. Create a new Railway project from the GitHub repository.
2. Add a Redis database service to the same Railway project.
3. Open the bot service variables and set:

| Variable | Value |
| --- | --- |
| `DISCORD_BOT_TOKEN` | Your Discord bot token |
| `DISCORD_VOICE_CATEGORY_NAME` | `🎮 Voice Category` or your preferred category |
| `SPRING_DATA_REDIS_HOST` | Reference the Redis service `REDISHOST` variable |
| `SPRING_DATA_REDIS_PORT` | Reference the Redis service `REDISPORT` variable |
| `SPRING_DATA_REDIS_PASSWORD` | Reference the Redis service `REDISPASSWORD` variable |

4. Deploy the bot service.
5. Check logs for successful JDA startup and slash command registration.
6. In Discord, post a `https://gg.riotgames.com/LOL?joinCode=...` link and confirm the bot creates a lobby card and voice room.

## Notes

- Keep exactly one bot replica for the MVP. Multiple replicas can race on Discord events and Redis state transitions.
- No health check path is configured because this is a worker, not an HTTP server.
- If Redis variables are renamed by Railway, map them manually to the `SPRING_DATA_REDIS_*` variables above.
- The `railway.toml` restart policy keeps the worker running after transient failures.
