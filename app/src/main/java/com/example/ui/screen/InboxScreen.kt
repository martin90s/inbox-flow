package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmailItem
import com.example.ui.viewmodel.InboxViewModel
import kotlin.math.absoluteValue

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onComposeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collecting single unified UI state using modern lifecycle collection stream
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 1. 创建共享的列表状态
    val listState = rememberLazyListState()
    
    // 优化 1：使用 remember 缓存所有 Lambda 回调，确保 EmailList 的参数引用保持不变
    // 这样 EmailList 就有更高的几率触发 Skip（跳过重组）
    val onEmailClicked = remember { { id: Long -> viewModel.onEmailClicked(id) } }
    val onToggleStarClicked = remember { { id: Long -> viewModel.onToggleStarClicked(id) } }
    val onToggleReadStatus = remember { { id: Long, isRead: Boolean -> viewModel.onMarkAsReadClicked(id, isRead) } }
    val onLoadMore = remember { { viewModel.onLoadMore() } }
    val onQueryChanged = remember { { q: String -> viewModel.onSearchQueryChanged(q) } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 优化 2：将 listState 直接传给 SearchBanner，内部去计算 isScrolled
            // 这样当滚动状态变化时，只有 SearchBanner 会重组，InboxScreen 和 EmailList 都不会动
            SearchBanner(
                query = uiState.searchQuery,
                onQueryChanged = onQueryChanged,
                onStartDrafting = onComposeClick,
                listState = listState
            )

            // 2. Email list viewport
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isEmpty) {
                EmptyStateView(query = uiState.searchQuery)
            } else {
                val emailListState = remember(uiState.emails) { EmailListState(uiState.emails) }
                EmailList(
                    state = emailListState,
                    listState = listState,
                    expandedEmailId = uiState.expandedEmailId,
                    onEmailClicked = onEmailClicked,
                    onToggleStarClicked = onToggleStarClicked,
                    onToggleReadStatus = onToggleReadStatus,
                    onLoadMore = onLoadMore
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBanner(
    query: String,
    onQueryChanged: (String) -> Unit,
    onStartDrafting: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState, // 接收 state 而不是 boolean
    modifier: Modifier = Modifier
) {
    // 将计算逻辑移入内部。现在只有这个组件会响应滚动状态的变化
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    // 根据滚动状态动态调整海拔和背景色
    val elevation by animateDpAsState(
        targetValue = if (isScrolled) 8.dp else 2.dp,
        label = "elevation"
    )
    val containerColor = if (isScrolled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .testTag("search_bar_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "Search mail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_textfield"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onStartDrafting,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Compose email",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Immutable
data class EmailListState(
    val emails: List<EmailItem>
)

@Composable
fun EmailList(
    state: EmailListState,
    listState: androidx.compose.foundation.lazy.LazyListState, // 接收传入的 state
    expandedEmailId: Long?,
    onEmailClicked: (Long) -> Unit,
    onToggleStarClicked: (Long) -> Unit,
    onToggleReadStatus: (Long, Boolean) -> Unit, // 注意参数名对齐
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 移除内部创建的 listState

    // INTERVIEW ANTI-PATTERN: Reading scroll state directly in the Composable body.
    // This causes the entire EmailList (and all its children) to recompose on EVERY pixel scrolled.
//    val scrollOffset = listState.firstVisibleItemScrollOffset

    // Detect when we are near the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            // Load more when 3 items from the bottom
            lastVisibleItemIndex > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("email_lazy_column")
    ) {
        items(
            items = state.emails,
            key = { it.id }
        ) { email ->
            val isExpanded = email.id == expandedEmailId
            EmailRowItem(
                email = email,
                isExpanded = isExpanded,
                onClick = { onEmailClicked(email.id) },
                onToggleStar = { onToggleStarClicked(email.id) },
                onToggleReadStatus = { onToggleReadStatus(email.id, !email.isRead) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Loading indicator at the bottom
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun EmailRowItem(
    email: EmailItem,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleReadStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smoothen list cell expansion transitions using animateColorAsState
    val backgroundColor by animateColorAsState(
        targetValue = if (isExpanded) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        label = "rowBackground"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("email_item_${email.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Profile Initials Avatar
            AvatarView(
                senderName = email.sender,
                isUnread = !email.isRead
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.sender,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal,
                            color = if (!email.isRead) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = email.timestamp,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (!email.isRead) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!email.isRead) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (!email.isRead) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Conditionally display limited snippet or expansive full body
                if (!isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Inline quick-star actions in collapsed state
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!email.isRead) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                            }

                            IconButton(
                                onClick = onToggleStar,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("star_button_${email.id}")
                            ) {
                                Icon(
                                    imageVector = if (email.isStarred) {
                                        Icons.Default.Star
                                    } else {
                                        Icons.Default.StarBorder
                                    },
                                    contentDescription = "Star email",
                                    tint = if (email.isStarred) {
                                        Color(0xFFE0A900)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded view detail pane with responsive actions
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 48.dp) // Offset slightly from avatar
            ) {
                // Email headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To: me <${email.senderEmail}>",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row {
                        IconButton(
                            onClick = onToggleReadStatus,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (email.isRead) Icons.Default.MarkEmailUnread else Icons.Default.Drafts,
                                contentDescription = if (email.isRead) "Mark as Unread" else "Mark as Read",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleStar,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (email.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star email",
                                tint = if (email.isStarred) Color(0xFFE0A900) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Formatted body text
                Text(
                    text = email.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons representing interview-ready visual fidelity
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reply icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reply", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = "Forward icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Forward", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarView(
    senderName: String,
    isUnread: Boolean,
    modifier: Modifier = Modifier
) {
    // 优化：直接获取首字母，避免 100 次循环计算
    val initial = remember(senderName) {
        senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    
    // 优化：使用简单的哈希算法从预定义列表中选色，避免每次重组都生成 500 个颜色对象
    val avatarColor = remember(senderName) {
        val colors = listOf(
            Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
            Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
            Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726)
        )
        val index = (senderName.hashCode().absoluteValue) % colors.size
        colors[index]
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
fun EmptyStateView(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Empty list icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No mail found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (query.isNotBlank()) {
                "We couldn't search up any results matching \"$query\". Check your spelling or filter keywords."
            } else {
                "You are all caught up! Your inbox is pristine."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
