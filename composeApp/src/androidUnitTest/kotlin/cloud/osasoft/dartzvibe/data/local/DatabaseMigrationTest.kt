package cloud.osasoft.dartzvibe.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Upgrades real SQLite databases from every schema version that has existed on a device to the current
 * one. The starting schemas are copied from git history, so they don't change when the `.sq` files do.
 *
 * Lives in androidUnitTest because [JdbcSqliteDriver] is JVM-only.
 */
class DatabaseMigrationTest : FreeSpec({

    "Should upgrade a database from each historical schema version to the current schema" - {
        withData(
            nameFn = { "from schema v${it.version}" },
            HISTORICAL_SCHEMAS,
        ) { historical ->
            // GIVEN a database created by an old build, holding a player and a finished game
            val driver = inMemoryDriver()
            historical.createStatements.forEach { driver.execute(null, it, 0) }
            driver.setUserVersion(historical.version)
            driver.execute(
                identifier = null,
                sql = "INSERT INTO Player (id, name, nickname, avatarColor, createdAt) " +
                    "VALUES ('p1', 'Alice', 'Ace', 3, 1000)",
                parameters = 0,
            )
            driver.execute(
                identifier = null,
                sql = "INSERT INTO GameSession (id, gameType, doubleIn, doubleOut, playerIds, legsToWin, legsJson, " +
                    "currentLegIndex, status, startedAt, finishedAt, winnerId) " +
                    "VALUES ('g1', 'X501', 0, 1, 'p1', 1, '[]', 0, 'COMPLETED', 2000, 3000, 'p1')",
                parameters = 0,
            )

            // WHEN the current build opens it and migrates
            DartzVibeDatabase.Schema.migrate(driver, historical.version, DartzVibeDatabase.Schema.version)

            // THEN its tables match a freshly created database
            val fresh = inMemoryDriver().also { DartzVibeDatabase.Schema.create(it) }
            TABLES.forEach { table ->
                driver.columns(table) shouldContainExactlyInAnyOrder fresh.columns(table)
            }

            // AND the old rows are still there and readable through the generated queries
            val database = DartzVibeDatabase(driver)
            val player = database.playerQueries.selectById("p1").executeAsOne()
            player.name shouldBe "Alice"
            player.nickname shouldBe "Ace"
            player.avatarColor shouldBe 3L
            player.createdAt shouldBe 1000L

            val game = database.gameSessionQueries.selectById("g1").executeAsOne()
            game.gameType shouldBe "X501"
            game.gameMode shouldBe "CLASSIC"
            game.doubleOut shouldBe 1L
            game.playerIds shouldBe "p1"
            game.status shouldBe "COMPLETED"
            game.startedAt shouldBe 2000L
            game.finishedAt shouldBe 3000L
            game.winnerId shouldBe "p1"
            game.cricketSegments.shouldBeNull()
            game.checkoutPracticeTargets.shouldBeNull()
            game.rouletteTargetSegments.shouldBeNull()
            game.rouletteRounds.shouldBeNull()
            game.rouletteTargetScore.shouldBeNull()

            driver.close()
            fresh.close()
        }
    }

    // Consistency check only: no device takes this path (new installs run Schema.create).
    "Should build the current schema from an empty database by migrations alone" {
        // GIVEN an empty database
        val driver = inMemoryDriver()

        // WHEN all migrations run from version 1
        DartzVibeDatabase.Schema.migrate(driver, 1, DartzVibeDatabase.Schema.version)

        // THEN the tables match a freshly created database
        val fresh = inMemoryDriver().also { DartzVibeDatabase.Schema.create(it) }
        TABLES.forEach { table ->
            driver.columns(table) shouldContainExactlyInAnyOrder fresh.columns(table)
        }
        driver.close()
        fresh.close()
    }
})

private data class HistoricalSchema(val version: Long, val createStatements: List<String>)

private data class Column(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val default: String?,
    val pk: Boolean,
)

private val TABLES = listOf("GameSession", "Player")

private fun inMemoryDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

private fun SqlDriver.setUserVersion(version: Long) {
    execute(null, "PRAGMA user_version = $version", 0)
}

