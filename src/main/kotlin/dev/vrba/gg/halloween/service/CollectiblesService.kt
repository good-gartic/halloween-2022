package dev.vrba.gg.halloween.service

import dev.vrba.gg.halloween.domain.Collectible
import dev.vrba.gg.halloween.domain.Score
import dev.vrba.gg.halloween.repository.CollectibleRepository
import dev.vrba.gg.halloween.repository.ScoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

@Service
class CollectiblesService(
    private val collectibleRepository: CollectibleRepository,
    private val scoreRepository: ScoreRepository
) {
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

        return collectible
    }

    fun getUserScore(user: Long): Score? {
        return scoreRepository.findByUser(user)
    }

}