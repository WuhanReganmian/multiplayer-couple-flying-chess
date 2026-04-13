package com.couplechess.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 品牌基础色 =====
val DeepPurple = Color(0xFF2D1B3D)
val DarkRed = Color(0xFF4A1A1F)
val PureBlack = Color(0xFF0A0A0A)
val Gold = Color(0xFFD4AF37)
val GoldLight = Color(0xFFE8C84A)
val SoftWhite = Color(0xFFF5F0EB)
val MutedPurple = Color(0xFF6B4D7A)
val WarmRed = Color(0xFF8B2F3A)

// ===== 背景色 =====
val BackgroundPrimary = PureBlack
val BackgroundCard = Color(0xFF1A0D24)
val BackgroundElevated = Color(0xFF221533)
val DividerColor = Color(0xFF3D2B4D)

// ===== 文字色 =====
val TextPrimary = SoftWhite
val TextSecondary = Color(0xFFB8A9CC)
val TextHint = Color(0xFF7A6B8A)
val TextGold = Gold

// ===== 按钮色 =====
val ButtonPrimary = Gold
val ButtonPrimaryText = PureBlack
val ButtonSecondary = MutedPurple
val ButtonDanger = Color(0xFF9B2335)

// ===== 阶段背景渐变（L1 → L5 从浅到深红）=====
val LevelColors = listOf(
    Color(0xFF3D2B4D),  // L1 深紫
    Color(0xFF4A2340),  // L2 紫红
    Color(0xFF5A1D35),  // L3 中红
    Color(0xFF6A1528),  // L4 深红
    Color(0xFF7A0D1B),  // L5 最深红
)

// ===== 性别标识色 =====
val MaleColor = Color(0xFF4A90D9)
val FemaleColor = Color(0xFFD9509A)

// ===== 任务格色 =====
val TaskCellColor = Gold
val NormalCellColor = Color(0xFF3D2B4D)
val StartCellColor = Color(0xFF2A8040)
val FinishCellColor = Color(0xFFD4AF37)
