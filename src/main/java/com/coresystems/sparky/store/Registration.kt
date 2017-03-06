package com.coresystems.sparky.store

/**
 * Represents a registered user, including his account, name and token.
 */
data class Registration(val account: String = "", val userName: String = "", val fullName: String = "", val token: String = "", val callerId: String = "")