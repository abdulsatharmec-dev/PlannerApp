package com.dailycurator.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

enum class AppBackgroundOption(
    val storageId: String,
    val displayLabel: String,
) {
    NONE("none", "None"),
    SOFT_MIST("soft_mist", "Soft mist"),
    AURORA("aurora", "Aurora glow"),
    NIGHT_SKY("night_sky", "Night sky"),
    WARM_LINEN("warm_linen", "Warm linen"),
    SILK_MESH("silk_mesh", "Silk mesh"),
    ;

    companion object {
        fun fromStorageId(id: String?): AppBackgroundOption {
            if (id.isNullOrBlank()) return NONE
            return entries.firstOrNull { it.storageId == id } ?: NONE
        }
    }
}

val LocalAppBackgroundOption = compositionLocalOf { AppBackgroundOption.NONE }

/** Persisted `content://` URI for a user-picked wallpaper (empty = none). */
val LocalCustomWallpaperUri = compositionLocalOf { "" }

@Composable
fun backdropShowsThrough(): Boolean {
    val opt = LocalAppBackgroundOption.current
    val custom = LocalCustomWallpaperUri.current
    return opt != AppBackgroundOption.NONE || custom.isNotBlank()
}

@Composable
fun appScaffoldContainerColor(): Color {
    return if (backdropShowsThrough()) Color.Transparent else MaterialTheme.colorScheme.background
}

fun Modifier.appScreenBackground(): Modifier = composed {
    background(
        if (backdropShowsThrough()) Color.Transparent else MaterialTheme.colorScheme.background,
    )
}

/**
 * Decorative layers above the flat theme background, below the readability scrim and app content.
 * Note: [androidx.compose.ui.res.painterResource] cannot load layer-list / shape XML; mesh is drawn in Compose.
 */
@Composable
fun AppDecorBackdrop(option: AppBackgroundOption, customWallpaperUri: String) {
    val hasPreset = option != AppBackgroundOption.NONE
    val hasCustom = customWallpaperUri.isNotBlank()
    if (!hasPreset && !hasCustom) return

    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth.value <= 0f || maxHeight.value <= 0f) return@BoxWithConstraints

        if (hasPreset) {
            when (option) {
                AppBackgroundOption.SOFT_MIST -> {
                    val mid = lerp(bg, primary, 0.09f)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to bg,
                                        0.42f to mid,
                                        1f to bg,
                                    ),
                                ),
                            ),
                    )
                }
                AppBackgroundOption.AURORA -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        lerp(bg, tertiary, 0.14f),
                                        bg,
                                        lerp(bg, secondary, 0.12f),
                                        lerp(bg, primary, 0.1f),
                                    ),
                                ),
                            ),
                    )
                }
                AppBackgroundOption.WARM_LINEN -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        lerp(bg, Color(0xFFFFF8E1), 0.14f),
                                        bg,
                                        lerp(bg, secondary, 0.1f),
                                    ),
                                ),
                            ),
                    )
                }
                AppBackgroundOption.NIGHT_SKY -> NightSkyLayer(bg)
                AppBackgroundOption.SILK_MESH -> SilkMeshComposeLayer(bg, primary)
                AppBackgroundOption.NONE -> Unit
            }
        }

        if (hasCustom) {
            CustomWallpaperLayer(uriString = customWallpaperUri)
        }
    }
}

/**
 * Semi-transparent veil so dark photos, strong gradients, and mesh remain readable with transparent scaffolds.
 */
@Composable
fun WallpaperReadableScrim(wallpaperActive: Boolean, hasCustomPhoto: Boolean) {
    if (!wallpaperActive) return
    val scheme = MaterialTheme.colorScheme
    val darkUi = scheme.background.luminance() < 0.5f
    val hasPhoto = hasCustomPhoto
    val alpha = when {
        hasPhoto && darkUi -> 0.58f
        hasPhoto -> 0.64f
        darkUi -> 0.48f
        else -> 0.52f
    }
    val tint = if (darkUi) Color.Black.copy(alpha = alpha) else Color.White.copy(alpha = alpha)
    Box(Modifier.fillMaxSize().background(tint))
}

@Composable
private fun SilkMeshComposeLayer(bg: Color, primary: Color) {
    val gloss = Color.White.copy(alpha = 0.14f)
    val tintBand = lerp(bg, primary, 0.08f).copy(alpha = 0.45f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val hPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(gloss, Color.Transparent, tintBand, Color.Transparent, gloss),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, tintBand.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(wPx, hPx),
                    ),
                ),
        )
    }
}

@Composable
private fun NightSkyLayer(bg: Color) {
    val starColor = remember(bg) {
        if (bg.luminance() > 0.4f) Color(0xFF1565C0).copy(alpha = 0.12f)
        else Color.White.copy(alpha = 0.35f)
    }
    val seed = remember(bg) { bg.hashCode() }
    val random = remember(seed) { Random(seed.toLong()) }
    val stars = remember(seed) {
        List(72) {
            Triple(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat() * 1.4f + 0.4f,
            )
        }
    }
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    lerp(bg, Color(0xFF0D1B2A), 0.25f),
                    bg,
                ),
            ),
        )
        val w = size.width
        val h = size.height
        val pxPerDp = density.density
        stars.forEach { (nx, ny, r) ->
            drawCircle(
                color = starColor,
                radius = (r * 1.2f) * pxPerDp,
                center = Offset(nx * w, ny * h),
            )
        }
    }
}

@Composable
private fun CustomWallpaperLayer(uriString: String) {
    val context = LocalContext.current
    var imageBitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        if (uriString.isBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
        if (uri == null) {
            imageBitmap = null
            return@LaunchedEffect
        }
        imageBitmap = withContext(Dispatchers.IO) {
            decodeScaledBitmap(context, uri, maxSidePx = 2048)?.asImageBitmap()
        }
    }
    val bmp = imageBitmap ?: return
    Image(
        painter = BitmapPainter(bmp),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.92f,
    )
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxSidePx: Int): android.graphics.Bitmap? {
    return runCatching {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while (max(w, h) / sample > maxSidePx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}
