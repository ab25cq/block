package com.ab25cq.block

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        startNewGame()
    }

    fun startNewGame() {
        _gameState.value = GameState(
            grid = Array(GRID_SIZE) { Array(GRID_SIZE) { null } },
            score = 0,
            highScore = _gameState.value.highScore,
            currentPieces = generateNewPieces(),
            isGameOver = false,
            clearedLines = emptySet(),
            clearedCols = emptySet(),
            combo = 0
        )
    }

    private fun generateNewPieces(): List<BlockPiece?> {
        return listOf(generateRandomPiece(), generateRandomPiece(), generateRandomPiece())
    }

    fun canPlacePiece(piece: BlockPiece, row: Int, col: Int): Boolean {
        val grid = _gameState.value.grid
        for ((dr, dc) in piece.cells) {
            val r = row + dr
            val c = col + dc
            if (r < 0 || r >= GRID_SIZE || c < 0 || c >= GRID_SIZE) return false
            if (grid[r][c] != null) return false
        }
        return true
    }

    fun placePiece(pieceIndex: Int, row: Int, col: Int) {
        val state = _gameState.value
        val piece = state.currentPieces.getOrNull(pieceIndex) ?: return
        if (!canPlacePiece(piece, row, col)) return

        val newGrid = state.grid.map { it.clone() }.toTypedArray()
        for ((dr, dc) in piece.cells) {
            newGrid[row + dr][col + dc] = piece.color
        }

        val (clearedGrid, clearedLines, clearedCols, linesCleared) = clearLines(newGrid)

        val baseScore = piece.cells.size * 10
        val clearScore = linesCleared * linesCleared * 100
        val newCombo = if (linesCleared > 0) state.combo + 1 else 0
        val comboBonus = if (newCombo > 1) newCombo * 50 else 0
        val addedScore = baseScore + clearScore + comboBonus
        val newScore = state.score + addedScore

        val newPieces = state.currentPieces.toMutableList()
        newPieces[pieceIndex] = null

        val allUsed = newPieces.all { it == null }
        val finalPieces = if (allUsed) generateNewPieces() else newPieces

        val newHighScore = maxOf(state.highScore, newScore)

        val isGameOver = checkGameOver(clearedGrid, finalPieces)

        _gameState.value = state.copy(
            grid = clearedGrid,
            score = newScore,
            highScore = newHighScore,
            currentPieces = finalPieces,
            isGameOver = isGameOver,
            clearedLines = clearedLines,
            clearedCols = clearedCols,
            combo = newCombo,
            clearCount = if (linesCleared > 0) state.clearCount + 1 else state.clearCount
        )
    }

    private data class ClearResult(
        val grid: Array<Array<Color?>>,
        val clearedLines: Set<Int>,
        val clearedCols: Set<Int>,
        val totalCleared: Int
    )

    private fun clearLines(grid: Array<Array<Color?>>): ClearResult {
        val newGrid = grid.map { it.clone() }.toTypedArray()
        val clearedLines = mutableSetOf<Int>()
        val clearedCols = mutableSetOf<Int>()

        for (r in 0 until GRID_SIZE) {
            if (newGrid[r].all { it != null }) clearedLines.add(r)
        }
        for (c in 0 until GRID_SIZE) {
            if ((0 until GRID_SIZE).all { r -> newGrid[r][c] != null }) clearedCols.add(c)
        }

        for (r in clearedLines) {
            for (c in 0 until GRID_SIZE) newGrid[r][c] = null
        }
        for (c in clearedCols) {
            for (r in 0 until GRID_SIZE) newGrid[r][c] = null
        }

        return ClearResult(newGrid, clearedLines, clearedCols, clearedLines.size + clearedCols.size)
    }

    private fun checkGameOver(grid: Array<Array<Color?>>, pieces: List<BlockPiece?>): Boolean {
        val availablePieces = pieces.filterNotNull()
        if (availablePieces.isEmpty()) return false
        for (piece in availablePieces) {
            for (r in 0 until GRID_SIZE) {
                for (c in 0 until GRID_SIZE) {
                    if (canPlacePieceOnGrid(piece, r, c, grid)) return false
                }
            }
        }
        return true
    }

    private fun canPlacePieceOnGrid(piece: BlockPiece, row: Int, col: Int, grid: Array<Array<Color?>>): Boolean {
        for ((dr, dc) in piece.cells) {
            val r = row + dr
            val c = col + dc
            if (r < 0 || r >= GRID_SIZE || c < 0 || c >= GRID_SIZE) return false
            if (grid[r][c] != null) return false
        }
        return true
    }

}
