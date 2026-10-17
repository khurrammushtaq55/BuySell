package com.mmushtaq04.buysell.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.core.net.toUri

object RatingManager {
    private const val TAG = "RatingManager"
    private const val PREFS_NAME = "app_rating_prefs"
    private const val KEY_NEVER_SHOW_AGAIN = "never_show_again"
    private const val KEY_INTERACTION_COUNT = "interaction_count"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private const val TRIGGER_INTERACTION_INTERVAL = 50 // Prompt after 50 key interactions
    private const val MIN_PROMPT_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000L // 3 days minimum between prompts

    fun isNeverShowAgain(context: Context): Boolean {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        return prefs?.getBoolean(KEY_NEVER_SHOW_AGAIN, false) ?: false
    }

    fun setNeverShowAgain(context: Context, neverShow: Boolean) {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val editor = prefs?.edit()
        if (editor != null) {
            editor.putBoolean(KEY_NEVER_SHOW_AGAIN, neverShow)
            editor.commit()
        }
        Log.d(TAG, "Updated never_show_again preference to: $neverShow")
    }

    fun recordInteraction(context: Context) {
        if (isNeverShowAgain(context)) return
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val count = (prefs?.getInt(KEY_INTERACTION_COUNT, 0) ?: 0) + 1
        val editor = prefs?.edit()
        if (editor != null) {
            editor.putInt(KEY_INTERACTION_COUNT, count)
            editor.commit()
        }
        Log.d(TAG, "Recorded interaction count: $count")
    }

    fun shouldShowRatingPrompt(context: Context): Boolean {
        if (isNeverShowAgain(context)) return false
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val count = prefs?.getInt(KEY_INTERACTION_COUNT, 0) ?: 0
        val lastPromptMs = prefs?.getLong(KEY_LAST_PROMPT_MS, 0L) ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastPromptMs < MIN_PROMPT_COOLDOWN_MS) {
            return false
        }

        return count >= TRIGGER_INTERACTION_INTERVAL
    }

    fun resetInteractionCount(context: Context) {
        val prefs = runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
        val editor = prefs?.edit()
        if (editor != null) {
            editor.putInt(KEY_INTERACTION_COUNT, 0)
            editor.putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
            editor.commit()
        }
    }

    fun launchGoogleInAppReview(activity: Activity, onComplete: () -> Unit = {}) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    Log.i(TAG, "✓ Google In-App Review flow completed")
                    onComplete()
                }
            } else {
                Log.w(TAG, "Google In-App Review flow request failed. Falling back to Play Store link.", task.exception)
                openPlayStorePage(activity)
                onComplete()
            }
        }
    }

    fun openPlayStorePage(context: Context) {
        val appPackageName = context.packageName
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
