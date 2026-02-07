package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.Throw
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
internal sealed interface ModeEngine {
    val session: GameSession
    val currentTurnThrows: List<Throw>

    fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult>
    fun endTurn(): Pair<ModeEngine, TurnResult>
    fun undoLastThrow(): ModeEngine?
    fun isTurnEnded(): Boolean
    fun getCurrentPlayerScore(): Int
    fun withNonScoringThrow(throwObj: Throw): ModeEngine
}
