package dev.vrba.gg.halloween.repository

import dev.vrba.gg.halloween.domain.Collectible
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CollectibleRepository : CrudRepository<Collectible, Int>