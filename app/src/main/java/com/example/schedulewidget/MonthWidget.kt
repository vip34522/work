package com.example.schedulewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.background

class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(0xFF0F172A.toInt()))
                    .padding(16.dp)
            ) {
                Text(
                    text = "极简测试：月视图渲染成功！",
                    style = TextStyle(
                        color = ColorProvider(0xFF34D399.toInt()),
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}
