package com.couplechess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.couplechess.data.model.Gender
import com.couplechess.data.model.PlayerForm
import com.couplechess.data.model.TaskLevel
import com.couplechess.ui.theme.BackgroundCard
import com.couplechess.ui.theme.BackgroundElevated
import com.couplechess.ui.theme.DividerColor
import com.couplechess.ui.theme.FemaleColor
import com.couplechess.ui.theme.Gold
import com.couplechess.ui.theme.LevelColors
import com.couplechess.ui.theme.MaleColor
import com.couplechess.ui.theme.SoftWhite
import com.couplechess.ui.theme.TextSecondary

/**
 * 性别选择器组件
 * 
 * 显示男/女两个可选按钮，选中时高亮显示
 */
@Composable
fun GenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GenderButton(
            gender = Gender.MALE,
            isSelected = selectedGender == Gender.MALE,
            onClick = { onGenderSelected(Gender.MALE) }
        )
        GenderButton(
            gender = Gender.FEMALE,
            isSelected = selectedGender == Gender.FEMALE,
            onClick = { onGenderSelected(Gender.FEMALE) }
        )
    }
}

@Composable
private fun GenderButton(
    gender: Gender,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (gender) {
                Gender.MALE -> MaleColor.copy(alpha = 0.2f)
                Gender.FEMALE -> FemaleColor.copy(alpha = 0.2f)
            }
        } else {
            BackgroundElevated
        },
        animationSpec = tween(200),
        label = "genderBgColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (gender) {
                Gender.MALE -> MaleColor
                Gender.FEMALE -> FemaleColor
            }
        } else {
            DividerColor
        },
        animationSpec = tween(200),
        label = "genderBorderColor"
    )
    
    val iconColor = when (gender) {
        Gender.MALE -> MaleColor
        Gender.FEMALE -> FemaleColor
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(150),
        label = "genderScale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (gender) {
                Gender.MALE -> Icons.Default.Male
                Gender.FEMALE -> Icons.Default.Female
            },
            contentDescription = when (gender) {
                Gender.MALE -> "男性"
                Gender.FEMALE -> "女性"
            },
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * 阶段等级选择滑块
 * 
 * 滑动选择 L1-L5 等级，显示等级名称和颜色指示
 */
@Composable
fun LevelSlider(
    currentLevel: TaskLevel,
    onLevelChange: (TaskLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = TaskLevel.entries
    val currentIndex = levels.indexOf(currentLevel).coerceIn(0, levels.size - 1)
    val levelColor = LevelColors.getOrElse(currentIndex) { LevelColors.first() }
    
    Column(modifier = modifier) {
        // 等级名称显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前阶段",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(levelColor)
                )
                Text(
                    text = "${currentLevel.name} · ${currentLevel.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 滑块
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { newValue ->
                val newIndex = newValue.toInt().coerceIn(0, levels.size - 1)
                onLevelChange(levels[newIndex])
            },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = levels.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = levelColor,
                inactiveTrackColor = DividerColor
            )
        )
        
        // 等级标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            levels.forEach { level ->
                Text(
                    text = level.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (level == currentLevel) Gold else TextSecondary,
                    fontWeight = if (level == currentLevel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 玩家卡片组件
 * 
 * 显示单个玩家的完整设置：名称输入、性别选择、等级滑块
 */
@Composable
fun PlayerCard(
    index: Int,
    playerForm: PlayerForm,
    onNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onLevelChange: (TaskLevel) -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val levelIndex = TaskLevel.entries.indexOf(playerForm.currentLevel)
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            BackgroundCard,
            LevelColors.getOrElse(levelIndex) { BackgroundCard }.copy(alpha = 0.3f)
        )
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(16.dp)
        ) {
            Column {
                // 标题栏（玩家编号 + 删除按钮）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "玩家 ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    
                    if (onRemove != null) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "移除玩家",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 名称输入 + 性别选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = playerForm.name,
                        onValueChange = onNameChange,
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = DividerColor,
                            focusedLabelColor = Gold,
                            cursorColor = Gold
                        )
                    )
                    
                    GenderSelector(
                        selectedGender = playerForm.gender,
                        onGenderSelected = onGenderChange
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 等级选择
                LevelSlider(
                    currentLevel = playerForm.currentLevel,
                    onLevelChange = onLevelChange
                )
            }
        }
    }
}

/**
 * 添加玩家按钮组件
 */
@Composable
fun AddPlayerButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (enabled) Gold.copy(alpha = 0.5f) else DividerColor.copy(alpha = 0.3f),
        label = "addBtnBorder"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(if (enabled) BackgroundCard.copy(alpha = 0.5f) else Color.Transparent)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (enabled) "+ 添加玩家" else "已达最大人数",
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Gold else TextSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
