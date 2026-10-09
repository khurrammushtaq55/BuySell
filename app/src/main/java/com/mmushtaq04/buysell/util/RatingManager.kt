package com.mmushtaq04.buysell.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object RatingManager {
    private const val TAG = "RatingManager"
    private const val PREFS_NAME = "app_rating_prefs"
    private const val KEY_NEVER_SHOW_AGAIN = "never_show_again"
    private const val KEY_INTERACTION_COUNT = "interaction_count"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private const val TRIGGER_INTERACTION_INTERVAL = 2 // Prompt after every 2 key interactions

    fun isNeverShowAgain(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NEVER_SHOW_AGAIN, false)
    }

    fun setNeverShowAgain(context: Context, neverShow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NEVER_SHOW_AGAIN, neverShow).apply()
        Log.d(TAG, "Updated never_show_again preference to: $neverShow")
    }

    fun recordInteraction(context: Context) {
        if (isNeverShowAgain(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_INTERACTION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_INTERACTION_COUNT, count).apply()
        Log.d(TAG, "Recorded interaction count: $count")
    }

    fun shouldShowRatingPrompt(context: Context): Boolean {
        if (isNeverShowAgain(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_INTERACTION_COUNT, 0)
        return count >= TRIGGER_INTERACTION_INTERVAL
    }

    fun resetInteractionCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_INTERACTION_COUNT, 0)
            .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
            .apply()
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
