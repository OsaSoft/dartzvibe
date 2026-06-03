package cloud.osasoft.dartzvibe.domain.statistics

import cloud.osasoft.dartzvibe.data.model.CheckoutBandStat
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameReference
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.H2HGameSummary
import cloud.osasoft.dartzvibe.data.model.HeadToHeadStatistics
import cloud.osasoft.dartzvibe.data.model.ModeStatistics
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatAchievement
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pure domain class that calculates player statistics from completed games.
 *
 * Statistics are mode-scoped: [StatisticsFilter.gameMode] selects which [ModeStatistics]
 * subtype is produced. Each mode has its own calculation path, so no metric ever blends
 * across modes (e.g. a Cricket point total never contaminates a Classic 3-dart average).
 */
@OptIn(ExperimentalUuidApi::class)
class StatisticsCalculator {

    companion object {
        /** Maximum score that can be checked out in a single turn (170 = T20, T20, Bull) */
        const val MAX_CHECKOUT_SCORE = 170

        /** Checkout-practice difficulty bands, in display order. */
        private val CHECKOUT_BANDS = listOf(
            GameType.CHECKOUT_EASY,
            GameType.CHECKOUT_MEDIUM,
            GameType.CHECKOUT_HARD,
            GameType.CHECKOUT_FULL,
        )
    }

    /**
     * Calculate statistics for a player from a list of games.
     *
     * @param playerId The player to calculate stats for
     * @param games List of game sessions (should include completed games)
     * @param players List of all players (for name lookup)
     * @param filter Optional filter; [StatisticsFilter.gameMode] scopes to a single mode
     * @return Calculated statistics for the player
     */
    fun calculatePlayerStatistics(
        playerId: Uuid,
        games: List<GameSession>,
        players: List<Player>,
        filter: StatisticsFilter = StatisticsFilter(),
    ): PlayerStatistics {
        val relevantGames = filterRelevantGames(games, playerId, filter)

        if (relevantGames.isEmpty()) {
            return PlayerStatistics.empty(playerId)
        }

        val playerNameMap = players.associateBy { it.id }

        val modeStats: ModeStatistics? = when (filter.gameMode) {
            GameMode.CLASSIC -> calculateClassicStats(relevantGames, playerId, playerNameMap)
            GameMode.PARCHEESI -> calculateParcheesiStats(relevantGames, playerId)
            GameMode.CRICKET -> calculateCricketStats(relevantGames, playerId)
            GameMode.CHECKOUT_PRACTICE -> calculateCheckoutPracticeStats(relevantGames)
            GameMode.ROULETTE -> calculateRouletteStats(relevantGames, playerId)
            null -> null
        }

        return PlayerStatistics(
            playerId = playerId,
            gamesPlayed = relevantGames.size,
            gamesWon = countGamesWon(relevantGames, playerId),
            gamesWonList = buildGamesWonList(relevantGames, playerId, playerNameMap),
            legsPlayed = countLegsPlayed(relevantGames, playerId),
            legsWon = countLegsWon(relevantGames, playerId),
            totalTurns = countPlayerTurns(relevantGames, playerId),
            modeStats = modeStats,
        )
    }

    private fun filterRelevantGames(
        games: List<GameSession>,
        playerId: Uuid,
        filter: StatisticsFilter,
    ): List<GameSession> = games.asSequence()
        .filter { it.status == GameStatus.COMPLETED }
        .filter { it.config.playerIds.contains(playerId) }
        .filter { filter.gameMode == null || it.config.gameMode == filter.gameMode }
        .filter { filter.gameType == null || it.config.gameType == filter.gameType }
        .toList()

    // ---- Universal helpers (mode-agnostic) ----

    private fun countGamesWon(games: List<GameSession>, playerId: Uuid): Int =
        games.count { it.winnerId == playerId }

    private fun buildGamesWonList(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): List<GameReference> = games
        .filter { it.winnerId == playerId }
        .map { createGameReference(it, playerId, playerNameMap) }

    private fun countLegsPlayed(games: List<GameSession>, playerId: Uuid): Int =
        games.sumOf { game ->
            game.legs.count { leg -> leg.turns.any { it.playerId == playerId } }
        }

