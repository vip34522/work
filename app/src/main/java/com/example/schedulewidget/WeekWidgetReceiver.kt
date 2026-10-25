package com.example.schedulewidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.runBlocking

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
    companion object {
        fun refresh(context: Context) {
            runBlocking { try { WeekWidget().updateAll(context) } catch (_: Exception) {} }
        }
    }
}

class WeekPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDataStore.addWeekOffset(context, -1)
        WeekWidget().updateAll(context)
    }
}

class WeekNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDataStore.addWeekOffset(context, 1)
        WeekWidget().updateAll(context)
    }
}
