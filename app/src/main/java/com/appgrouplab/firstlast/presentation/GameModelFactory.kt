package com.appgrouplab.firstlast.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.appgrouplab.firstlast.data.GeminiGameRepository
import kotlinx.coroutines.Dispatchers

@Suppress("UNCHECKED_CAST")
class GameModelFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            GameViewModel(
                repository = GeminiGameRepository(context),
                dispatcher = Dispatchers.IO,
            ) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}
