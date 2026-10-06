package com.mmushtaq04.buysell.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit

object RatingManager {
    private const val PREFS_NAME = "rating_prefs"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private const val KEY_PROMPT_COUNT = "prompt_count"
    private const val MAX_PROMPT_COUNT = 3
    private const val MIN_DAYS_BETWEEN_PROMPTS = 3L

    fun promptReviewIfEligible(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val promptCount = prefs.getInt(KEY_PROMPT_COUNT, 0)
        val lastPromptMs = prefs.getLong(KEY_LAST_PROMPT_MS, 0L)
        val now = System.currentTimeMillis()

        if (promptCount >= MAX_PROMPT_COUNT) return
        if (now - lastPromptMs < TimeUnit.DAYS.toMillis(MIN_DAYS_BETWEEN_PROMPTS)) return

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    prefs.edit()
                        .putInt(KEY_PROMPT_COUNT, promptCount + 1)
                        .putLong(KEY_LAST_PROMPT_MS, System.currentTimeMillis())
                        .apply()
                }
            }
        }
    }
}
