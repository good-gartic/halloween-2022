package dev.vrba.gg.halloween.service

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextInt

@Service
class DiscordBotService(
    configuration: DiscordConfiguration,
    private val service: CollectiblesService,
    private val scheduler: TaskScheduler
) {

    private val jda: JDA = JDABuilder.createDefault(configuration.token)
        .build()
        .awaitReady()

    private val channel: TextChannel = jda.getTextChannelById(configuration.channel)
        ?: throw IllegalStateException("Cannot find the configured Discord channel")

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun scheduleRandomCollectible() {
        // Delay the post by 0-59 minutes
        val delay = Random.nextInt(0..59)
        val start = Instant.now() + Duration.ofMinutes(delay.toLong())

        scheduler.schedule(this::sendCollectible, start)
    }

    private fun sendCollectible() {
        val collectible = service.getRandomCollectible()
        val image = collectible.emoji.codePoints()
            .mapToObj { "https://emojiapi.dev/api/v1/${it.toString(16)}/256.png" }
            .toList()
            .first()

        val embed = EmbedBuilder()
            .setColor(0xf49200)
            .setTitle(collectible.emoji + " " + collectible.name)
            .setDescription(collectible.description)
            .setFooter("⭐".repeat(collectible.value))
            .setImage(image)
            .build()

        val button = Button.secondary("collect:${collectible.id}", "Collect")

        channel.sendMessageEmbeds(embed)
            .setActionRow(button)
            .queue()
    }
}