@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    kotlinx.coroutines.FlowPreview::class,
)

package com.dailycurator.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dailycurator.data.local.HomePdfViewMode
import com.dailycurator.data.pdf.PdfTextExtractor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private data class OpenPdf(val renderer: PdfRenderer, val pageCount: Int)

/** Pinch only when two fingers are down; does not consume one-finger drags so scroll/pan still work. */
private suspend fun PointerInputScope.detectTwoFingerPinchZoom(
    onZoom: (Float) -> Unit,
) {
    awaitEachGesture {
        var previousSpan = 0f
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            when {
                pressed.size >= 2 -> {
                    val span = pinchSpan(pressed)
                    if (previousSpan > 0f && span > 0f) {
                        val ratio = span / previousSpan
                        if (abs(ratio - 1f) > 0.005f) {
                            onZoom(ratio)
                        }
                    }
                    previousSpan = span
                    pressed.forEach { it.consume() }
                }
                pressed.isEmpty() -> break
                else -> previousSpan = 0f
            }
        }
    }
}

private fun pinchSpan(pressed: List<PointerInputChange>): Float {
    val a = pressed[0].position
    val b = pressed[1].position
    return (b - a).getDistance()
}

private fun homePdfLightColorScheme() = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    onError = Color.White,
)

private fun homePdfDarkColorScheme() = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF4C3D75),
    onPrimaryContainer = Color(0xFFE9E3FF),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF27272F),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF52525B),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005),
)

@Composable
private fun HomePdfViewerTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) homePdfDarkColorScheme() else homePdfLightColorScheme(),
        typography = MaterialTheme.typography,
        content = content,
    )
}

private val HomePdfCardContentHeight = 520.dp

private fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int, widthPx: Int): Bitmap? {
    if (pageIndex !in 0 until renderer.pageCount) return null
    val page = renderer.openPage(pageIndex)
    try {
        val w = widthPx.coerceAtLeast(1)
        val ratio = page.height.toFloat() / page.width.toFloat()
        val h = (w * ratio).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    } finally {
        page.close()
    }
}

@Composable
private fun PdfToolbarOverlay(
    modifier: Modifier = Modifier,
    viewMode: HomePdfViewMode,
    onViewModeChange: (HomePdfViewMode) -> Unit,
    themeDark: Boolean,
    onThemeDarkChange: (Boolean) -> Unit,
    onResetZoom: () -> Unit,
    fullscreenActive: Boolean,
    onCloseFullscreen: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onOpenInAdobeReader: () -> Unit,
    pdfScrollLocked: Boolean,
    onTogglePdfScrollLock: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "PDF options")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Continuous") },
                        onClick = {
                            onViewModeChange(HomePdfViewMode.CONTINUOUS)
                            menuOpen = false
                        },
                        trailingIcon = if (viewMode == HomePdfViewMode.CONTINUOUS) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Single page") },
                        onClick = {
                            onViewModeChange(HomePdfViewMode.SINGLE_PAGE)
                            menuOpen = false
                        },
                        trailingIcon = if (viewMode == HomePdfViewMode.SINGLE_PAGE) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Reading") },
                        onClick = {
                            onViewModeChange(HomePdfViewMode.READING_TEXT)
                            menuOpen = false
                        },
                        trailingIcon = if (viewMode == HomePdfViewMode.READING_TEXT) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Open in Adobe Reader") },
                        onClick = {
                            onOpenInAdobeReader()
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Reset zoom") },
                        onClick = {
                            onResetZoom()
                            menuOpen = false
                        },
                    )
                }
            }
            IconButton(
                onClick = { onThemeDarkChange(!themeDark) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    if (themeDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (themeDark) "Light theme" else "Dark theme",
                )
            }
            IconButton(
                onClick = onTogglePdfScrollLock,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    if (pdfScrollLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (pdfScrollLocked) {
                        "Unlock PDF scrolling"
                    } else {
                        "Lock PDF scrolling"
                    },
                )
            }
            if (fullscreenActive) {
                IconButton(onClick = onCloseFullscreen, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close fullscreen")
                }
            } else {
                IconButton(onClick = onOpenFullscreen, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                }
            }
        }
    }
}

