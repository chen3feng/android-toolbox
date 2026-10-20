package com.phongchen.toolbox.widget

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.phongchen.toolbox.core.Tool

/** One-tap home-screen widget pinning, with a manual-add fallback path. */
object WidgetPinner {

    fun isSupported(context: Context): Boolean =
        context.getSystemService(AppWidgetManager::class.java).isRequestPinAppWidgetSupported

    /** Returns true when the pin request was handed to the launcher (user may still cancel). */
    fun requestPin(activity: Activity, tool: Tool): Boolean {
        val manager = activity.getSystemService(AppWidgetManager::class.java)
        if (!manager.isRequestPinAppWidgetSupported) return false

        val provider = ComponentName(activity, tool.widgetReceiver)
        val callback = PendingIntent.getBroadcast(
            activity,
            tool.ordinal,
            Intent(ACTION_WIDGET_PINNED).setPackage(activity.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return manager.requestPinAppWidget(provider, null, callback)
    }

    const val ACTION_WIDGET_PINNED = "com.phongchen.toolbox.action.WIDGET_PINNED"
}
