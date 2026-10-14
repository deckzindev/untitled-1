package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("gitbuilder_prefs", Context.MODE_PRIVATE)

  private val _githubToken = MutableStateFlow(prefs.getString(KEY_GITHUB_TOKEN, "") ?: "")
  val githubToken: StateFlow<String> = _githubToken.asStateFlow()

  private val _githubUsername = MutableStateFlow(prefs.getString(KEY_GITHUB_USERNAME, "") ?: "")
  val githubUsername: StateFlow<String> = _githubUsername.asStateFlow()

  private val _defaultRepo = MutableStateFlow(prefs.getString(KEY_DEFAULT_REPO, "") ?: "")
  val defaultRepo: StateFlow<String> = _defaultRepo.asStateFlow()

  private val _defaultVariant = MutableStateFlow(prefs.getString(KEY_DEFAULT_VARIANT, "release") ?: "release")
  val defaultVariant: StateFlow<String> = _defaultVariant.asStateFlow()

  fun saveGitHubToken(token: String) {
    val cleanToken = token.trim()
    prefs.edit().putString(KEY_GITHUB_TOKEN, cleanToken).apply()
    _githubToken.value = cleanToken
  }

  fun saveGitHubUsername(username: String) {
    prefs.edit().putString(KEY_GITHUB_USERNAME, username).apply()
    _githubUsername.value = username
  }

  fun saveDefaultRepo(repo: String) {
    prefs.edit().putString(KEY_DEFAULT_REPO, repo).apply()
    _defaultRepo.value = repo
  }

  fun saveDefaultVariant(variant: String) {
    prefs.edit().putString(KEY_DEFAULT_VARIANT, variant).apply()
    _defaultVariant.value = variant
  }

  fun clearAuth() {
    prefs.edit().remove(KEY_GITHUB_TOKEN).remove(KEY_GITHUB_USERNAME).apply()
    _githubToken.value = ""
    _githubUsername.value = ""
  }

  companion object {
    private const val KEY_GITHUB_TOKEN = "key_github_token"
    private const val KEY_GITHUB_USERNAME = "key_github_username"
    private const val KEY_DEFAULT_REPO = "key_default_repo"
    private const val KEY_DEFAULT_VARIANT = "key_default_variant"
  }
}