private fun SqlDriver.columns(table: String): List<Column> = executeQuery(
    identifier = null,
    sql = "PRAGMA table_info($table)",
    mapper = { cursor ->
        val columns = mutableListOf<Column>()
        while (cursor.next().value) {
            columns += Column(
                name = cursor.getString(1)!!,
                type = cursor.getString(2)!!,
                notNull = cursor.getLong(3) == 1L,
                default = cursor.getString(4),
                pk = cursor.getLong(5)!! > 0L,
            )
        }
        QueryResult.Value(columns)
    },
    parameters = 0,
).value

private val PLAYER_V1 = """
    CREATE TABLE Player (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        nickname TEXT,
        avatarColor INTEGER NOT NULL DEFAULT 0,
        createdAt INTEGER NOT NULL
    )
""".trimIndent()

// Schema version = highest migrations/N.sqm + 1. No database was ever at version 2: the first
// migration file (2.sqm) moved the version from 1 straight to 3.
private val HISTORICAL_SCHEMAS = listOf(
    // 4ba78c7 "add player management": no migration files yet
    HistoricalSchema(
        version = 1,
        createStatements = listOf(
            """
            CREATE TABLE GameSession (
                id TEXT NOT NULL PRIMARY KEY,
                gameType TEXT NOT NULL,
                doubleIn INTEGER NOT NULL DEFAULT 0,
                doubleOut INTEGER NOT NULL DEFAULT 1,
                playerIds TEXT NOT NULL,
                legsToWin INTEGER NOT NULL DEFAULT 1,
                legsJson TEXT NOT NULL,
                currentLegIndex INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                winnerId TEXT
            )
            """.trimIndent(),
            PLAYER_V1,
        ),
    ),
    // 8013822 "add gametype parcheesi": 2.sqm
    HistoricalSchema(
        version = 3,
        createStatements = listOf(
            """
            CREATE TABLE GameSession (
                id TEXT NOT NULL PRIMARY KEY,
                gameType TEXT NOT NULL,
                gameMode TEXT NOT NULL DEFAULT 'CLASSIC',
                doubleIn INTEGER NOT NULL DEFAULT 0,
                doubleOut INTEGER NOT NULL DEFAULT 1,
                playerIds TEXT NOT NULL,
                legsToWin INTEGER NOT NULL DEFAULT 1,
                legsJson TEXT NOT NULL,
                currentLegIndex INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                winnerId TEXT
            )
            """.trimIndent(),
            PLAYER_V1,
        ),
    ),
    // 8e9a231 "add random cricket": 3.sqm
    HistoricalSchema(
        version = 4,
        createStatements = listOf(
            """
            CREATE TABLE GameSession (
                id TEXT NOT NULL PRIMARY KEY,
                gameType TEXT NOT NULL,
                gameMode TEXT NOT NULL DEFAULT 'CLASSIC',
                doubleIn INTEGER NOT NULL DEFAULT 0,
                doubleOut INTEGER NOT NULL DEFAULT 1,
                playerIds TEXT NOT NULL,
                legsToWin INTEGER NOT NULL DEFAULT 1,
                legsJson TEXT NOT NULL,
                currentLegIndex INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                winnerId TEXT,
                cricketSegments TEXT
            )
            """.trimIndent(),
            PLAYER_V1,
        ),
    ),
    // 139d866 "add checkout practice mode for solo training": 4.sqm
    HistoricalSchema(
        version = 5,
        createStatements = listOf(
            """
            CREATE TABLE GameSession (
                id TEXT NOT NULL PRIMARY KEY,
                gameType TEXT NOT NULL,
                gameMode TEXT NOT NULL DEFAULT 'CLASSIC',
                doubleIn INTEGER NOT NULL DEFAULT 0,
                doubleOut INTEGER NOT NULL DEFAULT 1,
                playerIds TEXT NOT NULL,
                legsToWin INTEGER NOT NULL DEFAULT 1,
                legsJson TEXT NOT NULL,
                currentLegIndex INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                winnerId TEXT,
                cricketSegments TEXT,
                checkoutPracticeTargets TEXT
            )
            """.trimIndent(),
            PLAYER_V1,
        ),
    ),
)
