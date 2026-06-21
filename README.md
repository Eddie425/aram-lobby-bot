# ARAM Lobby Bot

Discord bot MVP for tracking League of Legends ARAM lobby invite links.

## Documentation

- [Technical design](docs/technical-design.md)

## Scope

- Detect `https://gg.riotgames.com/LOL?joinCode=...`
- Create a Discord voice channel under a configured voice category
- Create a voice invite
- Post and update a lobby card with join/leave buttons
- Show active lobbies via `/aram list`
- Let a lobby owner close their latest lobby via `/aram close`
- Delete empty voice channels after the configured grace period

No Riot API, MySQL, ranking, binding, or statistics features are included.

## Required Discord permissions

- Manage Channels
- Create Instant Invite
- View Channels
- Send Messages
- Embed Links
- Use Slash Commands
- Read Message History

The bot also requires the Message Content, Guild Messages, and Guild Voice States intents.

## Run locally

```bash
export DISCORD_BOT_TOKEN=your-token
docker compose up --build
```

For local JVM execution, start Redis first and run:

```bash
mvn spring-boot:run
```

## Deployment options

This bot is a long-running worker process. It needs outbound internet access to Discord, a Redis instance, and a safe place to store `DISCORD_BOT_TOKEN`.

Recommended options for MVP testing:

- Docker on a small VPS: simplest operational model, full control over Redis and logs.
- Railway/Fly.io/Render worker: faster setup, but confirm the plan supports always-on background workers and Redis.
- Home lab/NAS with Docker: fine for a private Discord server test if uptime is acceptable.

Avoid serverless request/response platforms for this MVP because JDA keeps a persistent Discord gateway connection.

## Configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `DISCORD_BOT_TOKEN` | empty | Discord bot token. If empty, the app starts without connecting JDA. |
| `DISCORD_VOICE_CATEGORY_NAME` | `🎮 Voice Category` | Category used for auto-created voice rooms. Created if missing. |
| `ARAM_LOBBY_TTL` | `4h` | Redis TTL for lobby records. |
| `ARAM_CLEANUP_EMPTY_GRACE` | `10s` | How long an empty voice room can remain before deletion. |
| `ARAM_CLEANUP_FIXED_RATE` | `5s` | Cleanup scheduler interval. |
