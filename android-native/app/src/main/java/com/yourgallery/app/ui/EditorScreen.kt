package com.yourgallery.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.yourgallery.app.DeviceMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    item: DeviceMedia,
    imageLoader: ImageLoader,
    isSaving: Boolean = false,
    onClose: () -> Unit,
    onSave: (rotation: Float, exposure: Float, contrast: Float, saturation: Float, filter: String) -> Unit
) {
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var activeTab by remember { mutableStateOf("Adjust") }

    val filters = listOf("Original", "Rose Film", "Soft Glow", "Vivid", "Golden", "Dramatic", "Mono", "Noir")
    var selectedFilter by remember { mutableStateOf("Original") }

    // Dynamic ColorMatrix applying both sliders and preset filters
    val colorMatrix = remember(exposure, contrast, saturation, selectedFilter) {
        val matrix = ColorMatrix()

        // Apply preset filter transform first
        when (selectedFilter) {
            "Rose Film" -> {
                val rose = ColorMatrix(floatArrayOf(
                    1.15f, 0.05f, 0.05f, 0f, 15f,
                    0.05f, 0.95f, 0.05f, 0f, 5f,
                    0.05f, 0.05f, 1.05f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.timesAssign(rose)
            }
            "Soft Glow" -> {
                val glow = ColorMatrix(floatArrayOf(
                    1.08f, 0.02f, 0.02f, 0f, 10f,
                    0.02f, 1.05f, 0.02f, 0f, 10f,
                    0.02f, 0.02f, 1.02f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.timesAssign(glow)
            }
            "Vivid" -> {
                val vivid = ColorMatrix().apply { setToSaturation(1.6f) }
                matrix.timesAssign(vivid)
            }
            "Golden" -> {
                val golden = ColorMatrix(floatArrayOf(
                    1.25f, 0.05f, 0f, 0f, 12f,
                    0.05f, 1.10f, 0f, 0f, 8f,
                    0f, 0f, 0.85f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.timesAssign(golden)
            }
            "Dramatic" -> {
                val dramatic = ColorMatrix().apply {
                    setToSaturation(0.6f)
                }
                matrix.timesAssign(dramatic)
            }
            "Mono" -> {
                val mono = ColorMatrix().apply { setToSaturation(0f) }
                matrix.timesAssign(mono)
            }
            "Noir" -> {
                val noir = ColorMatrix().apply { setToSaturation(0f) }
                matrix.timesAssign(noir)
            }
        }

        // Apply Exposure/Brightness
        if (exposure != 0f) {
            val brightnessAdd = exposure * 100f
            val brightMat = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessAdd,
                0f, 1f, 0f, 0f, brightnessAdd,
                0f, 0f, 1f, 0f, brightnessAdd,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(brightMat)
        }

        // Apply Contrast
        if (contrast != 1f) {
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMat = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(contrastMat)
        }

        // Apply Saturation
        if (saturation != 1f && selectedFilter != "Mono" && selectedFilter != "Noir") {
            val satMat = ColorMatrix().apply { setToSaturation(saturation) }
            matrix.timesAssign(satMat)
        }

        matrix
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 Degrees")
                    }
                    IconButton(
                        onClick = { onSave(rotationDegrees, exposure, contrast, saturation, selectedFilter) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.uri,
                    imageLoader = imageLoader,
                    contentDescription = "Editing image",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotationDegrees)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                // Quick Actions: One-Tap Remaster & Reset
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            // Samsung One-Tap Remaster / Auto-Enhance algorithm
                            exposure = 0.08f
                            contrast = 1.22f
                            saturation = 1.18f
                            selectedFilter = "Vivid"
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto Remaster", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            exposure = 0f
                            contrast = 1f
                            saturation = 1f
                            selectedFilter = "Original"
                            rotationDegrees = 0f
                        }
                    ) {
                        Text("Reset", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (activeTab == "Adjust") {
                    Text("Exposure", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    Slider(value = exposure, onValueChange = { exposure = it }, valueRange = -1f..1f)

                    Text("Contrast", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.5f..2f)

                    Text("Saturation", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
                } else if (activeTab == "Filters") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filters) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { activeTab = "Adjust" }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = "Adjust",
                            tint = if (activeTab == "Adjust") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { activeTab = "Filters" }) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Filters",
                            tint = if (activeTab == "Filters") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { activeTab = "Crop" }) {
                        Icon(
                            Icons.Filled.Crop,
                            contentDescription = "Crop",
                            tint = if (activeTab == "Crop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
