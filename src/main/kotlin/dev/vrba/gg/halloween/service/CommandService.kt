package dev.vrba.gg.halloween.service

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CommandService(private val service: CollectiblesService) : ListenerAdapter() {

    private val commands = listOf(
        Commands.slash("leaderboard", "View the leaderboard"),
        Commands.slash("points", "View points and collection of an user")
            .addOption(OptionType.USER, "user", "Defaults to you, if left out or blank", false)
    )

    fun register(client: JDA) {
        client.addEventListener(this)
        client.updateCommands()
            .addCommands(commands)
            .complete()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val reply = event.deferReply().complete()
        val embed = when (event.name) {
            "points" -> displayPoints(event.getOption("user")?.asUser ?: event.user)
            "leaderboard" -> displayLeaderboard()
            "remaining" -> displayRemainingCollectibles()

            // Shouldn't happen. This is just to make the Kotlin compiler happy
            else -> throw IllegalStateException("Unknown command encountered")
        }

        reply.editOriginalEmbeds(embed).queue()
    }

    @Suppress("MoveVariableDeclarationIntoWhen")
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val content = event.message.contentRaw

        if (content.startsWith("h.")) {
            val command = content.removePrefix("h.").trim().split(" ").first()
            val embed = when (command) {
                "points" -> displayPoints(event.message.mentions.users.firstOrNull() ?: event.message.author)
                "leaderboard" -> displayLeaderboard()
                "remaining" -> displayRemainingCollectibles()

                // Shouldn't happen. This is again just to make the Kotlin compiler happy
                else -> throw IllegalStateException("Unknown command encountered")
            }

            event.message.replyEmbeds(embed).queue()
        }
    }

    private fun displayPoints(user: User): MessageEmbed {
        val score = service.getUserScore(user.idLong) ?: return EmbedBuilder()
            .setColor(0x202225)
            .setTitle("You have not collected any items yet")
            .setDescription("Don't worry, the items will keep appearing until the end of October")
            .setThumbnail(user.effectiveAvatarUrl)
            .build()

        return EmbedBuilder()
            .setColor(0xf49200)
            .setTitle("You have collected items worth ${score.points} point${if (score.points > 1) "s" else ""}")
            .addField("Your collection", score.collection, false)
            .setThumbnail(user.effectiveAvatarUrl)
            .build()
    }

    private fun displayLeaderboard(): MessageEmbed {
        val leaderboard = service.getLeaderboard()

        val builder = EmbedBuilder()
            .setColor(0xf49200)
            .setTitle("Good Gartic's Halloween leaderboard")
            .setTimestamp(Instant.now())

        return leaderboard.fold(builder) { embed, score ->
            embed.addField(
                "${score.points} point${if (score.points > 1) "s" else ""}",
                "<@${score.user}>",
                false
            )
        }.build()
    }

    private fun displayRemainingCollectibles(): MessageEmbed {
        val remaining = service.getRemainingCollectibles()

        val emojis = remaining
            .flatMap { collectible -> List(collectible.quantity) { collectible.emoji } }
            .shuffled()
            .joinToString(" ")

        return EmbedBuilder()
            .setColor(0xf49200)
            .setTitle("Remaining collectibles")
            .setDescription(emojis)
            .setTimestamp(Instant.now())
            .build()
    }
}