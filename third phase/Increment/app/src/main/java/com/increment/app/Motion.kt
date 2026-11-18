package com.increment.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal fun contentFadeIn() = fadeIn(tween(200, easing = LinearOutSlowInEasing))
internal fun contentFadeOut() = fadeOut(tween(120))

// Dispose the previous page as soon as navigation changes. It may own a player or recorder.
@Composable
internal fun PageEntrance(
    destination: Any,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    content: @Composable () -> Unit
) {
    key(destination) {
        val offset = with(LocalDensity.current) { 8.dp.roundToPx() }
        val visibility = remember { MutableTransitionState(!animate).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visibility,
            modifier = modifier,
            enter = if (animate) {
                contentFadeIn() + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { offset }
                )
            } else EnterTransition.None
        ) {
            content()
        }
    }
}
