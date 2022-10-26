package dev.vrba.gg.halloween.service

import dev.vrba.gg.halloween.domain.Collectible
import dev.vrba.gg.halloween.domain.Score
import dev.vrba.gg.halloween.repository.CollectibleRepository
import dev.vrba.gg.halloween.repository.ScoreRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

@Service
class CollectiblesService(
    private val collectibleRepository: CollectibleRepository,
    private val scoreRepository: ScoreRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.qualifiedName)

    fun getRandomCollectible(): Collectible {
        // Weighted random selection
        val collectible = collectibleRepository.findAllAvailableCollectibles()
            .flatMap { collectible -> List(collectible.quantity) { collectible } }
            .random()

        collectibleRepository.save(collectible.copy(quantity = collectible.quantity - 1))

        return collectible
    }

    fun collectItem(id: Int, user: Long): Collectible {
        val collectible = collectibleRepository.findByIdOrNull(id) ?: throw IllegalArgumentException("Cannot find the collectible")
        val score = scoreRepository.findByUser(user) ?: scoreRepository.save(Score(user = user, collection = ""))
        val updated = score.copy(
            points = score.points + collectible.value,
            collection = score.collection + " " + collectible.emoji
        )

        scoreRepository.save(updated)
        logger.info("The user [${user}] has collected the item [${collectible.emoji}]")

        return collectible
    }

    fun getUserScore(user: Long): Score? {
        return scoreRepository.findByUser(user)
    }

    fun getLeaderboard(): List<Score> {
        // Select the top 10 users
        val sort = Sort.by(Sort.Direction.DESC, "points")
        val pagination = PageRequest.of(0, 10, sort)

        return scoreRepository.findAll(pagination).toList()
    }

    fun getRemainingCollectibles(): List<Collectible> {
        return collectibleRepository.findAll().toList()
    }

}