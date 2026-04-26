package com.empiretycoon.game.ui.screens

import androidx.compose.runtime.Composable
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.world.ui.AvatarCustomizerScreen

@Composable
fun AvatarScreen(state: GameState, vm: GameViewModel, onBack: () -> Unit = {}) {
    AvatarCustomizerScreen(
        currentLook = state.world.avatar.look,
        onSave = { newLook ->
            vm.updateAvatarLook(newLook)
            onBack()
        },
        onCancel = onBack
    )
}
