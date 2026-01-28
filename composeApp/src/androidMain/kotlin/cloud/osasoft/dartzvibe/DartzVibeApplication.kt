package cloud.osasoft.dartzvibe

import android.app.Application
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.local.SettingsFactory
import cloud.osasoft.dartzvibe.di.AppComponent
import cloud.osasoft.dartzvibe.di.create

class DartzVibeApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        val driverFactory = DatabaseDriverFactory(applicationContext)
        val database = DartzVibeDatabase(driverFactory.createDriver())
        val settingsFactory = SettingsFactory(applicationContext)
        val settings = settingsFactory.createSettings()
        appComponent = AppComponent::class.create(database, settings)
    }
}
