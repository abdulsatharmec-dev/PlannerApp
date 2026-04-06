package com.dailycurator.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import com.dailycurator.data.chat.PendingChatDeletion
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

private data class ChatThemeColors(
    val screenBg: Color,
    val userBubble: Color,
    val userText: Color,
    val assistantBubble: Color,
    val assistantText: Color,
    val metaText: Color,
    val composerSurface: Color,
    val composerBorder: Color,
    val composerText: Color,
    val linkColor: Color,
    val codeBg: Color,
    val inlineCodeBg: Color,
    val sendFab: Color,
    val sendFabContent: Color,
)

@Composable
private fun rememberChatThemeColors(): ChatThemeColors {
    val bgLum = MaterialTheme.colorScheme.background.luminance()
    val dark = bgLum < 0.5f
    return remember(dark) {
        if (dark) {
            ChatThemeColors(
                screenBg = Color(0xFF0B141A),
                userBubble = Color(0xFF005C4B),
                userText = Color(0xFFE7FFEC),
                assistantBubble = Color(0xFF1F2C34),
                assistantText = Color(0xFFE9EDEF),
                metaText = Color(0xFF8696A0),
                composerSurface = Color(0xFF1F2C34),
                composerBorder = Color(0xFF2A3942),
                composerText = Color(0xFFE9EDEF),
                linkColor = Color(0xFF53BDEB),
                codeBg = Color(0xFF111B21),
                inlineCodeBg = Color(0xFF2A3942),
                sendFab = Color(0xFF00A884),
                sendFabContent = Color.White,
            )
        } else {
            ChatThemeColors(
                screenBg = Color(0xFFECE5DD),
                userBubble = Color(0xFFDCF8C6),
                userText = Color(0xFF111B21),
                assistantBubble = Color(0xFFFFFFFF),
                assistantText = Color(0xFF111B21),
                metaText = Color(0xFF667781),
                composerSurface = Color(0xFFF0F2F5),
                composerBorder = Color(0xFFD1D7DB),
                composerText = Color(0xFF111B21),
                linkColor = Color(0xFF027EB5),
                codeBg = Color(0xFFE8EBEF),
                inlineCodeBg = Color(0xFFE8EBEF),
                sendFab = Color(0xFF25D366),
                sendFabContent = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingDeletion by viewModel.pendingDeletion.collectAsState()
    val pendingMemoryProposal by viewModel.pendingMemoryProposal.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val chatColors = rememberChatThemeColors()
    val messageTextStyle = remember {
        TextStyle(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.01.sp)
    }

    pendingMemoryProposal?.let { lines ->
        val stableKey = lines.joinToString("\u0001")
        var checked by remember(stableKey) { mutableStateOf(List(lines.size) { true }) }
        LaunchedEffect(stableKey) {
            checked = List(lines.size) { true }
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissMemoryProposal() },
            title = { Text("Save to memory?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Suggested from what you wrote in chat. Uncheck anything you do not want stored.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    lines.forEachIndexed { i, line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked.getOrElse(i) { false },
                                onCheckedChange = { v ->
                                    checked = checked.mapIndexed { j, b -> if (j == i) v else b }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmMemoryProposal(
                            lines.filterIndexed { i, _ -> checked.getOrElse(i) { false } },
                        )
                    },
                ) {
                    Text("Add selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMemoryProposal() }) {
                    Text("Skip all")
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Start new chat?") },
            text = { Text("This removes the conversation from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearChat()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.screenBg)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { showClearDialog = true },
                enabled = messages.isNotEmpty() || isLoading,
            ) {
                Text("New chat", color = chatColors.linkColor)
            }
        }

        pendingDeletion?.let { pending ->
            val label = when (pending) {
                is PendingChatDeletion.Task -> "Delete task \"${pending.title}\"?"
                is PendingChatDeletion.Goal -> "Delete goal \"${pending.title}\"?"
                is PendingChatDeletion.Habit -> "Delete habit \"${pending.title}\"?"
            }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.dismissPendingDeletion() }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = { viewModel.confirmPendingDeletion() }) {
                        Text("Confirm")
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RectangleShape),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = messages,
                key = { it.id },
            ) { message ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    MessageBubble(
                        message = message,
                        colors = chatColors,
                        messageTextStyle = messageTextStyle,
                        maxBubbleWidth = maxWidth * 0.84f,
                    )
                }
            }
            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(28.dp),
                            strokeWidth = 2.dp,
                            color = chatColors.linkColor,
                        )
                    }
                }
            }
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            color = chatColors.composerSurface,
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Message",
                            color = chatColors.metaText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        color = chatColors.composerText,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = chatColors.composerSurface,
                        unfocusedContainerColor = chatColors.composerSurface,
                        focusedTextColor = chatColors.composerText,
                        unfocusedTextColor = chatColors.composerText,
                        focusedBorderColor = chatColors.composerBorder,
                        unfocusedBorderColor = chatColors.composerBorder,
                        cursorColor = chatColors.linkColor,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    containerColor = if (isLoading) {
                        chatColors.composerBorder.copy(alpha = 0.4f)
                    } else {
                        chatColors.sendFab
                    },
                    contentColor = if (isLoading) {
                        chatColors.metaText
                    } else {
                        chatColors.sendFabContent
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (isLoading) 0.dp else 3.dp,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.id, isLoading) {
        val totalItems = messages.size + if (isLoading) 1 else 0
        if (totalItems == 0) return@LaunchedEffect
        val targetIndex = totalItems - 1
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > targetIndex }
        withFrameNanos { }
        listState.animateScrollToItem(targetIndex)
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    colors: ChatThemeColors,
    messageTextStyle: TextStyle,
    maxBubbleWidth: Dp,
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) colors.userBubble else colors.assistantBubble
    val contentColor = if (message.isUser) colors.userText else colors.assistantText
    val shape = if (message.isUser) {
        RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
    } else {
        RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .wrapContentWidth(if (message.isUser) Alignment.End else Alignment.Start),
        ) {
            Surface(
                color = bgColor,
                shape = shape,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp,
            ) {
                if (message.isUser) {
                    Text(
                        text = message.content,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = messageTextStyle.copy(color = contentColor),
                    )
                } else {
                    AssistantMarkdownBody(
                        markdown = message.content,
                        textColor = contentColor,
                        messageTextStyle = messageTextStyle,
                        colors = colors,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = colors.metaText,
                )
                if (!message.isUser && message.totalTokens != null) {
                    Text(
                        text = "· ${message.totalTokens} tok",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.metaText.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMarkdownBody(
    markdown: String,
    textColor: Color,
    messageTextStyle: TextStyle,
    colors: ChatThemeColors,
) {
    val mdColors = markdownColor(
        text = textColor,
        codeText = textColor,
        inlineCodeText = textColor,
        linkText = colors.linkColor,
        codeBackground = colors.codeBg,
        inlineCodeBackground = colors.inlineCodeBg,
    )
    CompositionLocalProvider(LocalTextStyle provides messageTextStyle.copy(color = textColor)) {
        Markdown(
            content = markdown,
            colors = mdColors,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
