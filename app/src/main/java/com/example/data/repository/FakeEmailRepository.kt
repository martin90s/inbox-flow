package com.example.data.repository

import com.example.data.model.EmailItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay

class FakeEmailRepository : EmailRepository {
    private val mutex = Mutex()

    // Helper to generate a large dataset
    private val allMails = (1..100).map { i ->
        EmailItem(
            id = i.toLong(),
            sender = when (i % 5) {
                0 -> "Sarah Jenkins"
                1 -> "HR Team (TechCorp)"
                2 -> "GitHub Notifications"
                3 -> "Kotlin Weekly"
                else -> "Figma Team"
            },
            senderEmail = when (i % 5) {
                0 -> "sarah.j@google.com"
                1 -> "careers@techcorp.com"
                2 -> "noreply@github.com"
                3 -> "newsletter@kotlinweekly.net"
                else -> "support@figma.com"
            },
            subject = "Subject #$i: " + when (i % 3) {
                0 -> "Important update on project"
                1 -> "New design system ready"
                else -> "Weekly newsletter for developers"
            },
            body = "This is the body for email #$i. It contains some detailed information about the topic mentioned in the subject line. We hope you find this useful and informative.",
            timestamp = if (i < 10) "${10 - i}:35 AM" else "May ${30 - (i / 5)}",
            isRead = i > 10,
            isStarred = i % 7 == 0
        )
    }

    private val pageSize = 20
    private var currentPage = 1

    // Using MutableStateFlow to act as a reactive, in-memory database
    private val _emailsFlow = MutableStateFlow(allMails.take(pageSize))

    override fun getEmails(): Flow<List<EmailItem>> {
        return _emailsFlow.asStateFlow()
    }

    override suspend fun markAsRead(emailId: Long, isRead: Boolean) {
        _emailsFlow.update { currentEmails ->
            currentEmails.map { email ->
                if (email.id == emailId) email.copy(isRead = isRead) else email
            }
        }
    }

    override suspend fun toggleStar(emailId: Long) {
        _emailsFlow.update { currentEmails ->
            currentEmails.map { email ->
                if (email.id == emailId) email.copy(isStarred = !email.isStarred) else email
            }
        }
    }

    override suspend fun loadMore() {
        mutex.withLock {
            val nextItems = allMails.take((currentPage + 1) * pageSize)
            if (nextItems.size > _emailsFlow.value.size) {
                // Simulate network delay to make pagination visible
                delay(500)
                currentPage++
                _emailsFlow.value = nextItems
            }
        }
    }
}
