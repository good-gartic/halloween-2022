package dev.vrba.gg.halloween

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import dev.vrba.gg.halloween.configuration.GameConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(DiscordConfiguration::class, GameConfiguration::class)
class GoodGarticHalloweenApplication

fun main(args: Array<String>) {
    runApplication<GoodGarticHalloweenApplication>(*args)
}
