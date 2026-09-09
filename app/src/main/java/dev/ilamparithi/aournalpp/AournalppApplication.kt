package dev.ilamparithi.aournalpp

import android.app.Application
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.data.X11Preferences

class AournalppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        X11Preferences.initDefaults(this)
        DocumentRepository.init(this)
    }

    companion object {
        lateinit var instance: AournalppApplication
            private set
    }
}
