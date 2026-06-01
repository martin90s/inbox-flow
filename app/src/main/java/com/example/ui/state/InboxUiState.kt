package com.example.ui.state

import com.example.data.model.EmailItem

/**
 * Immutable UI State representing the single source of truth for the Inbox screen.
 * This pattern is a standard representation of Unidirectional Data Flow (UDF) in MVI/MVVM.
 */
data class InboxUiState(
    val emails: List<EmailItem> = emptyList(),
    val searchQuery: String = "",
    val expandedEmailId: Long? = null,
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean get() = !isLoading && emails.isEmpty()
}
