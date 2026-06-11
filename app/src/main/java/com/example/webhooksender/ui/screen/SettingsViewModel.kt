package com.example.webhooksender.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.webhooksender.data.local.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    val webhookUrl: StateFlow<String> = settingsDataStore.webhookUrl
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val lastKeyword: StateFlow<String> = settingsDataStore.lastKeyword
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    private val _saveResult = MutableStateFlow(false)
    val saveResult: StateFlow<Boolean> = _saveResult.asStateFlow()

    fun saveWebhookUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.saveWebhookUrl(url)
            _saveResult.value = true
        }
    }

    fun resetSaveResult() {
        _saveResult.value = false
    }

    companion object {
        fun provideFactory(settingsDataStore: SettingsDataStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsDataStore) as T
                }
            }
    }
}
