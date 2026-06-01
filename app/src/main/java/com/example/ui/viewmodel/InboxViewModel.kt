package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.EmailRepository
import com.example.ui.state.InboxUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val repository: EmailRepository
) : ViewModel() {

    // Internal reactive state elements
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _expandedEmailId = MutableStateFlow<Long?>(null)
    val expandedEmailId = _expandedEmailId.asStateFlow()

    private val _isDrafting = MutableStateFlow(false)
    val isDrafting = _isDrafting.asStateFlow()

    /**
     * Cohesive flow combination merging the repository stream, search query, and expansion state.
     * Whenever any source emits, we reconstruct a new, immutable InboxUiState.
     * This is highly interview-friendly as it demonstrates clean, non-blocking stream processing.
     */
    val uiState: StateFlow<InboxUiState> = combine(
        repository.getEmails(),
        _searchQuery,
        _expandedEmailId,
        _isDrafting
    ) { emails, query, expandedId, isDrafting ->
        val filteredEmails = if (query.isBlank()) {
            emails
        } else {
            emails.filter { email ->
                email.sender.contains(query, ignoreCase = true) ||
                email.subject.contains(query, ignoreCase = true) ||
                email.body.contains(query, ignoreCase = true) ||
                email.senderEmail.contains(query, ignoreCase = true)
            }
        }
        InboxUiState(
            emails = filteredEmails,
            searchQuery = query,
            expandedEmailId = expandedId,
            isLoading = false,
            isDrafting = isDrafting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Keep active for 5s after UI unbinds to handle configuration changes
        initialValue = InboxUiState(isLoading = true)
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Toggles the expansion state. Only one email can be expanded at any time.
     * If the clicked email is already expanded, collapse it (set to null).
     */
    fun onEmailClicked(emailId: Long) {
        _expandedEmailId.updateValue { current ->
            if (current == emailId) null else emailId
        }

        // Auto-mark as read when clicked & expanded
        viewModelScope.launch {
            repository.markAsRead(emailId, isRead = true)
        }
    }

    fun onToggleStarClicked(emailId: Long) {
        viewModelScope.launch {
            repository.toggleStar(emailId)
        }
    }

    fun onMarkAsReadClicked(emailId: Long, isRead: Boolean) {
        viewModelScope.launch {
            repository.markAsRead(emailId, isRead)
        }
    }

    fun onLoadMore() {
        viewModelScope.launch {
            repository.loadMore()
        }
    }

    fun onStartDrafting() {
        _isDrafting.value = true
    }

    fun onCancelDrafting() {
        _isDrafting.value = false
    }

    fun onSendEmail(subject: String, body: String) {
        viewModelScope.launch {
            repository.sendEmail("Me", subject, body)
            _isDrafting.value = false
        }
    }

    // Thread-safe update helper
    private inline fun <T> MutableStateFlow<T>.updateValue(transform: (T) -> T) {
        var prev: T
        var next: T
        do {
            prev = value
            next = transform(prev)
        } while (!compareAndSet(prev, next))
    }
}