    private fun countLegsWon(games: List<GameSession>, playerId: Uuid): Int =
        games.sumOf { game ->
            game.legs.count { leg ->
                leg.winnerId == playerId && leg.turns.any { it.playerId == playerId }
            }
        }

    private fun countPlayerTurns(games: List<GameSession>, playerId: Uuid): Int =
        games.sumOf { game ->
            game.legs.sumOf { leg ->
                leg.playerTurns.count { it.playerId == playerId }
            }
        }

    private fun collectAllTurns(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): List<TurnWithContext> = buildList {
        games.forEach { game ->
            val gameRef = createGameReference(game, playerId, playerNameMap)
            game.legs.forEach { leg ->
                leg.playerTurns
                    .filter { it.playerId == playerId }
                    .forEachIndexed { index, turn ->
                        add(TurnWithContext(turn = turn, legTurnIndex = index, gameRef = gameRef))
                    }
            }
        }
    }

    private fun createGameReference(
        game: GameSession,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): GameReference {
        val opponentNames = game.config.playerIds
            .filter { it != playerId }
            .mapNotNull { playerNameMap[it]?.name }
            .joinToString(" vs ")

        return GameReference(
            sessionId = game.id,
            timestamp = game.startedAt,
            opponentNames = opponentNames.ifEmpty { "Solo" },
        )
    }

    // ---- Classic ----

    private fun calculateClassicStats(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): ModeStatistics.Classic {
        val turns = collectAllTurns(games, playerId, playerNameMap)
        val checkout = calculateCheckoutStats(games, turns, playerId)
        val high = countHighScores(turns, games, playerId, playerNameMap)

        return ModeStatistics.Classic(
            threeDartAverage = calculateThreeDartAverage(turns),
            first9Average = calculateFirst9Average(turns),
            checkoutAttempts = checkout.attempts,
            checkoutsHit = checkout.hits,
            checkoutPercentage = checkout.percentage,
            bestCheckout = findBestCheckout(games, playerId, playerNameMap),
            count180s = high.count180s,
            games180s = high.games180s,
            count140Plus = high.count140Plus,
            count100Plus = high.count100Plus,
            highestTurnScore = findHighestTurnScore(turns),
        )
    }

    private fun calculateThreeDartAverage(turns: List<TurnWithContext>): FixedDecimal {
        val valid = turns.filter { !it.turn.isBust && it.turn.throws.size == 3 }
        if (valid.isEmpty()) return FixedDecimal.ZERO
        return FixedDecimal.divide(valid.sumOf { it.turn.totalScore }, valid.size)
    }

    private fun calculateFirst9Average(turns: List<TurnWithContext>): FixedDecimal {
        val first9 = turns.filter { it.legTurnIndex < 3 && !it.turn.isBust }
        if (first9.isEmpty()) return FixedDecimal.ZERO
        return FixedDecimal.divide(first9.sumOf { it.turn.totalScore }, first9.size)
    }

    private fun calculateCheckoutStats(
        games: List<GameSession>,
        turns: List<TurnWithContext>,
        playerId: Uuid,
    ): CheckoutStats {
        val attempts = turns.count { it.turn.scoreBeforeTurn in 1..MAX_CHECKOUT_SCORE }
        val hits = countLegsWon(games, playerId)
        return CheckoutStats(
            attempts = attempts,
            hits = hits,
            percentage = FixedDecimal.percentage(hits, attempts),
        )
    }

    private fun findBestCheckout(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): StatAchievement? {
        var best: StatAchievement? = null
        games.forEach { game ->
            val gameRef = createGameReference(game, playerId, playerNameMap)
            game.legs.forEach { leg ->
                if (leg.winnerId != playerId) return@forEach
                val winningTurn = leg.playerTurns.lastOrNull { it.playerId == playerId } ?: return@forEach
                if (!winningTurn.isBust) {
                    val score = winningTurn.scoreBeforeTurn
                    val current = best
                    if (current == null || score > current.value) {
                        best = StatAchievement(score, gameRef)
                    }
                }
            }
        }
        return best
    }

