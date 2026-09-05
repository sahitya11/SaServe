package com.servicesync.app

import android.app.Application
import com.servicesync.app.data.repository.ServiceSyncRepository
import com.servicesync.app.notification.NotificationHelper

class ServiceSyncApp : Application() {

    lateinit var repository: ServiceSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ServiceSyncRepository.getInstance(this)
        NotificationHelper.createNotificationChannels(this)
    }
}
