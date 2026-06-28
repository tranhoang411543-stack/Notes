package com.fini.todoapp.ui.auth

import android.content.Context
import android.content.pm.PackageManager
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fini.todoapp.ui.ClearFocusLayer
import com.fini.todoapp.ui.theme.FiniBackground
import com.fini.todoapp.ui.theme.FiniBlack
import com.fini.todoapp.ui.theme.FiniBorder
import com.fini.todoapp.ui.theme.FiniGray
import kotlinx.coroutines.delay

private val AuthBackground = FiniBackground
private val AuthDark = FiniBlack
private val AuthDisabled = Color.White

@Composable
internal fun isAutomotive(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_CAR ||
        Build.DEVICE.contains("car", ignoreCase = true) ||
        Build.MODEL.contains("car", ignoreCase = true) ||
        Build.FINGERPRINT.contains("car", ignoreCase = true) ||
        (context.resources.configuration.screenWidthDp >= 800 && context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)
    }
}

@Composable
fun AuthScaffold(
    subtitle: String,
    modifier: Modifier = Modifier,
    headline: String? = null,
    toastMessage: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        ClearFocusLayer()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val compact = isWideCompactAuth()
                val isCar = isAutomotive()
                val formMaxWidth = if (isCar) 520.dp else if (compact) 430.dp else maxWidth
                val topPadding = if (isCar) 80.dp else 0.dp
                val constraintMaxHeight = maxHeight - topPadding

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = formMaxWidth)
                            .fillMaxWidth()
                            .heightIn(min = constraintMaxHeight)
                            .padding(horizontal = if (compact) 20.dp else 28.dp)
                            .padding(top = topPadding + (if (isCar) 12.dp else 24.dp), bottom = if (isCar) 12.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NoteBrand(
                            headline = headline,
                            subtitle = subtitle,
                            compact = compact
                        )

                        Spacer(
                            modifier = Modifier.height(
                                if (isCar) 16.dp else if (compact) 16.dp else 28.dp
                            )
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(
                                if (isCar) 12.dp else if (compact) 10.dp else 16.dp
                            ),
                            content = content
                        )
                    }
                }
            }
        }

        AuthTopToast(
            message = toastMessage,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun NoteBrand(
    headline: String?,
    subtitle: String,
    compact: Boolean
) {
    val isCar = isAutomotive()
    val noteFontSize = if (isCar) 36.sp else if (compact) 26.sp else 44.sp
    val subtitleFontSize = (noteFontSize.value / 2f).sp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCar) 8.dp else if (compact) 6.dp else 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(if (isCar) 96.dp else if (compact) 58.dp else 96.dp)
                .shadow(
                    elevation = if (isCar) 24.dp else if (compact) 12.dp else 22.dp,
                    shape = RoundedCornerShape(if (isCar) 20.dp else if (compact) 16.dp else 24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color.Black.copy(alpha = 0.14f)
                ),
            shape = RoundedCornerShape(if (isCar) 20.dp else if (compact) 16.dp else 24.dp),
            color = AuthDark
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isCar) 54.dp else if (compact) 34.dp else 54.dp)
                )
            }
        }

        Text(
            text = "Note",
            color = AuthDark,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = noteFontSize
            ),
            fontWeight = FontWeight.Bold
        )

        if (headline != null) {
            Text(
                text = headline,
                color = AuthDark,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (isCar) 22.sp else if (compact) 20.sp else MaterialTheme.typography.headlineMedium.fontSize
                ),
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = subtitle,
            color = FiniGray,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = subtitleFontSize,
                lineHeight = (subtitleFontSize.value * 1.2f).sp,
                fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    password: Boolean = false,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()
    val shape = RoundedCornerShape(if (isCar) 16.dp else if (compact) 12.dp else 18.dp)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCar) 68.dp else if (compact) 46.dp else 64.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        enabled = enabled,
        singleLine = true,
        leadingIcon = {
            Box(
                modifier = Modifier.padding(
                    start = if (isCar) 14.dp else if (compact) 10.dp else 12.dp,
                    end = if (isCar) 10.dp else if (compact) 6.dp else 8.dp
                )
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = AuthDark,
                    modifier = Modifier.size(if (isCar) 26.dp else if (compact) 18.dp else 24.dp)
                )
            }
        },
        trailingIcon = if (password) {
            {
                Box(
                    modifier = Modifier.padding(
                        start = if (isCar) 10.dp else if (compact) 6.dp else 8.dp,
                        end = if (isCar) 14.dp else if (compact) 10.dp else 12.dp
                    )
                ) {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(if (isCar) 38.dp else if (compact) 32.dp else 36.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = null,
                            tint = AuthDark,
                            modifier = Modifier.size(if (isCar) 26.dp else if (compact) 20.dp else 24.dp)
                        )
                    }
                }
            }
        } else {
            null
        },
        placeholder = {
            Text(
                text = placeholder,
                color = FiniGray,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isCar) 18.sp else if (compact) 12.sp else 16.sp
                )
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = AuthDark,
            fontSize = if (isCar) 18.sp else if (compact) 12.sp else 16.sp
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() },
            onSearch = { onImeAction?.invoke() }
        ),
        visualTransformation = if (password && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AuthDark,
            unfocusedTextColor = AuthDark,
            disabledTextColor = FiniGray,
            cursorColor = AuthDark,
            focusedBorderColor = FiniBorder,
            unfocusedBorderColor = FiniBorder,
            disabledBorderColor = FiniBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedPlaceholderColor = FiniGray,
            unfocusedPlaceholderColor = FiniGray,
            disabledPlaceholderColor = FiniGray
        )
    )
}

