package dev.ilamparithi.aournalpp.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

val DEFAULT_PRESET_FOLDER_COLORS = listOf(
    "#3F51B5", // Indigo
    "#009688", // Teal
    "#4CAF50", // Emerald Green
    "#FF9800", // Amber
    "#E91E63", // Pink
    "#9C27B0", // Purple
    "#00BCD4", // Cyan
    "#F44336", // Coral Red
    "#795548", // Brown
    "#607D8B"  // Slate Blue
)

fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(AndroidColor.HSVToColor(hsv))
}

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    AndroidColor.colorToHSV(AndroidColor.rgb(r, g, b), hsv)
    return hsv
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
}

/**
 * An expressive folder color picker row with:
 * 1. Curated preset color circles
 * 2. Active custom color indicator
 * 3. Rainbow palette button opening a visual RGB/HSV palette picker
 */
@Composable
fun FolderColorPickerRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    presetColors: List<String> = DEFAULT_PRESET_FOLDER_COLORS,
    modifier: Modifier = Modifier
) {
    var showCustomColorDialog by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Preset Colors
        items(presetColors) { colorHex ->
            val color = remember(colorHex) {
                try { Color(AndroidColor.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
            }
            val isSelected = colorHex.equals(selectedColorHex, ignoreCase = true)
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onColorSelected(colorHex) }
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 2. Custom selected color (if active and not in presets)
        val isCustomSelected = presetColors.none { it.equals(selectedColorHex, ignoreCase = true) }
        if (isCustomSelected) {
            item {
                val customColor = remember(selectedColorHex) {
                    try { Color(AndroidColor.parseColor(selectedColorHex)) } catch (e: Exception) { Color.Gray }
                }
                Surface(
                    shape = CircleShape,
                    color = customColor,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { showCustomColorDialog = true }
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 3. Rainbow Palette '+' Button for Custom Color Picker
        item {
            val rainbowBrush = Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
            )
            Surface(
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rainbowBrush)
                    .clickable { showCustomColorDialog = true }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Visual Color Picker",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColorHex = selectedColorHex,
            onDismiss = { showCustomColorDialog = false },
            onColorSelected = onColorSelected
        )
    }
}

/**
 * A visual 2D RGB/HSV Palette Picker Dialog with:
 * - Interactive 2D Saturation / Value Gradient Canvas with touch selector
 * - Continuous 1D Rainbow Hue Slider Bar with touch selector
 * - RGB sliders (Red, Green, Blue) & Hex code input
 * - Live color preview banner
 */
@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialColor = remember(initialColorHex) {
        try { Color(AndroidColor.parseColor(initialColorHex)) } catch (e: Exception) { Color(0xFF4CAF50) }
    }
    val initialHsv = remember(initialColor) { colorToHsv(initialColor) }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    var redInt by remember { mutableIntStateOf((initialColor.red * 255).toInt()) }
    var greenInt by remember { mutableIntStateOf((initialColor.green * 255).toInt()) }
    var blueInt by remember { mutableIntStateOf((initialColor.blue * 255).toInt()) }

    var hexInput by remember { mutableStateOf(colorToHex(initialColor)) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Visual 2D Canvas, 1 = RGB Sliders

    val currentColor = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value)
    }

    fun updateFromHsv(newH: Float, newS: Float, newV: Float) {
        hue = newH
        saturation = newS
        value = newV
        val c = hsvToColor(newH, newS, newV)
        redInt = (c.red * 255).toInt()
        greenInt = (c.green * 255).toInt()
        blueInt = (c.blue * 255).toInt()
        hexInput = colorToHex(c)
    }

    fun updateFromRgb(r: Int, g: Int, b: Int) {
        redInt = r.coerceIn(0, 255)
        greenInt = g.coerceIn(0, 255)
        blueInt = b.coerceIn(0, 255)
        val c = Color(redInt, greenInt, blueInt)
        val hsv = colorToHsv(c)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = colorToHex(c)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Visual Color Palette", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Color Preview Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = currentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val lum = 0.299f * currentColor.red + 0.587f * currentColor.green + 0.114f * currentColor.blue
                        val textColor = if (lum > 0.55f) Color(0xFF191C1D) else Color.White
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = colorToHex(currentColor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "RGB(${redInt}, ${greenInt}, ${blueInt})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Mode Tabs (Visual 2D Palette vs RGB Sliders)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Visual Palette", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("RGB Sliders", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // 1. 2D Saturation / Value Visual Canvas
                    val pureHueColor = remember(hue) {
                        Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Shade & Saturation Canvas", style = MaterialTheme.typography.labelMedium)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pureHueColor)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        val s = (down.position.x / size.width).coerceIn(0f, 1f)
                                        val v = (1f - (down.position.y / size.height)).coerceIn(0f, 1f)
                                        updateFromHsv(hue, s, v)

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (!change.pressed) break
                                            change.consume()
                                            val curS = (change.position.x / size.width).coerceIn(0f, 1f)
                                            val curV = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                            updateFromHsv(hue, curS, curV)
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Horizontal White -> Transparent gradient
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.White, Color.Transparent)
                                    )
                                )
                                // Vertical Transparent -> Black gradient
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black)
                                    )
                                )

                                // Current selected point ring indicator
                                val targetX = saturation * size.width
                                val targetY = (1f - value) * size.height
                                drawCircle(
                                    color = Color.White,
                                    radius = 11.dp.toPx(),
                                    center = Offset(targetX, targetY),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.Black,
                                    radius = 8.dp.toPx(),
                                    center = Offset(targetX, targetY),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }

                    // 2. 1D Rainbow Hue Slider Bar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hue Spectrum", style = MaterialTheme.typography.labelMedium)
                            Text("${hue.toInt()}°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        val rainbowColors = remember {
                            listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        val newH = ((down.position.x / size.width) * 360f).coerceIn(0f, 360f)
                                        updateFromHsv(newH, saturation, value)

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (!change.pressed) break
                                            change.consume()
                                            val curH = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                                            updateFromHsv(curH, saturation, value)
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    brush = Brush.horizontalGradient(rainbowColors)
                                )
                                // Hue indicator thumb
                                val targetX = (hue / 360f) * size.width
                                drawCircle(
                                    color = Color.White,
                                    radius = 13.dp.toPx(),
                                    center = Offset(targetX, size.height / 2),
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(targetX, size.height / 2),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                } else {
                    // RGB Sliders Mode
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Red Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Red (R)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                Text("$redInt", style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(
                                value = redInt.toFloat(),
                                onValueChange = { updateFromRgb(it.toInt(), greenInt, blueInt) },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFE53935),
                                    activeTrackColor = Color(0xFFE53935)
                                )
                            )
                        }

                        // Green Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Green (G)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                                Text("$greenInt", style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(
                                value = greenInt.toFloat(),
                                onValueChange = { updateFromRgb(redInt, it.toInt(), blueInt) },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF43A047),
                                    activeTrackColor = Color(0xFF43A047)
                                )
                            )
                        }

                        // Blue Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Blue (B)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                                Text("$blueInt", style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(
                                value = blueInt.toFloat(),
                                onValueChange = { updateFromRgb(redInt, greenInt, it.toInt()) },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF1E88E5),
                                    activeTrackColor = Color(0xFF1E88E5)
                                )
                            )
                        }
                    }
                }

                // Direct Hex Input field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        hexInput = raw
                        val clean = if (raw.startsWith("#")) raw else "#$raw"
                        try {
                            if (clean.length == 7) {
                                val parsed = Color(AndroidColor.parseColor(clean))
                                val r = (parsed.red * 255).toInt()
                                val g = (parsed.green * 255).toInt()
                                val b = (parsed.blue * 255).toInt()
                                val hsv = colorToHsv(parsed)
                                redInt = r
                                greenInt = g
                                blueInt = b
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            }
                        } catch (_: Exception) {}
                    },
                    label = { Text("Hex Color Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onColorSelected(colorToHex(currentColor))
                onDismiss()
            }) {
                Text("Select Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
