package dev.vrba.gg.halloween.repository

import dev.vrba.gg.halloween.domain.Score
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface ScoreRepository : PagingAndSortingRepository<Score, Int> {

    fun findByUser(user: Long): Score?

}
