package com.appgrouplab.firstlast.presentation

import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.model.League

sealed class GameUiState {
    object Loading : GameUiState()
    data class Success(
        val games: List<Game>,
        val leagues: List<League>
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
