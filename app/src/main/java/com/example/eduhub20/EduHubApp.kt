package com.example.eduhub20

import android.app.Application
import com.example.eduhub20.data.local.EduHubLocalStorage

class EduHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EduHubLocalStorage.init(this)
    }
}