@Composable
fun AuthOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() }
        ),
        textStyle = TextStyle(
            color = Color.Transparent,
            fontSize = 1.sp
        ),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { index ->
                        val char = value.getOrNull(index)?.toString().orEmpty()
                        val selected = enabled && index == value.length.coerceAtMost(5)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCar) 64.dp else if (compact) 42.dp else 56.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.05f),
                                    spotColor = Color.Black.copy(alpha = 0.05f)
                                )
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) AuthDark else FiniBorder,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                color = if (enabled) AuthDark else FiniGray,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = if (isCar) 22.sp else if (compact) 16.sp else MaterialTheme.typography.titleLarge.fontSize
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0f)
                ) {
                    innerTextField()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPrimaryButton(
    text: String,
    loadingText: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()
    val active = enabled && !loading
    val visuallyEnabled = enabled || loading
    val shape = RoundedCornerShape(if (isCar) 16.dp else if (compact) 12.dp else 18.dp)
    val containerColor = if (visuallyEnabled) AuthDark else Color.White
    val contentColor = if (visuallyEnabled) Color.White else AuthDark

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCar) 68.dp else if (compact) 46.dp else 64.dp)
                .shadow(
                    elevation = if (visuallyEnabled) 20.dp else 34.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(
                        alpha = if (visuallyEnabled) 0.18f else 0.24f
                    ),
                    spotColor = Color.Black.copy(
                        alpha = if (visuallyEnabled) 0.18f else 0.24f
                    ),
                    clip = false
                )
                .background(
                    color = containerColor,
                    shape = shape
                )
                .then(
                    if (visuallyEnabled) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = AuthDark,
                            shape = shape
                        )
                    }
                )
                .clip(shape)
                .clickable(
                    enabled = active,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (loading) loadingText else text,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isCar) 18.sp else if (compact) 12.sp else 15.sp
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCar) 68.dp else if (compact) 46.dp else 64.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(if (isCar) 16.dp else 18.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(if (isCar) 16.dp else 18.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = AuthDark,
            disabledContainerColor = Color.White,
            disabledContentColor = AuthDark
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (isCar) 18.sp else if (compact) 12.sp else 15.sp
            ),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthInlineButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = AuthDark
            )
        ) {
            AuthUnderlinedLinkText(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isCar) 15.sp else if (compact) 12.sp else 16.sp
                )
            )
        }
    }
}

@Composable
fun AuthPromptLink(
    prefix: String,
    linkText: String,
    onClick: () -> Unit
) {
    val compact = isWideCompactAuth()
    val isCar = isAutomotive()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prefix ",
            color = AuthDark,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isCar) 15.sp else if (compact) 12.sp else 16.sp
            )
        )
        TextButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = AuthDark)
        ) {
            AuthUnderlinedLinkText(
                text = linkText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isCar) 15.sp else if (compact) 12.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun AuthUnderlinedLinkText(
    text: String,
    style: TextStyle
) {
    Text(
        text = text,
        color = AuthDark,
        style = style,
        modifier = Modifier.drawBehind {
            val y = size.height - 1.dp.toPx()
            drawLine(
                color = AuthDark,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}

@Composable
private fun isWideCompactAuth(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 700 && configuration.screenHeightDp <= 700
}

@Composable
private fun AuthTopToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    var visible by remember(message) { mutableStateOf(!message.isNullOrBlank()) }

    LaunchedEffect(message) {
        visible = !message.isNullOrBlank()
        if (visible) {
            delay(3600)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && !message.isNullOrBlank(),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 12.dp, start = 22.dp, end = 22.dp)
    ) {
        Surface(
            color = AuthDark,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp
        ) {
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
