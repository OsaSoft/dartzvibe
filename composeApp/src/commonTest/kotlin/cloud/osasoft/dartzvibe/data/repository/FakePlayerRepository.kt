package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of PlayerRepository for testing purposes.
 */
@OptIn(ExperimentalUuidApi::class)
class FakePlayerRepository : PlayerRepository {

    private val players = MutableStateFlow<Map<Uuid, Player>>(emptyMap())

    override fun getAllPlayers(): Flow<List<Player>> {
        return players.map { it.values.toList().sortedBy { p -> p.name } }
    }

    override fun getPlayerById(id: Uuid): Flow<Player?> {
        return players.map { it[id] }
    }

    override suspend fun insertPlayer(player: Player) {
        players.value = players.value + (player.id to player)
    }

    override suspend fun updatePlayer(player: Player) {
        players.value = players.value + (player.id to player)
    }

    override suspend fun deletePlayer(id: Uuid) {
        players.value = players.value - id
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

    // Test helpers
    fun clear() {
        players.value = emptyMap()
    }

    fun getPlayerCount(): Int = players.value.size
}
