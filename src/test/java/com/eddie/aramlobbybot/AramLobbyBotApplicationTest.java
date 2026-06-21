package com.eddie.aramlobbybot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "discord.bot-token=",
        "aram.cleanup.fixed-rate=1s"
})
class AramLobbyBotApplicationTest {

    @Test
    void contextLoads() {
    }
}