    private fun countHighScores(
        turns: List<TurnWithContext>,
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): HighScoreCounts {
        var count180s = 0
        var count140Plus = 0
        var count100Plus = 0
        val gamesWithOne80 = mutableSetOf<Uuid>()

        turns.forEach { ctx ->
            if (ctx.turn.isBust) return@forEach
            val score = ctx.turn.totalScore
            when {
                score >= 180 -> {
                    count180s++
                    count140Plus++
                    count100Plus++
                    gamesWithOne80.add(ctx.gameRef.sessionId)
                }

                score >= 140 -> {
                    count140Plus++
                    count100Plus++
                }

                score >= 100 -> count100Plus++
            }
        }

        val games180s = games
            .filter { it.id in gamesWithOne80 }
            .map { createGameReference(it, playerId, playerNameMap) }

        return HighScoreCounts(count180s, games180s, count140Plus, count100Plus)
    }

    private fun findHighestTurnScore(turns: List<TurnWithContext>): StatAchievement? {
        var highest: StatAchievement? = null
        turns.forEach { ctx ->
            if (ctx.turn.isBust) return@forEach
            val score = ctx.turn.totalScore
            val current = highest
            if (current == null || score > current.value) {
                highest = StatAchievement(score, ctx.gameRef)
            }
        }
        return highest
    }

    // ---- Parcheesi ----

    private fun calculateParcheesiStats(
        games: List<GameSession>,
        playerId: Uuid,
    ): ModeStatistics.Parcheesi {
        val playerTurns = games.flatMap { game ->
            game.legs.flatMap { leg -> leg.playerTurns.filter { it.playerId == playerId } }
        }
        val validTurns = playerTurns.filter { !it.isBust && it.throws.size == 3 }
        val threeDartAverage = if (validTurns.isEmpty()) {
            FixedDecimal.ZERO
        } else {
            FixedDecimal.divide(validTurns.sumOf { it.totalScore }, validTurns.size)
        }

        val knockout = calculateKnockoutStats(games, playerId)

        val bounceTurns = playerTurns.count { it.isBounce }
        val bounceBackRate = FixedDecimal.percentage(bounceTurns, playerTurns.size)

        val legsWon = countLegsWon(games, playerId)
        val turnsInWonLegs = games.sumOf { game ->
            game.legs.filter { it.winnerId == playerId }.sumOf { leg ->
                leg.playerTurns.count { it.playerId == playerId }
            }
        }
        val avgTurnsToWin = if (legsWon > 0) {
            FixedDecimal.divide(turnsInWonLegs, legsWon)
        } else {
            FixedDecimal.ZERO
        }

        val gamesPlayed = games.size
        val gamesWon = countGamesWon(games, playerId)

        return ModeStatistics.Parcheesi(
            threeDartAverage = threeDartAverage,
            knockoutsDealt = knockout.knockoutsDealt,
            timesKnockedOut = knockout.timesKnockedOut,
            bounceBackRate = bounceBackRate,
            avgTurnsToWin = avgTurnsToWin,
            winRate = FixedDecimal.percentage(gamesWon, gamesPlayed),
        )
    }

    // ---- Cricket ----

    private fun calculateCricketStats(
        games: List<GameSession>,
        playerId: Uuid,
    ): ModeStatistics.Cricket {
        var totalMarks = 0
        var totalRounds = 0
        var dartsOnTarget = 0
        var totalDarts = 0
        var totalClosed = 0
        var totalSegments = 0
        var totalPoints = 0

        games.forEach { game ->
            game.legs.forEach { leg ->
                val segments = leg.cricketState?.segments?.segments
                    ?: game.config.cricketSegments?.segments
                    ?: return@forEach
                val playerState = leg.cricketState?.getPlayerState(playerId)

                leg.playerTurns
                    .filter { it.playerId == playerId }
                    .forEach { turn ->
                        totalRounds++
                        turn.throws.forEach { t ->
                            totalDarts++
                            if (t.segment in segments) {
                                dartsOnTarget++
                                totalMarks += t.multiplier.value
                            }
                        }
                    }

                if (playerState != null) {
                    totalSegments += segments.size
                    totalClosed += segments.count { playerState.isClosed(it) }
                    totalPoints += playerState.points
                }
            }
        }

        return ModeStatistics.Cricket(
            marksPerRound = FixedDecimal.divide(totalMarks, totalRounds),
            closeRate = FixedDecimal.percentage(totalClosed, totalSegments),
            avgPointsPerGame = FixedDecimal.divide(totalPoints, games.size),
            hitRate = FixedDecimal.percentage(dartsOnTarget, totalDarts),
        )
    }

