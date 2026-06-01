package com.example.data.repository

import com.example.data.model.EmailItem
import kotlinx.coroutines.flow.Flow

interface EmailRepository {
    /**
     * Exposes the continuous stream of emails. Any updates to emails
     * will be emitted reactively to collectors of this Flow.
     */
    fun getEmails(): Flow<List<EmailItem>>

    /**
     * Marks an email as read or unread. This demonstrates how a repository
     * supports write operations that trigger reactive updates to the Flow.
     */
    suspend fun markAsRead(emailId: Long, isRead: Boolean)

    /**
     * Toggles the starred status of an email.
     */
    suspend fun toggleStar(emailId: Long)

    /**
     * Loads the next page of emails.
     */
    suspend fun loadMore()
}
