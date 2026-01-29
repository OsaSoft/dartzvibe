package cloud.osasoft.dartzvibe.domain.statistics

import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameReference
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.H2HGameSummary
import cloud.osasoft.dartzvibe.data.model.H2HPlayerStats
import cloud.osasoft.dartzvibe.data.model.HeadToHeadStatistics
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatAchievement
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pure domain class that calculates player statistics from completed games.
 */
@OptIn(ExperimentalUuidApi::class)
class StatisticsCalculator {

    companion object {
        /** Maximum score that can be checked out in a single turn (170 = T20, T20, Bull) */
        const val MAX_CHECKOUT_SCORE = 170
    }

    /**
     * Calculate statistics for a player from a list of games.
     *
     * @param playerId The player to calculate stats for
     * @param games List of game sessions (should include completed games)
     * @param players List of all players (for name lookup)
     * @param filter Optional filter to narrow down games
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
        val allTurns = collectAllTurns(relevantGames, playerId, playerNameMap)
        val checkoutStats = calculateCheckoutStats(relevantGames, allTurns, playerId)
        val highScores = countHighScores(allTurns, relevantGames, playerId, playerNameMap)

        return PlayerStatistics(
            playerId = playerId,
            gamesPlayed = relevantGames.size,
            gamesWon = countGamesWon(relevantGames, playerId),
            gamesWonList = buildGamesWonList(relevantGames, playerId, playerNameMap),
            legsPlayed = countLegsPlayed(relevantGames, playerId),
            legsWon = countLegsWon(relevantGames, playerId),
            totalTurns = allTurns.size,
            totalScore = calculateTotalScore(allTurns),
            threeDartAverage = calculateThreeDartAverage(allTurns),
            first9Average = calculateFirst9Average(allTurns),
            checkoutAttempts = checkoutStats.attempts,
            checkoutsHit = checkoutStats.hits,
            checkoutPercentage = checkoutStats.percentage,
            bestCheckout = findBestCheckout(relevantGames, playerId, playerNameMap),
            count180s = highScores.count180s,
            games180s = highScores.games180s,
            count140Plus = highScores.count140Plus,
            count100Plus = highScores.count100Plus,
            highestTurnScore = findHighestTurnScore(allTurns),
        )
    }

    private fun filterRelevantGames(
        games: List<GameSession>,
        playerId: Uuid,
        filter: StatisticsFilter,
    ): List<GameSession> = games.asSequence()
        .filter { it.status == GameStatus.COMPLETED }
        .filter { it.config.playerIds.contains(playerId) }
        .filter { filter.gameType == null || it.config.gameType == filter.gameType }
        .toList()

    private fun collectAllTurns(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): List<TurnWithContext> = buildList {
        for (game in games) {
            val gameRef = createGameReference(game, playerId, playerNameMap)
            for (leg in game.legs) {
                leg.playerTurns
                    .filter { it.playerId == playerId }
                    .forEachIndexed { index, turn ->
                        add(TurnWithContext(turn = turn, legTurnIndex = index, gameRef = gameRef))
                    }
            }
        }
    }

    private fun countGamesWon(
        games: List<GameSession>,
        playerId: Uuid,
    ): Int = games.count { it.winnerId == playerId }

    private fun buildGamesWonList(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): List<GameReference> = buildList {
        for (game in games) {
            if (game.winnerId == playerId) {
                add(createGameReference(game, playerId, playerNameMap))
            }
        }
    }

    private fun countLegsPlayed(
        games: List<GameSession>,
        playerId: Uuid,
    ): Int = games.sumOf { game ->
        game.legs.count { leg ->
            leg.turns.any { it.playerId == playerId }
        }
    }

    private fun countLegsWon(
        games: List<GameSession>,
        playerId: Uuid,
    ): Int = games.sumOf { game ->
        game.legs.count { leg ->
            leg.winnerId == playerId && leg.turns.any { it.playerId == playerId }
        }
    }

    private fun calculateTotalScore(turns: List<TurnWithContext>): Int =
        turns.filter { !it.turn.isBust && it.turn.throws.size == 3 }
            .sumOf { it.turn.totalScore }

    private fun calculateThreeDartAverage(turns: List<TurnWithContext>): FixedDecimal {
        val validTurns = turns.filter { !it.turn.isBust && it.turn.throws.size == 3 }
        if (validTurns.isEmpty()) return FixedDecimal.ZERO

        val totalScore = validTurns.sumOf { it.turn.totalScore }
        return FixedDecimal.divide(totalScore, validTurns.size)
    }

    private fun calculateFirst9Average(turns: List<TurnWithContext>): FixedDecimal {
        val first9Turns = turns.filter { it.legTurnIndex < 3 && !it.turn.isBust }
        if (first9Turns.isEmpty()) return FixedDecimal.ZERO

        val totalScore = first9Turns.sumOf { it.turn.totalScore }
        return FixedDecimal.divide(totalScore, first9Turns.size)
    }

    private fun calculateCheckoutStats(
        games: List<GameSession>,
        turns: List<TurnWithContext>,
        playerId: Uuid,
    ): CheckoutStats {
        val attempts = turns.count { turn ->
            turn.turn.scoreBeforeTurn in 1..MAX_CHECKOUT_SCORE
        }

        val hits = games.sumOf { game ->
            game.legs.count { leg ->
                leg.winnerId == playerId && leg.turns.any { it.playerId == playerId }
            }
        }

        val percentage = FixedDecimal.percentage(hits, attempts)

        return CheckoutStats(attempts = attempts, hits = hits, percentage = percentage)
    }

    private fun findBestCheckout(
        games: List<GameSession>,
        playerId: Uuid,
        playerNameMap: Map<Uuid, Player>,
    ): StatAchievement? {
        var best: StatAchievement? = null

        for (game in games) {
            val gameRef = createGameReference(game, playerId, playerNameMap)
            for (leg in game.legs) {
                if (leg.winnerId != playerId) continue

                val playerTurns = leg.playerTurns.filter { it.playerId == playerId }
                val winningTurn = playerTurns.lastOrNull() ?: continue

                if (!winningTurn.isBust) {
                    val checkoutScore = winningTurn.scoreBeforeTurn
                    if (best == null || checkoutScore > best.value) {
                        best = StatAchievement(checkoutScore, gameRef)
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

        for (turn in turns) {
            if (turn.turn.isBust) continue

            val score = turn.turn.totalScore
            when {
                score >= 180 -> {
                    count180s++
                    count140Plus++
                    count100Plus++
                    gamesWithOne80.add(turn.gameRef.sessionId)
                }

                score >= 140 -> {
                    count140Plus++
                    count100Plus++
                }

                score >= 100 -> {
                    count100Plus++
                }
            }
        }

        val games180s = buildList {
            for (game in games) {
                if (game.id in gamesWithOne80) {
                    add(createGameReference(game, playerId, playerNameMap))
                }
            }
        }

        return HighScoreCounts(
            count180s = count180s,
            games180s = games180s,
            count140Plus = count140Plus,
            count100Plus = count100Plus,
        )
    }

    private fun findHighestTurnScore(turns: List<TurnWithContext>): StatAchievement? {
        var highest: StatAchievement? = null

        for (turn in turns) {
            if (turn.turn.isBust) continue

            val score = turn.turn.totalScore
            if (highest == null || score > highest.value) {
                highest = StatAchievement(score, turn.gameRef)
            }
        }

        return highest
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

    /**
     * Calculate head-to-head statistics between two players.
     *
     * @param player1Id First player
     * @param player2Id Second player
     * @param games List of all game sessions
     * @param gameTypeFilter Optional game type filter
     * @return Head-to-head statistics for the two players
     */
    fun calculateHeadToHeadStatistics(
        player1Id: Uuid,
        player2Id: Uuid,
        games: List<GameSession>,
        gameTypeFilter: GameType? = null,
    ): HeadToHeadStatistics {
        // Filter to games where BOTH players participated and game is completed
        val h2hGames = games.asSequence()
            .filter { it.status == GameStatus.COMPLETED }
            .filter { it.config.playerIds.contains(player1Id) }
            .filter { it.config.playerIds.contains(player2Id) }
            .filter { gameTypeFilter == null || it.config.gameType == gameTypeFilter }
            .sortedByDescending { it.startedAt }
            .toList()

        if (h2hGames.isEmpty()) {
            return HeadToHeadStatistics.empty(player1Id, player2Id)
        }

        val player1Stats = calculateH2HPlayerStats(player1Id, h2hGames)
        val player2Stats = calculateH2HPlayerStats(player2Id, h2hGames)

        val player1Wins = h2hGames.count { it.winnerId == player1Id }
        val player2Wins = h2hGames.count { it.winnerId == player2Id }

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

        return HeadToHeadStatistics(
            player1Id = player1Id,
            player2Id = player2Id,
            gamesPlayed = h2hGames.size,
            player1Wins = player1Wins,
            player2Wins = player2Wins,
            player1Stats = player1Stats,
            player2Stats = player2Stats,
            recentGames = recentGames,
        )
    }

