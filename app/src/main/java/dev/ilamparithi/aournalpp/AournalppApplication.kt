package dev.ilamparithi.aournalpp

import android.app.Application
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.data.X11Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AournalppApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        X11Preferences.initDefaults(this)
        DocumentRepository.init(this)

        // Pre-warm encrypted credential vault and execute pending service purges on IO
        applicationScope.launch {
            val vault = CredentialsVault.getInstance(this@AournalppApplication)
            vault.getAllServices()
            vault.getPendingDeletedServiceIds()
            vault.getExclusionFilter()
            vault.purgePendingDeletedServices(this@AournalppApplication)
        }
    }

    companion object {
        lateinit var instance: AournalppApplication
            private set

        val applicationScope: CoroutineScope
            get() = instance.applicationScope
    }
}
