package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

enum class NotificationKind { INFO, SUCCESS, WARNING, ERROR, ECONOMY, EVENT }

@Serializable
data class GameNotification(
    val id: Long,
    val timestamp: Long,
    val kind: NotificationKind,
    val title: String,
    val message: String
)
