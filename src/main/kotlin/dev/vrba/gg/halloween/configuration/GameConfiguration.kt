package dev.vrba.gg.halloween.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "spring.game")
class GameConfiguration(
    val log: Long,
    val channels: List<Long>,
)