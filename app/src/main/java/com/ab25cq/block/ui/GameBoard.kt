package com.ab25cq.block.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.ab25cq.block.BlockPiece
import com.ab25cq.block.GRID_SIZE
import com.ab25cq.block.GameViewModel
import com.ab25cq.block.ui.theme.DarkCard
import kotlinx.coroutines.delay

val CELL_SIZE = 34.dp

@Composable
fun GameBoard(
    grid: Array<Array<Color?>>,
    clearedLines: Set<Int>,
    clearedCols: Set<Int>,
    clearCount: Int,
    draggingPiece: BlockPiece?,
    @Suppress("UNUSED_PARAMETER") dragOffset: Offset,
    @Suppress("UNUSED_PARAMETER") dragOrigin: Offset,
    highlightCell: Pair<Int, Int>?,
    @Suppress("UNUSED_PARAMETER") onHighlightChanged: (Pair<Int, Int>?) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDrop: (Int, Int) -> Unit,
    viewModel: GameViewModel,
    onBoardPositioned: (Offset) -> Unit = {}
) {
    // Flash animation for cleared lines
    val flashAnim = remember { Animatable(0f) }
    var flashingLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var flashingCols by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Key on clearCount so this coroutine is never cancelled by external state reset
    LaunchedEffect(clearCount) {
        if (clearedLines.isEmpty() && clearedCols.isEmpty()) return@LaunchedEffect
        flashingLines = clearedLines
        flashingCols = clearedCols
        flashAnim.snapTo(0f)
        flashAnim.animateTo(1f, tween(120))
        delay(60)
        flashAnim.animateTo(0f, tween(120))
        flashAnim.animateTo(1f, tween(120))
        delay(180)
        flashAnim.animateTo(0f, tween(180))
        flashingLines = emptySet()
        flashingCols = emptySet()
    }

    // Glow pulse
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )

    // Calculate highlighted cell from drag position
    var boardWindowPos by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .size(CELL_SIZE * GRID_SIZE)
            .onGloballyPositioned { coords ->
                boardWindowPos = coords.positionInWindow()
                onBoardPositioned(boardWindowPos)
            }
    ) {
        val cellSizePx = size.width / GRID_SIZE
        val cellPadPx = cellSizePx * 0.05f

        // Board background
        drawRoundRect(
            color = DarkCard,
            cornerRadius = CornerRadius(16f, 16f),
            size = size
        )

        // Grid cells
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val cx = c * cellSizePx + cellPadPx
                val cy = r * cellSizePx + cellPadPx
                val cs = cellSizePx - cellPadPx * 2

                val cellColor = grid[r][c]
                val isClearing = r in flashingLines || c in flashingCols
                val isHL = isHighlightedByDrag(r, c, draggingPiece, highlightCell, viewModel)

                when {
                    isClearing -> {
                        drawRoundRect(
                            color = Color.White.copy(alpha = flashAnim.value * 0.95f),
                            topLeft = Offset(cx, cy),
                            size = Size(cs, cs),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    cellColor != null -> {
                        drawFilledCell(cx, cy, cs, cellColor, glowPulse)
                    }
                    isHL -> {
                        drawHighlightCell(cx, cy, cs, draggingPiece?.color ?: Color.White)
                    }
                    else -> {
                        drawEmptyCell(cx, cy, cs)
                    }
                }
            }
        }
    }
}

private fun isHighlightedByDrag(
    r: Int, c: Int,
    piece: BlockPiece?,
    highlightCell: Pair<Int, Int>?,
    viewModel: GameViewModel
): Boolean {
    if (piece == null || highlightCell == null) return false
    val (baseR, baseC) = highlightCell
    if (!viewModel.canPlacePiece(piece, baseR, baseC)) return false
    return piece.cells.any { (dr, dc) -> baseR + dr == r && baseC + dc == c }
}

fun DrawScope.drawFilledCell(x: Float, y: Float, size: Float, color: Color, glowPulse: Float) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.4f * glowPulse), Color.Transparent),
            center = Offset(x + size / 2, y + size / 2),
            radius = size * 0.85f
        ),
        topLeft = Offset(x - size * 0.15f, y - size * 0.15f),
        size = Size(size * 1.3f, size * 1.3f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(color, color.copy(alpha = 0.65f)),
            start = Offset(x, y),
            end = Offset(x + size, y + size)
        ),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            startY = y,
            endY = y + size * 0.5f
        ),
        topLeft = Offset(x + 2f, y + 2f),
        size = Size(size - 4f, size * 0.45f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1f)
    )
}

fun DrawScope.drawEmptyCell(x: Float, y: Float, size: Float) {
    drawRoundRect(
        color = Color.White.copy(alpha = 0.04f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.06f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(5f, 5f),
        style = Stroke(width = 0.5f)
    )
}

fun DrawScope.drawHighlightCell(x: Float, y: Float, size: Float, color: Color) {
    drawRoundRect(
        color = color.copy(alpha = 0.28f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.65f),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5f)
    )
}