@Composable
fun HomePdfReaderCard(
    uriString: String,
    lastReadPageIndex: Int,
    viewMode: HomePdfViewMode,
    onViewModeChange: (HomePdfViewMode) -> Unit,
    themeDark: Boolean,
    onThemeDarkChange: (Boolean) -> Unit,
    zoomScale: Float,
    onZoomMultiply: (Float) -> Unit,
    onResetZoom: () -> Unit,
    onVisiblePageIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uriString.isBlank()) return

    var fullscreen by remember { mutableStateOf(false) }
    /** When true, PDF pan/scroll is disabled so the home feed scrolls instead; tap lock icon to allow PDF scroll. */
    var pdfScrollLocked by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val pdfScrollEnabled = !pdfScrollLocked

    LaunchedEffect(uriString) {
        pdfScrollLocked = true
    }

    fun launchAdobeReader() {
        val uri = Uri.parse(uriString)
        val page1Based = (lastReadPageIndex + 1).coerceAtLeast(1)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            setPackage("com.adobe.reader")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("page", page1Based)
        }
        try {
            ContextCompat.startActivity(context, intent, null)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Install Adobe Acrobat Reader from the Play Store to use this option.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val adobeAction: () -> Unit = { launchAdobeReader() }

    @Composable
    fun PdfBody(scrollAreaModifier: Modifier) {
        when (viewMode) {
            HomePdfViewMode.READING_TEXT -> ReadingPdfContent(
                uriString = uriString,
                lastReadPageIndex = lastReadPageIndex,
                zoomScale = zoomScale,
                onPinchZoomMultiply = onZoomMultiply,
                onVisiblePageIndexChanged = onVisiblePageIndexChanged,
                scrollEnabled = pdfScrollEnabled,
                modifier = scrollAreaModifier,
            )
            else -> BitmapPdfContent(
                uriString = uriString,
                lastReadPageIndex = lastReadPageIndex,
                viewMode = viewMode,
                zoomScale = zoomScale,
                onPinchZoomMultiply = onZoomMultiply,
                onVisiblePageIndexChanged = onVisiblePageIndexChanged,
                scrollEnabled = pdfScrollEnabled,
                modifier = scrollAreaModifier,
            )
        }
    }

    HomePdfViewerTheme(themeDark) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HomePdfCardContentHeight)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                PdfBody(Modifier.fillMaxSize())
                PdfToolbarOverlay(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    themeDark = themeDark,
                    onThemeDarkChange = onThemeDarkChange,
                    onResetZoom = onResetZoom,
                    fullscreenActive = false,
                    onCloseFullscreen = { fullscreen = false },
                    onOpenFullscreen = { fullscreen = true },
                    onOpenInAdobeReader = adobeAction,
                    pdfScrollLocked = pdfScrollLocked,
                    onTogglePdfScrollLock = { pdfScrollLocked = !pdfScrollLocked },
                )
            }
        }
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            HomePdfViewerTheme(themeDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        PdfBody(Modifier.fillMaxSize())
                        PdfToolbarOverlay(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            themeDark = themeDark,
                            onThemeDarkChange = onThemeDarkChange,
                            onResetZoom = onResetZoom,
                            fullscreenActive = true,
                            onCloseFullscreen = { fullscreen = false },
                            onOpenFullscreen = { },
                            onOpenInAdobeReader = adobeAction,
                            pdfScrollLocked = pdfScrollLocked,
                            onTogglePdfScrollLock = { pdfScrollLocked = !pdfScrollLocked },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BitmapPdfContent(
    uriString: String,
    lastReadPageIndex: Int,
    viewMode: HomePdfViewMode,
    zoomScale: Float,
    onPinchZoomMultiply: (Float) -> Unit,
    onVisiblePageIndexChanged: (Int) -> Unit,
    scrollEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val loadState: Result<OpenPdf>? by produceState<Result<OpenPdf>?>(
        initialValue = null,
        key1 = uriString,
    ) {
        value = null
        val opened = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val pfd: ParcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: error("no pfd")
                val r = PdfRenderer(pfd)
                OpenPdf(r, r.pageCount)
            }
        }
        if (opened.isFailure) {
            value = opened
            return@produceState
        }
        val open = opened.getOrThrow()
        value = Result.success(open)
        awaitDispose {
            open.renderer.close()
        }
    }

    val success = loadState?.getOrNull()
    val pageCount = success?.pageCount ?: 0
    val baseW = with(density) {
        (context.resources.displayMetrics.widthPixels * if (viewMode == HomePdfViewMode.SINGLE_PAGE) 0.98f else 0.90f)
            .toInt()
            .coerceIn(240, 1600)
    }
    val widthPx = (baseW * zoomScale.coerceIn(0.5f, 3f)).toInt().coerceIn(200, 3200)

    val listState = rememberLazyListState()
    var didInitialListScroll by remember(uriString, viewMode) { mutableStateOf(false) }

    val initialPager = remember(uriString, pageCount, lastReadPageIndex) {
        if (pageCount <= 0) 0 else lastReadPageIndex.coerceIn(0, pageCount - 1)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPager,
        pageCount = { pageCount.coerceAtLeast(1) },
    )

    LaunchedEffect(uriString, pageCount, lastReadPageIndex, viewMode) {
        if (pageCount <= 0 || viewMode != HomePdfViewMode.CONTINUOUS || didInitialListScroll) return@LaunchedEffect
        val target = lastReadPageIndex.coerceIn(0, pageCount - 1)
        listState.scrollToItem(target)
        didInitialListScroll = true
    }

    LaunchedEffect(uriString, pageCount, lastReadPageIndex, viewMode) {
        if (pageCount <= 0 || viewMode != HomePdfViewMode.SINGLE_PAGE) return@LaunchedEffect
        val target = lastReadPageIndex.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(listState, pageCount, viewMode) {
        if (pageCount <= 0 || viewMode != HomePdfViewMode.CONTINUOUS) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(400)
            .collect { idx ->
                onVisiblePageIndexChanged(idx.coerceIn(0, pageCount - 1))
            }
    }

    LaunchedEffect(pagerState, pageCount, viewMode) {
        if (pageCount <= 0 || viewMode != HomePdfViewMode.SINGLE_PAGE) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .debounce(400)
            .collect { page ->
                onVisiblePageIndexChanged(page.coerceIn(0, pageCount - 1))
            }
    }

    when {
        loadState == null -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(36.dp))
        }
        loadState!!.isFailure -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Couldn’t open PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        success != null && pageCount > 0 -> {
            val r = success.renderer
            when (viewMode) {
                HomePdfViewMode.READING_TEXT -> Box(modifier)
                HomePdfViewMode.CONTINUOUS -> {
                    key(uriString) {
                        BoxWithConstraints(modifier) {
                            val density = LocalDensity.current
                            val pageWdp = with(density) { widthPx.toDp() }
                            val innerW = maxOf(pageWdp, maxWidth)
                            val hScroll = rememberScrollState()
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .width(innerW)
                                    .horizontalScroll(hScroll, enabled = scrollEnabled),
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(onPinchZoomMultiply) {
                                            detectTwoFingerPinchZoom(onPinchZoomMultiply)
                                        },
                                    userScrollEnabled = scrollEnabled,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(count = pageCount, key = { it }) { pageIndex ->
                                        PdfPageRow(
                                            renderer = r,
                                            pageIndex = pageIndex,
                                            widthPx = widthPx,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HomePdfViewMode.SINGLE_PAGE -> {
                    key(uriString) {
                        BoxWithConstraints(modifier) {
                            val density = LocalDensity.current
                            val pageWdp = with(density) { widthPx.toDp() }
                            val innerW = maxOf(pageWdp, maxWidth)
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .pointerInput(onPinchZoomMultiply) {
                                        detectTwoFingerPinchZoom(onPinchZoomMultiply)
                                    },
                                userScrollEnabled = scrollEnabled,
                            ) { page ->
                                key(page) {
                                    val pageHScroll = rememberScrollState()
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .horizontalScroll(pageHScroll, enabled = scrollEnabled)
                                            .width(innerW),
                                    ) {
                                        PdfPageRow(
                                            renderer = r,
                                            pageIndex = page,
                                            widthPx = widthPx,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(36.dp))
        }
    }
}

@Composable
private fun ReadingPdfContent(
    uriString: String,
    lastReadPageIndex: Int,
    zoomScale: Float,
    onPinchZoomMultiply: (Float) -> Unit,
    onVisiblePageIndexChanged: (Int) -> Unit,
    scrollEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var didInitialScroll by remember(uriString) { mutableStateOf(false) }

    val docResult: Result<PDDocument>? by produceState<Result<PDDocument>?>(
        initialValue = null,
        key1 = uriString,
    ) {
        value = null
        val opened = withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(Uri.parse(uriString))
                    ?: error("no stream")
                PDDocument.load(stream)
            }
        }
        if (opened.isFailure) {
            value = opened
            return@produceState
        }
        val doc = opened.getOrThrow()
        value = Result.success(doc)
        awaitDispose {
            runCatching { doc.close() }
        }
    }

    val doc = docResult?.getOrNull()
    val pageCount = doc?.numberOfPages ?: 0

    LaunchedEffect(uriString, pageCount, lastReadPageIndex) {
        if (pageCount <= 0 || didInitialScroll) return@LaunchedEffect
        listState.scrollToItem(lastReadPageIndex.coerceIn(0, pageCount - 1))
        didInitialScroll = true
    }

    LaunchedEffect(listState, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(400)
            .collect { idx ->
                onVisiblePageIndexChanged(idx.coerceIn(0, pageCount - 1))
            }
    }

    when {
        docResult == null -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(36.dp))
        }
        docResult!!.isFailure -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Couldn’t read PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        doc != null && pageCount > 0 -> {
            LazyColumn(
                state = listState,
                modifier = modifier
                    .pointerInput(onPinchZoomMultiply) {
                        detectTwoFingerPinchZoom(onPinchZoomMultiply)
                    },
                userScrollEnabled = scrollEnabled,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(count = pageCount, key = { it }) { pageIndex ->
                    ReadingPageBlock(
                        document = doc,
                        pageIndex = pageIndex,
                        zoomScale = zoomScale.coerceIn(0.5f, 3f),
                    )
                }
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(36.dp))
        }
    }
}

@Composable
private fun ReadingPageBlock(
    document: PDDocument,
    pageIndex: Int,
    zoomScale: Float,
) {
    var text by remember(pageIndex) { mutableStateOf<String?>(null) }
    LaunchedEffect(document, pageIndex) {
        text = withContext(Dispatchers.Default) {
            PdfTextExtractor.extractBestPageText(document, pageIndex)
        }
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        lineHeight = (24f * zoomScale).sp,
        fontSize = (17f * zoomScale).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        when (text) {
            null -> Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp))
            }
            "" -> Spacer(Modifier.height(1.dp))
            else -> Text(
                text!!,
                style = textStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(
            Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun PdfPageRow(
    renderer: PdfRenderer,
    pageIndex: Int,
    widthPx: Int,
) {
    val density = LocalDensity.current
    val pageWidthDp = with(density) { widthPx.toDp() }
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, pageIndex, widthPx) {
        bitmap?.recycle()
        bitmap = null
        val bmp = withContext(Dispatchers.Default) {
            renderPageToBitmap(renderer, pageIndex, widthPx)
        }
        if (!isActive) {
            bmp?.recycle()
            return@LaunchedEffect
        }
        bitmap = bmp
    }

    DisposableEffect(pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    val bmp = bitmap
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp == null) {
            Box(
                Modifier
                    .width(pageWidthDp)
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
        } else {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF page ${pageIndex + 1}",
                modifier = Modifier.width(pageWidthDp),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}
