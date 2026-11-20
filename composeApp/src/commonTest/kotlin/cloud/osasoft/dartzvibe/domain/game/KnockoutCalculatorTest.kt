package cloud.osasoft.dartzvibe.domain.game

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class KnockoutCalculatorTest : FreeSpec({

    val bob = Uuid.parse("00000000-0000-0000-0000-0000000000b0")
    val carol = Uuid.parse("00000000-0000-0000-0000-0000000000c0")
    val dave = Uuid.parse("00000000-0000-0000-0000-0000000000d0")

    fun List<KnockoutRoute>.display() = map { route -> route.throws.joinToString(" ") { it.toDisplayString() } }

    "route" - {

        data class RouteCase(
            val gap: Int,
            val dartsLeft: Int,
            val expected: String,
        )

        withData(
            nameFn = { "Should reach a gap of ${it.gap} with ${it.dartsLeft} darts left as ${it.expected}" },
            RouteCase(gap = 21, dartsLeft = 1, expected = "T7"),
            RouteCase(gap = 20, dartsLeft = 3, expected = "S20"),
            RouteCase(gap = 25, dartsLeft = 1, expected = "S25"),
            RouteCase(gap = 40, dartsLeft = 2, expected = "D20"),
            RouteCase(gap = 50, dartsLeft = 1, expected = "Bull"),
            RouteCase(gap = 60, dartsLeft = 1, expected = "T20"),
            RouteCase(gap = 81, dartsLeft = 2, expected = "T20 T7"),
            RouteCase(gap = 61, dartsLeft = 2, expected = "T20 S1"),
            RouteCase(gap = 120, dartsLeft = 3, expected = "T20 T20"),
        ) { case ->
            // GIVEN a gap to an opponent and the darts left
            // WHEN finding the route
            val route = KnockoutCalculator.route(gap = case.gap, maxDarts = case.dartsLeft)

            // THEN it uses the fewest darts, highest setup first, and adds up to the gap
            route.shouldNotBeNull()
            route.joinToString(" ") { it.toDisplayString() } shouldBe case.expected
            route.sumOf { it.score } shouldBe case.gap
        }

        withData(
            nameFn = { (gap, darts) -> "Should use $darts darts for a gap of $gap" },
            1 to 1,
            60 to 1,
            61 to 2,
            119 to 3,
            120 to 2,
            121 to 3,
            171 to 3,
            180 to 3,
        ) { (gap, darts) ->
            // GIVEN a gap and three darts left
            // WHEN finding the route
            val route = KnockoutCalculator.route(gap = gap, maxDarts = 3)

            // THEN it takes exactly the fewest darts that can score it
            route.shouldNotBeNull().size shouldBe darts
            route.sumOf { it.score } shouldBe gap
        }

        withData(
            nameFn = { (gap, darts) -> "Should find no route for a gap of $gap with $darts darts left" },
            61 to 1,
            23 to 1,
            59 to 1,
            121 to 2,
            163 to 3,
            179 to 3,
            181 to 3,
            200 to 3,
            20 to 0,
            0 to 3,
        ) { (gap, darts) ->
            // GIVEN a gap the darts left can't score exactly
            // WHEN finding the route
            // THEN there is none
            KnockoutCalculator.route(gap = gap, maxDarts = darts).shouldBeNull()
        }
    }

    "getKnockoutOptions" - {

        "Should suggest the opponent ahead by a reachable amount" {
            // GIVEN the thrower on 100 and Bob on 121, one dart left
            // WHEN finding knockouts
            val options = KnockoutCalculator.getKnockoutOptions(
                currentScore = 100,
                targetScore = 301,
                opponentScores = listOf(bob to 121),
                dartsLeft = 1,
            )

            // THEN Bob is knocked out with T7
            options.map { it.opponentId } shouldBe listOf(bob)
            options.display() shouldBe listOf("T7")
        }

        "Should keep every intermediate score below the target" {
            // GIVEN the thrower on 120 and Carol on 300 of 301 (180 ahead), three darts left
            // WHEN finding knockouts
            val options = KnockoutCalculator.getKnockoutOptions(
                currentScore = 120,
                targetScore = 301,
                opponentScores = listOf(carol to 300),
                dartsLeft = 3,
            )

            // THEN the route's running totals never reach the target (no bounce, no win on the way)
            val route = options.single().throws
            route.runningFold(120) { score, dart -> score + dart.score }.drop(1).forEach { score ->
                (score < 301) shouldBe true
            }
            route.sumOf { it.score } shouldBe 180
        }

        "Should order by darts needed, then by the higher score, and keep at most two" {
            // GIVEN the thrower on 0: Bob 81 ahead (2 darts), Carol 25 ahead (1 dart), Dave 60 ahead (1 dart)
            // WHEN finding knockouts with three darts left
            val options = KnockoutCalculator.getKnockoutOptions(
                currentScore = 0,
                targetScore = 301,
                opponentScores = listOf(bob to 81, carol to 25, dave to 60),
                dartsLeft = 3,
            )

            // THEN the one-dart knockouts come first, the leader (Dave) before Carol, and Bob is left out
            options.map { it.opponentId } shouldBe listOf(dave, carol)
            options.display() shouldBe listOf("T20", "S25")
        }

        "Should ignore opponents on 0, behind, level, at the target or out of reach" {
            // GIVEN the thrower on 100 with one dart: Bob on 0, Carol behind on 80, Dave 61 ahead
            // WHEN finding knockouts
            val options = KnockoutCalculator.getKnockoutOptions(
                currentScore = 100,
                targetScore = 301,
                opponentScores = listOf(bob to 0, carol to 80, dave to 161),
                dartsLeft = 1,
            )

            // THEN nobody can be knocked out
            options.shouldBeEmpty()

            // AND opponents level with the thrower or on the target are never suggested either
            KnockoutCalculator.getKnockoutOptions(
                currentScore = 290,
                targetScore = 301,
                opponentScores = listOf(bob to 290, carol to 301),
                dartsLeft = 3,
            ).shouldBeEmpty()
        }

        "Should suggest nothing with no darts left" {
            // GIVEN Bob 20 ahead but the turn's darts are thrown
            // WHEN finding knockouts
            // THEN there are none
            KnockoutCalculator.getKnockoutOptions(
                currentScore = 0,
                targetScore = 301,
                opponentScores = listOf(bob to 20),
                dartsLeft = 0,
            ).shouldBeEmpty()
        }

        "Should keep turn order for opponents needing the same darts on the same score" {
            // GIVEN Bob and Carol both 40 ahead
            // WHEN finding knockouts
            val options = KnockoutCalculator.getKnockoutOptions(
                currentScore = 0,
                targetScore = 301,
                opponentScores = listOf(bob to 40, carol to 40),
                dartsLeft = 1,
            )

            // THEN Bob (earlier in turn order) is first
            options.map { it.opponentId } shouldBe listOf(bob, carol)
        }
    }
})
