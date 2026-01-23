package cloud.osasoft.dartzvibe.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = DartzVibeDatabase.Schema,
        name = "dartzvibe.db",
    )
}
