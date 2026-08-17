package com.nightshift.tracker

import android.app.Application
import com.nightshift.tracker.alarm.TimerAlarms
import com.nightshift.tracker.data.AppDatabase
import com.nightshift.tracker.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NightshiftApp : Application() {
    // Application-scoped: outlives every activity; only dies with the process,
    // at which point all data is already committed to Room anyway.
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this, AppDatabase.get(this), appScope)
        TimerAlarms.ensureChannel(this)
    }
}
