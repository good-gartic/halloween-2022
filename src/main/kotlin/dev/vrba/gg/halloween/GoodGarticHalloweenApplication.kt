package dev.vrba.gg.halloween

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableConfigurationProperties(DiscordConfiguration::class)
@SpringBootApplication
class GoodGarticHalloweenApplication

fun main(args: Array<String>) {
    runApplication<GoodGarticHalloweenApplication>(*args)
}
