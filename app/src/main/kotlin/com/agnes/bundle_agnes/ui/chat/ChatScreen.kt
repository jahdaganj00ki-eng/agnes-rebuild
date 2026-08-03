package com.agnes.bundle_agnes.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnes.bundle_agnes.core.ui.theme.Theme.AgnesTheme
import com.agnes.bundle_agnes.feature.chat.repository.ChatRepository
import com.agnes.bundle_agnes.feature.chat.repository.ChatStreamRequestBody
import com.sobrr.agnes.feature_chat.model.ChatMessage
import com.sobrr.agnes.feature_chat.model.MessageRole
import com.sobrr.agnes.feature_chat.model.StreamBlock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatRepository: ChatRepository,
    onLogout: () -> Unit
) {
    val messages by chatRepository.messages.collectAsStateWithLifecycle()
    val isStreaming by chatRepository.isStreaming.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val inputText by remember { mutableStateOf("") }
    val isSending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Chat") },
            actions = {
                IconButton(onClick = onLogout) {
                    androidx.compose.material.icons.filled.Logout
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        )

        // Messages list
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = true,
                state = scrollState
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message = message)
                }
                // Streaming indicator
                if (isStreaming) {
                    item {
                        StreamingIndicator()
                    }
                }
            }
        }

        // Input area
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type a message...") },
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp, top = 8.dp)
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank() && !isSending) {
                            sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isSending && inputText.isNotBlank(),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isSending) {
                        androidx.compose.material.ProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        androidx.compose.material.icons.filled.Send
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = alignment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            message.blocks.forEach { block ->
                RenderStreamBlock(block = block, isUser = isUser)
            }
        }
    }
}

@Composable
fun RenderStreamBlock(block: StreamBlock, isUser: Boolean) {
    when (block) {
        is StreamBlock.TextBlock -> {
            Text(
                text = block.content,
                color = if (isUser) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(12.dp, 8.dp)
                    .background(
                        color = if (isUser) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .wrapContentWidth()
            )
        }
        is StreamBlock.ThinkingBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤔 Thinking...", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(block.content, fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    if (block.isExpanded) {
                        Text("(expanded)", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        is StreamBlock.SkillLoadBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔧 Skill Loaded: ${block.skillName}", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer)
                    block.skillDescription?.let {
                        Text(it, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
        is StreamBlock.ToolCallBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text("🛠 ${block.toolName} (${block.toolType})", fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(block.status.name, fontSize = 11.sp, color = when (block.status) {
                            StreamBlock.ToolCallStatus.COMPLETED -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                            StreamBlock.ToolCallStatus.FAILED -> androidx.compose.material3.MaterialTheme.colorScheme.error
                            else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        })
                    }
                    if (block.arguments.isNotEmpty()) {
                        Text("Args: ${block.arguments}", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    block.result?.let { result ->
                        Text("Result: $result", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        is StreamBlock.ImageBlock -> {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = androidx.compose.ui.res.R.drawable.ic_launcher_foreground),
                        contentDescription = block.prompt ?: "Generated image",
                        contentScale = androidx.compose.foundation.layout.ContentScale.Crop
                    )
                }
                block.prompt?.let {
                    Text("Prompt: $it", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        is StreamBlock.ArtifactBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    Text("📄 ${block.title}", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Type: ${block.artifactType}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    block.previewUrl?.let {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { /* open preview */ }) { Text("Preview") }
                            Button(onClick = { /* download */ }) { Text("Download") }
                        }
                    }
                }
            }
        }
        is StreamBlock.FollowupsBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Follow-up questions:", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    block.questions.forEachIndexed { index, question ->
                        Text("${index + 1}. $question", fontSize = 13.sp, modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(12.dp)
                        )
                    }
                }
            }
        }
        is StreamBlock.ErrorBlock -> {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                )
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ Error (${block.code}): ${block.message}", fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                    if (block.recoverable) {
                        Text("Tap to retry", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }
        is StreamBlock.DoneBlock -> {
            // End of stream marker - could show a small indicator
        }
    }
}

@Composable
fun StreamingIndicator() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(24.dp)
        ) {
            androidx.compose.material.ProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
        Text("Generating response...", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Extension for sending messages
fun ChatScreen.sendMessage(inputText: String) {
    // This would be implemented with proper scope
    // For now, placeholder
}