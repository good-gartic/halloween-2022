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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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

    private val lock: ReentrantLock = ReentrantLock()

    private val resolvedInteractions: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    init {
        jda.addEventListener(this)
        commands.register(jda)
    }

    @Scheduled(initialDelay = 0, fixedRate = 5, timeUnit = TimeUnit.MINUTES)
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

        val buttons = listOf("collect:${collectible.id}", "fake:1", "fake:2")
            .shuffled()
            .zip(listOf("\uD83D\uDE48", "\uD83D\uDE4A", "\uD83D\uDE49"))
            .map { (id, emoji) -> Button.secondary(id, emoji) }

        val channel = jda.getTextChannelById(game.channels.random()) ?: throw IllegalStateException("Cannot find the configured channel")
        val message = channel.sendMessageEmbeds(embed)
            .setActionRow(buttons)
            .complete()

        val expiration = Instant.now() + Duration.ofSeconds(30)

        // Release the collectible interaction and delete the message
        scheduler.schedule(
            {
                resolvedInteractions -= message.idLong
                message.delete().queue()
            },
            expiration
        )
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId.startsWith("fake")) {
            val reply = event.deferReply(true).complete()
            val embed = EmbedBuilder()
                .setColor(0xED4245)
                .setTitle("Oh no, wrong button!")
                .setDescription("Better luck next time")
                .build()

            return reply.editOriginalEmbeds(embed).queue()
        }

        if (!event.componentId.startsWith("collect:")) return

        lock.withLock {
            // Another user has already collected the collectible
            if (resolvedInteractions.contains(event.message.idLong)) {
                val interaction = event.deferReply(true).complete()
                val embed = EmbedBuilder()
                    .setColor(0xED4245)
                    .setTitle("Oh no! Somebody was faster")
                    .setDescription("Better luck next time, cowboy")
                    .setThumbnail("https://i.imgur.com/4cLpKnn.png")
                    .build()

                service.addMissedCollectible(event.user.idLong)

                return interaction.editOriginalEmbeds(embed).queue()
            }

            // Lock the collectible interaction
            resolvedInteractions += event.message.idLong

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
    }

    private fun emojiToImageUrl(emoji: String, size: Int = 256): String {
        return emoji.codePoints()
            .mapToObj { "https://emojiapi.dev/api/v1/${it.toString(16)}/${size}.png" }
            .toList()
            .first()
    }
}