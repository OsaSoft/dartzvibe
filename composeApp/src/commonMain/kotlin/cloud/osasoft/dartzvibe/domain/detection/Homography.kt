package cloud.osasoft.dartzvibe.domain.detection

import kotlin.math.abs

/**
 * Known calibration point positions on the normalized dartboard.
 * Center = (0,0), outer double ring midpoints at radius ~0.919.
 */
val CALIBRATION_BOARD_POSITIONS: Map<Int, Pair<Float, Float>> = mapOf(
    DetectionClass.CAL_20 to Pair(0.0f, -0.919f),
    DetectionClass.CAL_3 to Pair(0.0f, 0.919f),
    DetectionClass.CAL_11 to Pair(-0.919f, 0.0f),
    DetectionClass.CAL_6 to Pair(0.919f, 0.0f),
)

/**
 * Computes a 3x3 perspective homography matrix from exactly 4 source→destination point pairs
 * using Direct Linear Transform (DLT) solved via Gaussian elimination.
 *
 * @param srcPoints 4 source points (image coordinates)
 * @param dstPoints 4 destination points (board coordinates)
 * @return 9-element FloatArray representing the 3x3 matrix (row-major), or null if degenerate
 */
fun computeHomography(
    srcPoints: List<Pair<Float, Float>>,
    dstPoints: List<Pair<Float, Float>>,
): FloatArray? {
    require(srcPoints.size == 4) { "Exactly 4 source points required" }
    require(dstPoints.size == 4) { "Exactly 4 destination points required" }

    // Build the 8x9 matrix A for the DLT equation Ah = 0
    // For each point pair (x,y) -> (x',y'):
    //   [-x, -y, -1,  0,  0,  0, x*x', y*x', x']
    //   [ 0,  0,  0, -x, -y, -1, x*y', y*y', y']
    val a = Array(8) { DoubleArray(9) }
    (0 until 4).forEach { i ->
        val (sx, sy) = srcPoints[i]
        val (dx, dy) = dstPoints[i]
        val row1 = i * 2
        val row2 = row1 + 1

        a[row1][0] = -sx.toDouble()
        a[row1][1] = -sy.toDouble()
        a[row1][2] = -1.0
        a[row1][3] = 0.0
        a[row1][4] = 0.0
        a[row1][5] = 0.0
        a[row1][6] = (sx * dx).toDouble()
        a[row1][7] = (sy * dx).toDouble()
        a[row1][8] = dx.toDouble()

        a[row2][0] = 0.0
        a[row2][1] = 0.0
        a[row2][2] = 0.0
        a[row2][3] = -sx.toDouble()
        a[row2][4] = -sy.toDouble()
        a[row2][5] = -1.0
        a[row2][6] = (sx * dy).toDouble()
        a[row2][7] = (sy * dy).toDouble()
        a[row2][8] = dy.toDouble()
    }

    // Solve the 8x9 system using Gaussian elimination with partial pivoting.
    // We fix h[8] = 1 and solve the 8x8 system for h[0..7].
    // Rearrange: move column 8 to the right-hand side as -b.
    val mat = Array(8) { row -> DoubleArray(8) { col -> a[row][col] } }
    val b = DoubleArray(8) { row -> -a[row][8] }

    // Forward elimination with partial pivoting
    (0 until 8).forEach { col ->
        // Find pivot
        var maxVal = abs(mat[col][col])
        var maxRow = col
        ((col + 1) until 8).forEach { row ->
            if (abs(mat[row][col]) > maxVal) {
                maxVal = abs(mat[row][col])
                maxRow = row
            }
        }

        if (maxVal < 1e-10) return null // Degenerate

        // Swap rows
        if (maxRow != col) {
            val tmpRow = mat[col]
            mat[col] = mat[maxRow]
            mat[maxRow] = tmpRow
            val tmpB = b[col]
            b[col] = b[maxRow]
            b[maxRow] = tmpB
        }

        // Eliminate below
        ((col + 1) until 8).forEach { row ->
            val factor = mat[row][col] / mat[col][col]
            (col until 8).forEach { j ->
                mat[row][j] -= factor * mat[col][j]
            }
            b[row] -= factor * b[col]
        }
    }

    // Back substitution
    val h = DoubleArray(9)
    h[8] = 1.0
    (7 downTo 0).forEach { row ->
        var sum = b[row]
        ((row + 1) until 8).forEach { j ->
            sum -= mat[row][j] * h[j]
        }
        if (abs(mat[row][row]) < 1e-10) return null // Degenerate
        h[row] = sum / mat[row][row]
    }

    return FloatArray(9) { h[it].toFloat() }
}

/**
 * Applies a 3x3 homography matrix to a 2D point.
 *
 * @param matrix 9-element homography matrix (row-major)
 * @param x source X coordinate
 * @param y source Y coordinate
 * @return transformed (x', y') point
 */
fun applyHomography(
    matrix: FloatArray,
    x: Float,
    y: Float,
): Pair<Float, Float> {
    val w = matrix[6] * x + matrix[7] * y + matrix[8]
    if (abs(w) < 1e-10f) return Pair(0f, 0f)
    val tx = (matrix[0] * x + matrix[1] * y + matrix[2]) / w
    val ty = (matrix[3] * x + matrix[4] * y + matrix[5]) / w
    return Pair(tx, ty)
}
