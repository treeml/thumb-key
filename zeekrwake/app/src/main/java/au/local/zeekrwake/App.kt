package au.local.zeekrwake

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.appCtx = applicationContext
    }
}
