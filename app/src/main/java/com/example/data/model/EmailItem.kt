package com.example.data.model

data class EmailItem(
    val id: Long,
    val sender: String,
    val senderEmail: String,
    val subject: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean,
    val isStarred: Boolean = false
)
