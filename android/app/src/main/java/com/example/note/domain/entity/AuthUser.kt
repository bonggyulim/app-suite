package com.example.note.domain.entity

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)