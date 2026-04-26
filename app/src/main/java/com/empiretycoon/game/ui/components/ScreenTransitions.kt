package com.empiretycoon.game.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Envoltorio de [AnimatedContent] específico para transiciones entre tabs.
 * Decide el sentido del slide según el orden de tabs registrado.
 *
 * Uso típico:
 * ```
 * AnimatedTabContent(currentTab) { tab ->
 *     when (tab) { ... }
 * }
 * ```
 */
@Composable
fun AnimatedTabContent(
    targetTab: String,
    modifier: Modifier = Modifier,
    tabOrder: List<String> = defaultTabOrder,
    content: @Composable (String) -> Unit
) {
    val previousIndex = remember { intArrayOf(tabOrder.indexOf(targetTab).coerceAtLeast(0)) }

    AnimatedContent(
        targetState = targetTab,
        transitionSpec = {
            val newIdx = tabOrder.indexOf(targetState).coerceAtLeast(0)
            val oldIdx = previousIndex[0]
            val goingForward = newIdx >= oldIdx
            previousIndex[0] = newIdx
            buildTabTransform(goingForward)
        },
        label = "tabTransition",
        modifier = modifier
    ) { tab ->
        content(tab)
    }
}

private val defaultTabOrder = listOf(
    "home", "fact", "market", "research", "wealth", "player", "more"
)

private fun AnimatedContentTransitionScope<String>.buildTabTransform(
    forward: Boolean
): ContentTransform {
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(
        animationSpec = tween(durationMillis = 320)
    ) { full -> direction * full / 4 } + fadeIn(tween(280))) togetherWith
        (slideOutHorizontally(
            animationSpec = tween(durationMillis = 280)
        ) { full -> -direction * full / 4 } + fadeOut(tween(220)))
}
