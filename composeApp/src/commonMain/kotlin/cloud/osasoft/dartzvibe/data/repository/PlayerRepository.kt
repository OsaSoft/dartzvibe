package cloud.osasoft.dartzvibe.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.model.Player
import co.touchlab.kermit.Logger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import cloud.osasoft.dartzvibe.util.currentTimeMillis

/**
 * Repository interface for Player data operations.
 */
@OptIn(ExperimentalUuidApi::class)
interface PlayerRepository {
    fun getAllPlayers(): Flow<List<Player>>
    fun getPlayerById(id: Uuid): Flow<Player?>
    suspend fun insertPlayer(player: Player)
    suspend fun updatePlayer(player: Player)
    suspend fun deletePlayer(id: Uuid)
    suspend fun createPlayer(name: String, nickname: String?, avatarColor: Int): Player
}

/**
 * SQLDelight implementation of PlayerRepository.
 */
@OptIn(ExperimentalUuidApi::class)
@Inject
class PlayerRepositoryImpl(
    private val database: DartzVibeDatabase
) : PlayerRepository {

    private val log = Logger.withTag("PlayerRepository")
    private val queries = database.playerQueries

    override fun getAllPlayers(): Flow<List<Player>> {
        log.d { "Fetching all players" }
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toPlayer() } }
    }

    override fun getPlayerById(id: Uuid): Flow<Player?> {
        log.d { "Fetching player by id: $id" }
        return queries.selectById(id.toString())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toPlayer() }
    }

    override suspend fun insertPlayer(player: Player) {
        log.d { "Inserting player: ${player.name}" }
        withContext(Dispatchers.IO) {
            queries.insert(
                id = player.id.toString(),
                name = player.name,
                nickname = player.nickname,
                avatarColor = player.avatarColor.toLong(),
                createdAt = player.createdAt
            )
        }
    }

    override suspend fun updatePlayer(player: Player) {
        log.d { "Updating player: ${player.name}" }
        withContext(Dispatchers.IO) {
            queries.update(
                name = player.name,
                nickname = player.nickname,
                avatarColor = player.avatarColor.toLong(),
                id = player.id.toString()
            )
        }
    }

    override suspend fun deletePlayer(id: Uuid) {
        log.d { "Deleting player: $id" }
        withContext(Dispatchers.IO) {
            queries.delete(id.toString())
        }
    }

    override suspend fun createPlayer(name: String, nickname: String?, avatarColor: Int): Player {
        val player = Player(
            id = Uuid.random(),
            name = name,
            nickname = nickname,
            avatarColor = avatarColor,
            createdAt = currentTimeMillis()
        )
        insertPlayer(player)
        return player
    }

    private fun cloud.osasoft.dartzvibe.data.local.Player.toPlayer(): Player {
        return Player(
            id = Uuid.parse(id),
            name = name,
            nickname = nickname,
            avatarColor = avatarColor.toInt(),
            createdAt = createdAt
        )
    }
}
