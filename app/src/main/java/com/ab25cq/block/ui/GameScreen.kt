package com.ab25cq.block.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ab25cq.block.BlockPiece
import com.ab25cq.block.GameViewModel
import com.ab25cq.block.ui.theme.AccentCyan
import com.ab25cq.block.ui.theme.AccentPurple
import com.ab25cq.block.ui.theme.DarkCard
import kotlin.math.roundToInt

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val density = LocalDensity.current

    // Drag state
    var draggingPieceIndex by remember { mutableStateOf<Int>(-1) }
    var draggingPiece by remember { mutableStateOf<BlockPiece?>(null) }
    var dragWindowPos by remember { mutableStateOf(Offset.Zero) }  // finger position in window coords
    var boardWindowPos by remember { mutableStateOf(Offset.Zero) }
    var highlightCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Calc cell size in px
    val cellSizePx = with(density) { CELL_SIZE.toPx() }

    // Update highlight to exactly match FloatingPiece visual position
    fun updateHighlight() {
        val piece = draggingPiece ?: run { highlightCell = null; return }
        val maxR = piece.cells.maxOf { it.first } + 1
        val maxC = piece.cells.maxOf { it.second } + 1

        val relX = dragWindowPos.x - boardWindowPos.x
        val relY = dragWindowPos.y - boardWindowPos.y

        // FloatingPiece top-left is at:
        //   x = dragWindowPos.x - maxC * cellSizePx / 2
        //   y = dragWindowPos.y - maxR * cellSizePx / 2 - cellSizePx * 1.5
        // So board col/row of piece origin (0,0):
        val col = (relX / cellSizePx - maxC / 2f).roundToInt()
        val row = (relY / cellSizePx - maxR / 2f - 1.5f).roundToInt()

        highlightCell = if (viewModel.canPlacePiece(piece, row, col)) Pair(row, col) else null
    }

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "gradient"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated dark background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D1B2A), Color(0xFF0A0E1A), Color(0xFF050810)),
                        center = Offset(300f + gradientShift * 200f, 400f + gradientShift * 150f),
                        radius = 1400f
                    )
                )
        )

        // Decorative orbs
        Box(
            modifier = Modifier.size(220.dp).offset(x = (-50).dp, y = 30.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(listOf(AccentCyan.copy(0.18f), Color.Transparent)),
                    RoundedCornerShape(100.dp)
                )
        )
        Box(
            modifier = Modifier.size(280.dp).align(Alignment.BottomEnd).offset(x = 50.dp, y = (-80).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(listOf(AccentPurple.copy(0.13f), Color.Transparent)),
                    RoundedCornerShape(100.dp)
                )
        )
        Box(
            modifier = Modifier.size(180.dp).align(Alignment.CenterEnd).offset(x = 60.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFF6D00).copy(0.08f), Color.Transparent)),
                    RoundedCornerShape(100.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            GameHeader(score = gameState.score, highScore = gameState.highScore, combo = gameState.combo)

            Spacer(Modifier.height(14.dp))

            GameBoard(
                grid = gameState.grid,
                clearedLines = gameState.clearedLines,
                clearedCols = gameState.clearedCols,
                clearCount = gameState.clearCount,
                draggingPiece = draggingPiece,
                dragOffset = dragWindowPos,
                dragOrigin = Offset.Zero,
                highlightCell = highlightCell,
                onHighlightChanged = { highlightCell = it },
                onDrop = { row, col ->
                    if (draggingPieceIndex >= 0) {
                        viewModel.placePiece(draggingPieceIndex, row, col)
                    }
                    draggingPieceIndex = -1
                    draggingPiece = null
                    highlightCell = null
                },
                viewModel = viewModel,
                onBoardPositioned = { pos -> boardWindowPos = pos }
            )

            Spacer(Modifier.height(20.dp))

            // Piece slots with drag detection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                gameState.currentPieces.forEachIndexed { index, piece ->
                    val isDragging = draggingPieceIndex == index
                    val slotScale by animateFloatAsState(
                        targetValue = if (isDragging) 0.65f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "slot$index"
                    )

                    var slotWindowPos by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 5.dp)
                            .aspectRatio(1f)
                            .scale(slotScale)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.White.copy(0.07f), Color.White.copy(0.02f))
                                )
                            )
                            .background(DarkCard.copy(0.8f))
                            .onGloballyPositioned { coords ->
                                slotWindowPos = coords.positionInWindow()
                            }
                            .then(
                                if (piece != null) Modifier.pointerInput(piece, index) {
                                    detectDragGestures(
                                        onDragStart = { localOffset ->
                                            draggingPieceIndex = index
                                            draggingPiece = piece
                                            dragWindowPos = slotWindowPos + localOffset
                                            updateHighlight()
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            dragWindowPos = slotWindowPos + change.position
                                            updateHighlight()
                                        },
                                        onDragEnd = {
                                            val cell = highlightCell
                                            if (cell != null && draggingPieceIndex >= 0) {
                                                viewModel.placePiece(draggingPieceIndex, cell.first, cell.second)
                                            }
                                            draggingPieceIndex = -1
                                            draggingPiece = null
                                            highlightCell = null
                                        },
                                        onDragCancel = {
                                            draggingPieceIndex = -1
                                            draggingPiece = null
                                            highlightCell = null
                                        }
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PiecePreview(piece = piece, isDragging = isDragging)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Floating dragging piece
        if (draggingPiece != null) {
            val piece = draggingPiece!!
            val maxR = piece.cells.maxOf { it.first } + 1
            val maxC = piece.cells.maxOf { it.second } + 1
            val offsetX = dragWindowPos.x - (maxC * cellSizePx / 2f)
            val offsetY = dragWindowPos.y - (maxR * cellSizePx / 2f) - cellSizePx * 1.5f

            FloatingPiece(
                piece = piece,
                offset = Offset(offsetX, offsetY),
                cellSizePx = cellSizePx
            )
        }

        if (gameState.isGameOver) {
            GameOverOverlay(
                score = gameState.score,
                highScore = gameState.highScore,
                onRestart = { viewModel.startNewGame() }
            )
        }
    }
}

@Composable
fun PiecePreview(piece: BlockPiece?, isDragging: Boolean) {
    val glowPulse by rememberInfiniteTransition(label = "pg").animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pg_anim"
    )
    val alpha = if (isDragging) 0.2f else 1f

    if (piece == null) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(32.dp)) {
            drawRoundRect(color = AccentCyan.copy(0.08f), cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(
                color = AccentCyan.copy(0.15f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(1f)
            )
        }
        return
    }

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize(0.82f)) {
        val cells = piece.cells
        val maxR = cells.maxOf { it.first } + 1
        val maxC = cells.maxOf { it.second } + 1
        val cellSize = minOf(size.width / maxC, size.height / maxR) * 0.86f
        val padC = (size.width - cellSize * maxC) / 2
        val padR = (size.height - cellSize * maxR) / 2
        val pad = cellSize * 0.06f

        for ((r, c) in cells) {
            val x = padC + c * cellSize + pad
            val y = padR + r * cellSize + pad
            val cs = cellSize - pad * 2

            drawRoundRect(
                brush = Brush.radialGradient(
                    listOf(piece.glowColor.copy(0.32f * glowPulse * alpha), Color.Transparent),
                    center = Offset(x + cs / 2, y + cs / 2), radius = cs * 0.9f
                ),
                topLeft = Offset(x - cs * 0.2f, y - cs * 0.2f),
                size = Size(cs * 1.4f, cs * 1.4f), cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(piece.color.copy(alpha), piece.color.copy(0.65f * alpha)),
                    start = Offset(x, y), end = Offset(x + cs, y + cs)
                ),
                topLeft = Offset(x, y), size = Size(cs, cs), cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(0.38f * alpha), Color.Transparent),
                    startY = y, endY = y + cs * 0.45f
                ),
                topLeft = Offset(x + 2f, y + 2f), size = Size(cs - 4f, cs * 0.42f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun FloatingPiece(piece: BlockPiece, offset: Offset, cellSizePx: Float) {
    val maxR = piece.cells.maxOf { it.first } + 1
    val maxC = piece.cells.maxOf { it.second } + 1
    val widthDp = with(LocalDensity.current) { (cellSizePx * maxC).toDp() }
    val heightDp = with(LocalDensity.current) { (cellSizePx * maxR).toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(widthDp, heightDp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = cellSizePx * 0.06f
            for ((r, c) in piece.cells) {
                val x = c * cellSizePx + pad
                val y = r * cellSizePx + pad
                val cs = cellSizePx - pad * 2

                drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(piece.glowColor.copy(0.5f), Color.Transparent),
                        center = Offset(x + cs / 2, y + cs / 2), radius = cs
                    ),
                    topLeft = Offset(x - cs * 0.2f, y - cs * 0.2f),
                    size = Size(cs * 1.4f, cs * 1.4f), cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(piece.color, piece.color.copy(0.7f)),
                        start = Offset(x, y), end = Offset(x + cs, y + cs)
                    ),
                    topLeft = Offset(x, y), size = Size(cs, cs), cornerRadius = CornerRadius(7f, 7f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(0.4f), Color.Transparent),
                        startY = y, endY = y + cs * 0.45f
                    ),
                    topLeft = Offset(x + 2f, y + 2f), size = Size(cs - 4f, cs * 0.42f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
    }
}

@Composable
fun GameHeader(score: Int, highScore: Int, combo: Int) {
    val scoreAnim by animateIntAsState(targetValue = score, animationSpec = tween(300), label = "score")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                Text("SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = AccentCyan.copy(0.7f), letterSpacing = 2.sp)
                Text(scoreAnim.toString(), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        if (combo > 1) {
            Spacer(Modifier.width(10.dp))
            val cs by animateFloatAsState(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "combo")
            Box(
                modifier = Modifier
                    .scale(cs)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(AccentPurple, Color(0xFFFF6D00))))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMBO", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        color = Color.White.copy(0.8f), letterSpacing = 2.sp)
                    Text("x$combo", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        GlassCard(modifier = Modifier.weight(1f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                Text("BEST", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD740).copy(0.7f), letterSpacing = 2.sp)
                Text(highScore.toString(), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .background(DarkCard.copy(0.65f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GameOverOverlay(score: Int, highScore: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.88f)),
        contentAlignment = Alignment.Center
    ) {
        val scale by animateFloatAsState(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ov")
        Box(
            modifier = Modifier
                .scale(scale)
                .padding(28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A2235), Color(0xFF0D1422))))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GAME OVER", fontSize = 34.sp, fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1744), letterSpacing = 4.sp)
                Spacer(Modifier.height(24.dp))
                Text("SCORE", fontSize = 12.sp, color = AccentCyan.copy(0.6f), letterSpacing = 2.sp)
                Text(score.toString(), fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
                if (score >= highScore && score > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("NEW BEST!", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD740), letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(AccentCyan, Color(0xFF0091EA)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Black,
                            color = Color.Black, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
