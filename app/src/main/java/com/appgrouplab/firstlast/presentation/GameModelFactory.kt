package com.appgrouplab.firstlast.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.appgrouplab.firstlast.data.GameRepository
import kotlinx.coroutines.Dispatchers

@Suppress("UNCHECKED_CAST")
class GameModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            GameViewModel(
                application  = application,
                repository   = GameRepository(),
                dispatcher   = Dispatchers.IO,
            ) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}