    private fun calculateH2HPlayerStats(
        playerId: Uuid,
        games: List<GameSession>,
    ): H2HPlayerStats {
        var totalScore = 0
        var turnCount = 0
        var bestCheckout: Int? = null
        var legsWon = 0
        var legsPlayed = 0
        var count180s = 0
        var count140Plus = 0

        games.flatMap { it.legs }.forEach { leg ->
            val playerTurns = leg.playerTurns.filter { it.playerId == playerId }
            if (playerTurns.isNotEmpty()) {
                legsPlayed++
                if (leg.winnerId == playerId) {
                    legsWon++
                    // Calculate checkout score
                    val winningTurn = playerTurns.last()
                    if (!winningTurn.isBust) {
                        val checkoutScore = winningTurn.scoreBeforeTurn
                        if (bestCheckout == null || checkoutScore > bestCheckout) {
                            bestCheckout = checkoutScore
                        }
                    }
                }

                // Count turns and scores
                playerTurns.forEach { turn ->
                    if (!turn.isBust && turn.throws.size == 3) {
                        totalScore += turn.totalScore
                        turnCount++

                        val score = turn.totalScore
                        if (score >= 180) {
                            count180s++
                            count140Plus++
                        } else if (score >= 140) {
                            count140Plus++
                        }
                    }
                }
            }
        }

        val threeDartAverage = if (turnCount > 0) {
            FixedDecimal.divide(totalScore, turnCount)
        } else {
            FixedDecimal.ZERO
        }

        return H2HPlayerStats(
            threeDartAverage = threeDartAverage,
            bestCheckout = bestCheckout,
            legsWon = legsWon,
            legsPlayed = legsPlayed,
            count180s = count180s,
            count140Plus = count140Plus,
        )
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
}
