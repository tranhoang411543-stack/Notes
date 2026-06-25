package com.fini.todoapp.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.CompositionLocalProvider

private val ActionDark = Color(0xFF333333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val containerColor = if (enabled) ActionDark else Color.White
    val contentColor = if (enabled) Color.White else ActionDark

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "mono-button-scale"
    )

    val whiteRippleConfig = RippleConfiguration(
        color = Color.White,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.35f,
            focusedAlpha = 0.25f,
            hoveredAlpha = 0.15f,
            draggedAlpha = 0.15f
        )
    )

    CompositionLocalProvider(LocalRippleConfiguration provides whiteRippleConfig) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .height(height)
                .shadow(
                    elevation = if (enabled) 10.dp else 16.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (enabled) 0.15f else 0.20f),
                    spotColor = Color.Black.copy(alpha = if (enabled) 0.15f else 0.20f),
                    clip = false
                )
                .then(
                    if (enabled) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, ActionDark, shape)
                    }
                ),
            enabled = enabled,
            color = containerColor,
            shape = shape,
            interactionSource = interactionSource
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
