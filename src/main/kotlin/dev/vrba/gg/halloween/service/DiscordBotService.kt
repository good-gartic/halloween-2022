package dev.vrba.gg.halloween.service

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
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
final class DiscordBotService(
    configuration: DiscordConfiguration,
    private val service: CollectiblesService,
    private val scheduler: TaskScheduler
) : ListenerAdapter() {

    private val jda: JDA = JDABuilder.createDefault(configuration.token)
        .setActivity(Activity.playing(("the Halloween game")))
        .build()
        .awaitReady()

    private val channel: TextChannel = jda.getTextChannelById(configuration.channel)
        ?: throw IllegalStateException("Cannot find the configured Discord channel")

    init {
        jda.addEventListener(this)
    }

    @Scheduled(initialDelay = 0, fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    fun scheduleRandomCollectible() {
        // Delay the post by 0-29 minutes
        val delay = 0 // Random.nextInt(0..29)
        val start = Instant.now() + Duration.ofMinutes(delay.toLong())

        scheduler.schedule(this::sendCollectible, start)
    }

    private fun sendCollectible() {
        val collectible = service.getRandomCollectible()

        val user = jda.selfUser
        val image = emojiToImageUrl(collectible.emoji)
        val embed = EmbedBuilder()
            .setColor(0xf49200)
            .setTitle(collectible.displayName())
            .setDescription(collectible.description)
            .setFooter(user.name, user.effectiveAvatarUrl)
            .setImage(image)
            .build()

        val button = Button.secondary("collect:${collectible.id}", "Collect")

        channel.sendMessageEmbeds(embed)
            .setActionRow(button)
            .queue()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("collect:")) return

        event.deferEdit().complete()

        val id = event.componentId.removePrefix("collect:").toInt()
        val user = event.user
        val collectible = service.collectItem(id, user.idLong)

        val button = Button.secondary("collect:-", "Collected by ${user.name}").withDisabled(true)
        val image = emojiToImageUrl(collectible.emoji, 64)
        val embed = EmbedBuilder()
            .setColor(0x202225)
            .setTitle(collectible.displayName())
            .setDescription(collectible.description)
            .setThumbnail(image)
            .build()

        event.message.editMessage(" ")
            .setEmbeds(embed)
            .setActionRow(button)
            .queue()
    }

    private fun emojiToImageUrl(emoji: String, size: Int = 256): String {
        return emoji.codePoints()
            .mapToObj { "https://emojiapi.dev/api/v1/${it.toString(16)}/${size}.png" }
            .toList()
            .first()
    }
}