    // ---- Checkout practice ----

    private fun calculateCheckoutPracticeStats(
        games: List<GameSession>,
    ): ModeStatistics.CheckoutPractice {
        val allResults = games.flatMap { game ->
            game.legs.flatMap { leg -> leg.checkoutPracticeState?.roundResults.orEmpty() }
        }
        val successes = allResults.filter { it.success }
        val totalRounds = allResults.size
        val successCount = successes.size

        val avgDarts = if (successCount > 0) {
            FixedDecimal.divide(successes.sumOf { it.dartsUsed }, successCount)
        } else {
            FixedDecimal.ZERO
        }
        val bestTarget = successes.maxOfOrNull { it.target }

        val bands = CHECKOUT_BANDS.mapNotNull { band ->
            val bandGames = games.filter { it.config.gameType == band }
            if (bandGames.isEmpty()) return@mapNotNull null
            val results = bandGames.flatMap { game ->
                game.legs.flatMap { leg -> leg.checkoutPracticeState?.roundResults.orEmpty() }
            }
            CheckoutBandStat(
                band = band,
                successCount = results.count { it.success },
                attempts = results.size,
            )
        }

        return ModeStatistics.CheckoutPractice(
            successRate = FixedDecimal.percentage(successCount, totalRounds),
            successCount = successCount,
            totalRounds = totalRounds,
            avgDartsToCheckout = avgDarts,
            bestTarget = bestTarget,
            sessionsCompleted = games.size,
            bands = bands,
        )
    }

    // ---- Roulette ----

    private fun calculateRouletteStats(
        games: List<GameSession>,
        playerId: Uuid,
    ): ModeStatistics.Roulette {
        var hits = 0
        var totalDarts = 0
        var totalPoints = 0
        var turnCount = 0
        var bestRoundScore = 0

        games.forEach { game ->
            val targets = game.config.rouletteTargetSegments.orEmpty()
            val playerCount = game.config.playerIds.size.coerceAtLeast(1)

            game.legs.forEach { leg ->
                leg.playerTurns.forEachIndexed { index, turn ->
                    if (turn.playerId != playerId) return@forEachIndexed
                    val target = if (targets.isEmpty()) {
                        1
                    } else {
                        targets[(index / playerCount) % targets.size]
                    }
                    turnCount++
                    val turnPoints = turn.scoreAfterTurn - turn.scoreBeforeTurn
                    totalPoints += turnPoints
                    if (turnPoints > bestRoundScore) bestRoundScore = turnPoints
                    turn.throws.forEach { t ->
                        totalDarts++
                        if (t.segment == target) hits++
                    }
                }
            }
        }

        val gamesWon = countGamesWon(games, playerId)

        return ModeStatistics.Roulette(
            pointsPerRound = FixedDecimal.divide(totalPoints, turnCount),
            bestRoundScore = bestRoundScore,
            hitRate = FixedDecimal.percentage(hits, totalDarts),
            winRate = FixedDecimal.percentage(gamesWon, games.size),
        )
    }

    /**
     * Resolve the roulette target segment a throw at [turnIndex] (0-based position within
     * the leg's [Leg.playerTurns]) was aimed at. Exposed for tests that lock the invariant
     * that this derivation matches the engine. Rounds advance every [playerCount] turns and
     * roulette legs contain no phantom turns, so integer division is exact for any prefix.
     */
    fun rouletteTargetFor(targets: List<Int>, playerCount: Int, turnIndex: Int): Int {
        if (targets.isEmpty()) return 1
        val pc = playerCount.coerceAtLeast(1)
        return targets[(turnIndex / pc) % targets.size]
    }

