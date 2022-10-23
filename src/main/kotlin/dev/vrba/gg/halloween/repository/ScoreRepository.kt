package dev.vrba.gg.halloween.repository

import dev.vrba.gg.halloween.domain.Score
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ScoreRepository : CrudRepository<Score, Int> {

    fun findByUser(user: Long): Score?

}
