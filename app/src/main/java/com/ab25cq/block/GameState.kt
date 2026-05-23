package com.ab25cq.block

import androidx.compose.ui.graphics.Color

const val GRID_SIZE = 10

data class BlockPiece(
    val cells: List<Pair<Int, Int>>,
    val color: Color,
    val glowColor: Color
)

data class GameState(
    val grid: Array<Array<Color?>> = Array(GRID_SIZE) { Array(GRID_SIZE) { null } },
    val score: Int = 0,
    val highScore: Int = 0,
    val currentPieces: List<BlockPiece?> = emptyList(),
    val isGameOver: Boolean = false,
    val clearedLines: Set<Int> = emptySet(),
    val clearedCols: Set<Int> = emptySet(),
    val combo: Int = 0,
    val clearCount: Int = 0  // incremented each time lines/cols are cleared; used as animation trigger
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return score == other.score &&
            highScore == other.highScore &&
            isGameOver == other.isGameOver &&
            currentPieces == other.currentPieces &&
            clearedLines == other.clearedLines &&
            clearedCols == other.clearedCols &&
            combo == other.combo &&
            clearCount == other.clearCount &&
            grid.contentDeepEquals(other.grid)
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + score
        result = 31 * result + highScore
        result = 31 * result + currentPieces.hashCode()
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + clearedLines.hashCode()
        result = 31 * result + clearedCols.hashCode()
        result = 31 * result + combo
        result = 31 * result + clearCount
        return result
    }
}

val PIECE_COLORS = listOf(
    Pair(Color(0xFF00E5FF), Color(0xFF80FFFF)),  // Cyan
    Pair(Color(0xFFFF1744), Color(0xFFFF8A80)),  // Red
    Pair(Color(0xFF00E676), Color(0xFF69F0AE)),  // Green
    Pair(Color(0xFFFFD740), Color(0xFFFFFF72)),  // Amber
    Pair(Color(0xFFD500F9), Color(0xFFEA80FC)),  // Purple
    Pair(Color(0xFFFF6D00), Color(0xFFFFAB40)),  // Orange
    Pair(Color(0xFF2979FF), Color(0xFF82B1FF)),  // Blue
    Pair(Color(0xFFFF4081), Color(0xFFFF80AB)),  // Pink
)

val ALL_PIECES = listOf(
    // Single
    listOf(Pair(0, 0)),
    // Dominoes
    listOf(Pair(0, 0), Pair(0, 1)),
    listOf(Pair(0, 0), Pair(1, 0)),
    // Trominoes
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1)),
    listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1)),
    // Tetrominoes
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 2)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1)),
    listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, 2)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(2, 1)),
    listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(2, 0)),
    // Pentominoes (L-shapes etc)
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(2, 0)),
    listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(1, 2), Pair(1, 3)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(3, 1)),
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 2), Pair(2, 2)),
    // 2x3 Block
    listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(1, 1), Pair(1, 2)),
    // 3x3 Block
    listOf(
        Pair(0, 0), Pair(0, 1), Pair(0, 2),
        Pair(1, 0), Pair(1, 1), Pair(1, 2),
        Pair(2, 0), Pair(2, 1), Pair(2, 2)
    ),
    // 2x2 Block
    listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
)

fun generateRandomPiece(): BlockPiece {
    val cells = ALL_PIECES.random()
    val colorPair = PIECE_COLORS.random()
    return BlockPiece(cells = cells, color = colorPair.first, glowColor = colorPair.second)
}
