package com.appgrouplab.firstlast.model

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val dateTimeIso: String = "",
    val homePosition: Int = 0,
    val homeTeam: String = "",
    val season: String = "",
    val visitingPosition: Int = 0,
    val visitingTeam: String = "",
    val leagueSize: Int = 20
)
