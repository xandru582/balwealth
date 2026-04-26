package com.empiretycoon.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.ui.components.BalWealthDetail
import com.empiretycoon.game.ui.theme.Ink

@Composable
fun BalWealthScreen(state: GameState, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
    ) {
        BalWealthDetail(state.balWealth)
        Spacer(Modifier.height(60.dp))
    }
}
