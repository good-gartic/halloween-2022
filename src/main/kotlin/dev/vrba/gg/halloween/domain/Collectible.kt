package dev.vrba.gg.halloween.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("collectibles")
data class Collectible(
    @Id
    @Column("id")
    val id: Int = 0,

    @Column("emoji")
    val emoji: String,

    @Column("name")
    val name: String,

    @Column("description")
    val description: String,

    @Column("value")
    val value: Int,

    @Column("quantity")
    val quantity: Int,
)