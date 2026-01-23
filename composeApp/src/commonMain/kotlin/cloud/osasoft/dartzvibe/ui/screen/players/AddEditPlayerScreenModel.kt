package cloud.osasoft.dartzvibe.ui.screen.players

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import co.touchlab.kermit.Logger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalUuidApi::class)
data class AddEditPlayerState(
    val name: String = "",
    val nickname: String = "",
    val avatarColor: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val playerId: Uuid? = null,
    val saveCompleted: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank()
}

@OptIn(ExperimentalUuidApi::class)
class AddEditPlayerScreenModel(
    private val playerRepository: PlayerRepository,
    private val playerId: Uuid? = null
) : ScreenModel {

    private val log = Logger.withTag("AddEditPlayerScreenModel")

    private val _state = MutableStateFlow(AddEditPlayerState(
        isEditMode = playerId != null,
        playerId = playerId
    ))
    val state: StateFlow<AddEditPlayerState> = _state.asStateFlow()

    init {
        if (playerId != null) {
            loadPlayer(playerId)
        }
    }

    private fun loadPlayer(id: Uuid) {
        _state.update { it.copy(isLoading = true) }
        screenModelScope.launch {
            try {
                val player = playerRepository.getPlayerById(id).first()
                if (player != null) {
                    _state.update {
                        it.copy(
                            name = player.name,
                            nickname = player.nickname ?: "",
                            avatarColor = player.avatarColor,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Player not found") }
                }
            } catch (e: Exception) {
                log.e(e) { "Error loading player" }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateName(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun updateNickname(nickname: String) {
        _state.update { it.copy(nickname = nickname) }
    }

    fun updateAvatarColor(colorIndex: Int) {
        _state.update { it.copy(avatarColor = colorIndex) }
    }

    fun save() {
        val currentState = _state.value
        if (!currentState.isValid) {
            _state.update { it.copy(error = "Name is required") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }
        screenModelScope.launch {
            try {
                if (currentState.isEditMode && currentState.playerId != null) {
                    // Update existing player
                    val existingPlayer = playerRepository.getPlayerById(currentState.playerId).first()
                    if (existingPlayer != null) {
                        val updatedPlayer = existingPlayer.copy(
                            name = currentState.name.trim(),
                            nickname = currentState.nickname.trim().takeIf { it.isNotEmpty() },
                            avatarColor = currentState.avatarColor
                        )
                        playerRepository.updatePlayer(updatedPlayer)
                        log.d { "Updated player: ${updatedPlayer.name}" }
                    }
                } else {
                    // Create new player
                    playerRepository.createPlayer(
                        name = currentState.name.trim(),
                        nickname = currentState.nickname.trim().takeIf { it.isNotEmpty() },
                        avatarColor = currentState.avatarColor
                    )
                    log.d { "Created new player: ${currentState.name}" }
                }
                _state.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                log.e(e) { "Error saving player" }
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
