package dev.vrba.gg.halloween

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DiscordConfiguration::class)
class GoodGarticHalloweenApplication

fun main(args: Array<String>) {
    runApplication<GoodGarticHalloweenApplication>(*args)
}
