package dev.vrba.gg.halloween.repository

import dev.vrba.gg.halloween.domain.Collectible
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CollectibleRepository : CrudRepository<Collectible, Int> {

    @Query("select * from collectibles where quantity > 0")
    fun findAllAvailableCollectibles(): List<Collectible>

}