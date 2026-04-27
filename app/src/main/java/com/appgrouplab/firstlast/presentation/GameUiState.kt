package com.appgrouplab.firstlast.presentation

import com.appgrouplab.firstlast.model.Game

sealed class GameUiState {
    object Loading : GameUiState()
    data class Success(val games: List<Game>) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
