package dev.ilamparithi.aournalpp.ui.settings.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Standardized Material 3 editable value pill for settings sliders.
 *
 * Features:
 * - Direct tap-to-edit numerical input.
 * - Strict limit enforcement with immediate clamping on overflow (e.g. typing 999% clamps to 300%).
 * - Instant debounced Toast notifications when user attempts to exceed limits.
 * - Automatic correction and clamping on focus loss if user leaves an underflow or empty value.
 * - Clean bidirectional synchronization with external state changes (e.g. slider drags, reset actions).
 */
@Composable
fun ValueEditPill(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    unitSuffix: String,
    limitToastMessage: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(value.toString(), TextRange(value.toString().length)))
    }

    var activeToast by remember { mutableStateOf<Toast?>(null) }

    fun showLimitToast() {
        activeToast?.cancel()
        activeToast = Toast.makeText(context, limitToastMessage, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    // Synchronize with external value changes (slider movement, reset button) when not actively typing
    LaunchedEffect(value) {
        if (!isFocused) {
            val text = value.toString()
            textFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { tfv ->
                    val digits = tfv.text.filter { it.isDigit() }
                    val parsedLong = digits.toLongOrNull()

                    when {
                        digits.isEmpty() -> {
                            textFieldValue = tfv.copy(text = "", selection = TextRange(0))
                        }
                        parsedLong != null && parsedLong > valueRange.last -> {
                            showLimitToast()
                            val clampedStr = valueRange.last.toString()
                            textFieldValue = tfv.copy(
                                text = clampedStr,
                                selection = TextRange(clampedStr.length)
                            )
                            onValueChange(valueRange.last)
                        }
                        parsedLong != null && parsedLong in valueRange -> {
                            textFieldValue = tfv.copy(
                                text = digits,
                                selection = TextRange(digits.length)
                            )
                            onValueChange(parsedLong.toInt())
                        }
                        else -> {
                            textFieldValue = tfv.copy(
                                text = digits,
                                selection = TextRange(digits.length)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .widthIn(min = 28.dp, max = 56.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            val parsed = textFieldValue.text.toIntOrNull()
                            if (parsed == null || parsed < valueRange.first) {
                                showLimitToast()
                                val minVal = valueRange.first
                                val minStr = minVal.toString()
                                textFieldValue = TextFieldValue(minStr, TextRange(minStr.length))
                                onValueChange(minVal)
                            } else if (parsed > valueRange.last) {
                                showLimitToast()
                                val maxVal = valueRange.last
                                val maxStr = maxVal.toString()
                                textFieldValue = TextFieldValue(maxStr, TextRange(maxStr.length))
                                onValueChange(maxVal)
                            } else {
                                onValueChange(parsed)
                            }
                        }
                    },
                enabled = enabled,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )
            if (unitSuffix.isNotEmpty()) {
                VerticalDivider(
                    modifier = Modifier
                        .height(14.dp)
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = unitSuffix,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Decimal variant of [ValueEditPill] for fractional values (e.g. scales, multipliers).
 */
@Composable
fun DecimalValueEditPill(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unitSuffix: String,
    limitToastMessage: String,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 2,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    fun formatValue(v: Float): String {
        return String.format(java.util.Locale.US, "%.${decimalPlaces}f", v)
    }

    var textFieldValue by remember {
        val initialText = formatValue(value)
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }

    var activeToast by remember { mutableStateOf<Toast?>(null) }

    fun showLimitToast() {
        activeToast?.cancel()
        activeToast = Toast.makeText(context, limitToastMessage, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    LaunchedEffect(value) {
        if (!isFocused) {
            val text = formatValue(value)
            textFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { tfv ->
                    val filtered = buildString {
                        var hasDot = false
                        for (ch in tfv.text) {
                            if (ch.isDigit()) {
                                append(ch)
                            } else if (ch == '.' && !hasDot) {
                                hasDot = true
                                append(ch)
                            }
                        }
                    }

                    val parsedFloat = filtered.toFloatOrNull()

                    when {
                        filtered.isEmpty() -> {
                            textFieldValue = tfv.copy(text = "", selection = TextRange(0))
                        }
                        parsedFloat != null && parsedFloat > valueRange.endInclusive -> {
                            showLimitToast()
                            val clampedStr = formatValue(valueRange.endInclusive)
                            textFieldValue = tfv.copy(
                                text = clampedStr,
                                selection = TextRange(clampedStr.length)
                            )
                            onValueChange(valueRange.endInclusive)
                        }
                        parsedFloat != null && parsedFloat in valueRange -> {
                            textFieldValue = tfv.copy(
                                text = filtered,
                                selection = TextRange(filtered.length.coerceAtMost(tfv.selection.end))
                            )
                            onValueChange(parsedFloat)
                        }
                        else -> {
                            textFieldValue = tfv.copy(
                                text = filtered,
                                selection = TextRange(filtered.length.coerceAtMost(tfv.selection.end))
                            )
                        }
                    }
                },
                modifier = Modifier
                    .widthIn(min = 32.dp, max = 56.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            val parsed = textFieldValue.text.toFloatOrNull()
                            if (parsed == null || parsed < valueRange.start) {
                                showLimitToast()
                                val minVal = valueRange.start
                                val minStr = formatValue(minVal)
                                textFieldValue = TextFieldValue(minStr, TextRange(minStr.length))
                                onValueChange(minVal)
                            } else if (parsed > valueRange.endInclusive) {
                                showLimitToast()
                                val maxVal = valueRange.endInclusive
                                val maxStr = formatValue(maxVal)
                                textFieldValue = TextFieldValue(maxStr, TextRange(maxStr.length))
                                onValueChange(maxVal)
                            } else {
                                val normStr = formatValue(parsed)
                                textFieldValue = TextFieldValue(normStr, TextRange(normStr.length))
                                onValueChange(parsed)
                            }
                        }
                    },
                enabled = enabled,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )
            if (unitSuffix.isNotEmpty()) {
                VerticalDivider(
                    modifier = Modifier
                        .height(14.dp)
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = unitSuffix,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Standardized Material 3 editable value pill overload for floating-point / decimal values.
 */
@Composable
fun ValueEditPill(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unitSuffix: String,
    limitToastMessage: String,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 2,
    enabled: Boolean = true
) {
    DecimalValueEditPill(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        unitSuffix = unitSuffix,
        limitToastMessage = limitToastMessage,
        modifier = modifier,
        decimalPlaces = decimalPlaces,
        enabled = enabled
    )
}

