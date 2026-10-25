package com.example.schedulewidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.runBlocking

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
    companion object {
        fun refresh(context: Context) {
            runBlocking { try { MonthWidget().updateAll(context) } catch (_: Exception) {} }
        }
    }
}

class MonthPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDataStore.addMonthOffset(context, -1)
        MonthWidget().updateAll(context)
    }
}

class MonthNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDataStore.addMonthOffset(context, 1)
        MonthWidget().updateAll(context)
    }
}
