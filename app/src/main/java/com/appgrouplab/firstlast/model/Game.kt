package com.appgrouplab.firstlast.model

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val league: League = League(),
    val home: TeamEntry = TeamEntry(),
    val away: TeamEntry = TeamEntry(),
    val dateTimeIso: String = "",
    val leagueSize: Int = 20
)

@Serializable
data class League(
    val name: String = "",
    val key: String = ""
)

@Serializable
data class TeamEntry(
    val name: String = "",
    val key: String = "",
    val pos: Int = 0
)
