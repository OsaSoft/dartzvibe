package cloud.osasoft.dartzvibe.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating platform-specific SQLDelight database drivers.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
