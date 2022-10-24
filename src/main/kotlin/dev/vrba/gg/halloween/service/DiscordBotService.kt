package dev.vrba.gg.halloween.service

import dev.vrba.gg.halloween.configuration.DiscordConfiguration
import dev.vrba.gg.halloween.configuration.GameConfiguration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.GatewayIntent
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
    commands: CommandService,
    private val service: CollectiblesService,
    private val scheduler: TaskScheduler,
    private val game: GameConfiguration
) : ListenerAdapter() {

    private val jda: JDA = JDABuilder.createDefault(configuration.token)
        .setActivity(Activity.playing(("the Halloween game")))
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .build()
        .awaitReady()

    private val log: TextChannel = jda.getTextChannelById(game.log) ?: throw IllegalStateException("Cannot find the configured log channel")

    init {
        jda.addEventListener(this)
        commands.register(jda)
    }

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun scheduleRandomCollectible() {
        // Delay the post by 0-9 minutes
        val delay = Random.nextInt(0..9)
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
        val channel = jda.getTextChannelById(game.channels.random()) ?: throw IllegalStateException("Cannot find the configured channel")

        channel.sendMessageEmbeds(embed)
            .setActionRow(button)
            .complete()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("collect:")) return

        event.deferEdit().complete()

        val id = event.componentId.removePrefix("collect:").toInt()
        val collectible = service.collectItem(id, event.user.idLong)

        val username = event.member?.nickname ?: event.user.name
        val button = Button.secondary("disabled", "Collected by $username").withDisabled(true)
        val image = emojiToImageUrl(collectible.emoji, 48)
        val base = EmbedBuilder()
            .setColor(0x202225)
            .setTitle(collectible.displayName())
            .setDescription(collectible.description)
            .setAuthor(username, null, event.user.effectiveAvatarUrl)
            .setThumbnail(image)

        event.message
            .editMessageEmbeds(base.build())
            .setActionRow(button)
            .queue()

        log.sendMessageEmbeds(base.addField("Collected in", "<#${event.channel.idLong}>", false).build()).queue()
    }

    private fun emojiToImageUrl(emoji: String, size: Int = 256): String {
        return emoji.codePoints()
            .mapToObj { "https://emojiapi.dev/api/v1/${it.toString(16)}/${size}.png" }
            .toList()
            .first()
    }
}