package com.mazen.prayersglass

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object PendingIntentCompat {
    fun activity(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