    /**
     * Calculate head-to-head statistics between two players, scoped to a single game mode.
     *
     * Each player's [HeadToHeadStatistics.player1Stats]/[HeadToHeadStatistics.player2Stats] is a
     * mode-scoped [PlayerStatistics] over the shared games, so the comparison uses the same
     * per-mode metrics as the single-player view (no Classic-only assumptions).
     *
     * @param gameMode Mode to compare in. Solo modes (Checkout Practice) never match two players.
     */
    fun calculateHeadToHeadStatistics(
        player1Id: Uuid,
        player2Id: Uuid,
        games: List<GameSession>,
        players: List<Player> = emptyList(),
        gameMode: GameMode? = null,
        gameTypeFilter: GameType? = null,
    ): HeadToHeadStatistics {
        // Games where BOTH players participated, completed, in the selected mode.
        val h2hGames = games.asSequence()
            .filter { it.status == GameStatus.COMPLETED }
            .filter { gameMode == null || it.config.gameMode == gameMode }
            .filter { it.config.gameMode != GameMode.CHECKOUT_PRACTICE }
            .filter { it.config.playerIds.contains(player1Id) }
            .filter { it.config.playerIds.contains(player2Id) }
            .filter { gameTypeFilter == null || it.config.gameType == gameTypeFilter }
            .sortedByDescending { it.startedAt }
            .toList()

        if (h2hGames.isEmpty()) {
            return HeadToHeadStatistics.empty(player1Id, player2Id)
        }

        val recentGames = h2hGames.take(10).map { game ->
            H2HGameSummary(
                sessionId = game.id,
                timestamp = game.startedAt,
                gameType = game.config.gameType,
                winnerId = game.winnerId,
                player1LegsWon = game.legs.count { it.winnerId == player1Id },
                player2LegsWon = game.legs.count { it.winnerId == player2Id },
            )
        }

        val filter = StatisticsFilter(gameMode = gameMode, gameType = gameTypeFilter)

        return HeadToHeadStatistics(
            player1Id = player1Id,
            player2Id = player2Id,
            gamesPlayed = h2hGames.size,
            player1Wins = h2hGames.count { it.winnerId == player1Id },
            player2Wins = h2hGames.count { it.winnerId == player2Id },
            player1Stats = calculatePlayerStatistics(player1Id, h2hGames, players, filter),
            player2Stats = calculatePlayerStatistics(player2Id, h2hGames, players, filter),
            recentGames = recentGames,
        )
    }

    /**
     * Calculate knockout statistics for a player from Parcheesi games.
     *
     * Knockouts are tracked as phantom turns with scoreAfterTurn = 0. To find who caused a
     * knockout, we look at the non-phantom turn immediately before the phantom turn — if its
     * scoreAfterTurn matches the phantom's scoreBeforeTurn, that player dealt the knockout.
     */
    private fun calculateKnockoutStats(
        games: List<GameSession>,
        playerId: Uuid,
    ): KnockoutStats {
        var knockoutsDealt = 0
        var timesKnockedOut = 0

        games.filter { it.config.gameMode == GameMode.PARCHEESI }.forEach { game ->
            game.legs.forEach { leg ->
                leg.turns.forEachIndexed { index, turn ->
                    if (!turn.isPhantom) return@forEachIndexed
                    val causingTurn = leg.turns.take(index).lastOrNull { !it.isPhantom }
                    if (causingTurn != null && causingTurn.scoreAfterTurn == turn.scoreBeforeTurn) {
                        if (causingTurn.playerId == playerId) knockoutsDealt++
                        if (turn.playerId == playerId) timesKnockedOut++
                    }
                }
            }
        }
        return KnockoutStats(knockoutsDealt, timesKnockedOut)
    }

    /** Internal class to track turn context. */
    private data class TurnWithContext(
        val turn: Turn,
        val legTurnIndex: Int,
        val gameRef: GameReference,
    )

    /** Checkout statistics result. */
    private data class CheckoutStats(
        val attempts: Int,
        val hits: Int,
        val percentage: FixedDecimal,
    )

    /** High score counts result. */
    private data class HighScoreCounts(
        val count180s: Int,
        val games180s: List<GameReference>,
        val count140Plus: Int,
        val count100Plus: Int,
    )

    /** Knockout statistics result for Parcheesi games. */
    private data class KnockoutStats(
        val knockoutsDealt: Int,
        val timesKnockedOut: Int,
    )
}
