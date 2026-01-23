package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of GameRepository for testing purposes.
 */
@OptIn(ExperimentalUuidApi::class)
class FakeGameRepository : GameRepository {

    private val sessions = MutableStateFlow<Map<Uuid, GameSession>>(emptyMap())

    override fun getAllGameSessions(): Flow<List<GameSession>> {
        return sessions.map { it.values.toList().sortedByDescending { s -> s.startedAt } }
    }

    override fun getGameSessionById(id: Uuid): Flow<GameSession?> {
        return sessions.map { it[id] }
    }

    override fun getGameSessionsByStatus(status: GameStatus): Flow<List<GameSession>> {
        return sessions.map { map ->
            map.values.filter { it.status == status }.sortedByDescending { it.startedAt }
        }
    }

    override fun getInProgressGame(): Flow<GameSession?> {
        return sessions.map { map ->
            map.values.find { it.status == GameStatus.IN_PROGRESS }
        }
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
            winnerId = null
        )
        sessions.value = sessions.value + (session.id to session)
        return session
    }

    override suspend fun updateGameSession(session: GameSession) {
        sessions.value = sessions.value + (session.id to session)
    }

    override suspend fun deleteGameSession(id: Uuid) {
        sessions.value = sessions.value - id
    }

    // Test helpers
    fun clear() {
        sessions.value = emptyMap()
    }

    fun getSessionCount(): Int = sessions.value.size

    suspend fun insertSession(session: GameSession) {
        sessions.value = sessions.value + (session.id to session)
    }
}
