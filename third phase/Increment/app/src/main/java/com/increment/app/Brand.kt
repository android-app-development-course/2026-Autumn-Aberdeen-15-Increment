package com.increment.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun IncrementLogo(modifier: Modifier = Modifier) {
    Box(modifier.background(colorResource(R.color.increment_mark_fill), CircleShape)) {
        IncrementGlyph(Modifier.fillMaxSize())
    }
}

@Composable
private fun IncrementGlyph(modifier: Modifier) {
    Image(painterResource(R.drawable.ic_increment), contentDescription = null, modifier = modifier)
}

@Composable
internal fun LaunchBrand(splashExited: () -> Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var laidOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { laidOut }.first { it }
        withTimeoutOrNull(500) { snapshotFlow(splashExited).first { it } }
        progress.animateTo(1f, tween(durationMillis = 1100, easing = LinearEasing))
        onFinished()
    }

    val time = progress.value
    val expansion = FastOutSlowInEasing.transform(phase(time, .16f, .58f))
    val glyphOpacity = 1f - phase(time, .24f, .46f)
    val wordOpacity = phase(time, .39f, .64f)
    val exit = FastOutSlowInEasing.transform(phase(time, .77f, 1f))
    val markFill = colorResource(R.color.increment_mark_fill)
    val markInk = colorResource(R.color.increment_mark_ink)
    val wordStyle = with(LocalDensity.current) {
        MaterialTheme.typography.headlineLarge.copy(
            fontSize = 32.dp.toSp(), lineHeight = 40.dp.toSp(),
            fontWeight = FontWeight.Bold, letterSpacing = (-.65).dp.toSp()
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { laidOut = true }
            .graphicsLayer { alpha = 1f - exit }
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = lerp(104.dp, 230.dp, expansion), height = lerp(104.dp, 78.dp, expansion))
                .graphicsLayer {
                    scaleX = 1f - .06f * exit
                    scaleY = scaleX
                    shape = CircleShape
                    clip = true
                }
                .background(markFill),
            contentAlignment = Alignment.Center
        ) {
            if (glyphOpacity > 0f) {
                IncrementGlyph(Modifier.requiredSize(108.dp).graphicsLayer { alpha = glyphOpacity })
            }
            if (wordOpacity > 0f) {
                Text(
                    text = "increment",
                    modifier = Modifier.requiredWidth(222.dp).graphicsLayer { alpha = wordOpacity },
                    color = markInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = wordStyle
                )
            }
        }
    }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)
