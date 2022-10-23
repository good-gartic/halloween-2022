package dev.vrba.gg.halloween.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("score")
data class Score(
    @Id
    @Column("id")
    val id: Int = 0,

    @Column("user_id")
    val user: Long,

    @Column("points")
    val points: Int = 0
)