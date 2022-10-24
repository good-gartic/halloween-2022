package dev.vrba.gg.halloween.service

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CommandService(private val service: CollectiblesService) : ListenerAdapter() {

    private val commands = listOf(
        Commands.slash("points", "View your points and collection of items"),
        Commands.slash("leaderboard", "View the leaderboard")
    )

    fun register(client: JDA) {
        client.addEventListener(this)
        client.updateCommands()
            .addCommands(commands)
            .complete()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "points" -> displayPoints(event)
            "leaderboard" -> displayLeaderboard(event)

            // Shouldn't happen. This is just to make the Kotlin compiler happy
            else -> throw IllegalStateException("Unknown command encountered")
        }
    }

    private fun displayPoints(event: SlashCommandInteractionEvent) {
        val reply = event.deferReply().complete()
        val score = service.getUserScore(event.user.idLong)

        if (score == null) {
            val embed = EmbedBuilder()
                .setColor(0x202225)
                .setTitle("You have not collected any items yet")
                .setDescription("Don't worry, the items will keep appearing until the end of October")
                .setThumbnail(event.user.effectiveAvatarUrl)
                .build()

            return reply.editOriginalEmbeds(embed).queue()
        }

        val embed = EmbedBuilder()
            .setColor(0xf49200)
            .setTitle("You have collected items worth ${score.points} point${if (score.points > 1) "s" else ""}")
            .addField("Your collection", score.collection, false)
            .setThumbnail(event.user.effectiveAvatarUrl)
            .build()

        reply.editOriginalEmbeds(embed).queue()
    }

    private fun displayLeaderboard(event: SlashCommandInteractionEvent) {
        val reply = event.deferReply().complete()
        val leaderboard = service.getLeaderboard()

        val builder = EmbedBuilder()
            .setColor(0xf49200)
            .setTitle("Good Gartic's Halloween leaderboard")
            .setTimestamp(Instant.now())

        val embed = leaderboard.fold(builder) { embed, score ->
            embed.addField(
                "${score.points} point${if (score.points > 1) "s" else ""}",
                "<@${score.user}>",
                false
            )
        }.build()

        reply.editOriginalEmbeds(embed).queue()
    }

}