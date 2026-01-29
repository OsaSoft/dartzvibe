package cloud.osasoft.dartzvibe.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repository interface for GameSession data operations.
 */
@OptIn(ExperimentalUuidApi::class)
interface GameRepository {
    fun getAllGameSessions(): Flow<List<GameSession>>

    fun getGameSessionById(id: Uuid): Flow<GameSession?>

    fun getGameSessionsByStatus(status: GameStatus): Flow<List<GameSession>>

    fun getInProgressGame(): Flow<GameSession?>

    suspend fun createGameSession(config: GameConfig): GameSession

    suspend fun updateGameSession(session: GameSession)

    suspend fun deleteGameSession(id: Uuid)
}

/**
 * SQLDelight implementation of GameRepository.
 */
@OptIn(ExperimentalUuidApi::class)
@Inject
class GameRepositoryImpl(
    private val database: DartzVibeDatabase,
) : GameRepository {

    private val log = Logger.withTag("GameRepository")
    private val queries = database.gameSessionQueries
    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllGameSessions(): Flow<List<GameSession>> {
        log.d { "Fetching all game sessions" }
        return queries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toGameSession() } }
    }

    override fun getGameSessionById(id: Uuid): Flow<GameSession?> {
        log.d { "Fetching game session by id: $id" }
        return queries
            .selectById(id.toString())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toGameSession() }
    }

    override fun getGameSessionsByStatus(status: GameStatus): Flow<List<GameSession>> {
        log.d { "Fetching game sessions by status: $status" }
        return queries
            .selectByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toGameSession() } }
    }

    override fun getInProgressGame(): Flow<GameSession?> {
        log.d { "Fetching in-progress game" }
        return queries
            .selectByStatus(GameStatus.IN_PROGRESS.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.firstOrNull()?.toGameSession() }
    }

    override suspend fun createGameSession(config: GameConfig): GameSession {
        val session = GameSession(
            id = Uuid.random(),
            config = config,
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
            finishedAt = null,
            winnerId = null,
        )

        log.d { "Creating game session: ${session.id}" }
        withContext(Dispatchers.IO) {
            queries.insert(
                id = session.id.toString(),
                gameType = session.config.gameType.name,
                gameMode = session.config.gameMode.name,
                doubleIn = if (session.config.doubleIn) 1L else 0L,
                doubleOut = if (session.config.doubleOut) 1L else 0L,
                playerIds = session.config.playerIds.joinToString(",") { it.toString() },
                legsToWin = session.config.legsToWin.toLong(),
                legsJson = json.encodeToString(session.legs),
                currentLegIndex = session.currentLegIndex.toLong(),
                status = session.status.name,
                startedAt = session.startedAt,
                finishedAt = session.finishedAt,
                winnerId = session.winnerId?.toString(),
                cricketSegments = config.cricketSegments?.let { json.encodeToString(it) },
            )
        }
        return session
    }

    override suspend fun updateGameSession(session: GameSession) {
        log.d { "Updating game session: ${session.id}" }
        withContext(Dispatchers.IO) {
            queries.update(
                legsJson = json.encodeToString(session.legs),
                currentLegIndex = session.currentLegIndex.toLong(),
                status = session.status.name,
                finishedAt = session.finishedAt,
                winnerId = session.winnerId?.toString(),
                id = session.id.toString(),
            )
        }
    }

    override suspend fun deleteGameSession(id: Uuid) {
        log.d { "Deleting game session: $id" }
        withContext(Dispatchers.IO) {
            queries.delete(id.toString())
        }
    }

    private fun cloud.osasoft.dartzvibe.data.local.GameSession.toGameSession(): GameSession {
        val playerIdList = playerIds.split(",").map { Uuid.parse(it.trim()) }
        val legsList: List<Leg> = json.decodeFromString(legsJson)

        return GameSession(
            id = Uuid.parse(id),
            config = GameConfig(
                gameType = GameType.valueOf(gameType),
                gameMode = GameMode.valueOf(gameMode),
                doubleIn = doubleIn != 0L,
                doubleOut = doubleOut != 0L,
                playerIds = playerIdList,
                legsToWin = legsToWin.toInt(),
                cricketSegments = cricketSegments?.let { json.decodeFromString<CricketSegments>(it) },
            ),
            legs = legsList,
            currentLegIndex = currentLegIndex.toInt(),
            status = GameStatus.valueOf(status),
            startedAt = startedAt,
            finishedAt = finishedAt,
            winnerId = winnerId?.let { Uuid.parse(it) },
        )
    }
}
