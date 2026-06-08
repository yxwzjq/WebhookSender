package com.example.webhooksender

import android.app.Application
import com.example.webhooksender.data.local.AppDatabase
import com.example.webhooksender.data.local.SettingsDataStore
import com.example.webhooksender.data.remote.WebhookApiService
import com.example.webhooksender.data.repository.MessageRepository

class WebhookApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val application: Application) {
    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(application)
    }
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(application)
    }
    val webhookApiService: WebhookApiService by lazy {
        WebhookApiService()
    }
    val messageRepository: MessageRepository by lazy {
        MessageRepository(database.messageDao(), webhookApiService)
    }
}
