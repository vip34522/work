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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.background

class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 没有任何复杂的计算，没有任何从 JSON 取数据，纯写死测试
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(0xFF1E293B.toInt())) // 深色背景
                    .padding(16.dp)
            ) {
                Text(
                    text = "极简测试：如果你能看到我，说明渲染没问题！",
                    style = TextStyle(
                        color = ColorProvider(0xFFFFFFFF.toInt()), // 纯白字
                        fontSize = 14.sp
                    )
                )
                Row(modifier = GlanceModifier.padding(top = 10.dp)) {
                    Text(
                        text = "周一：白班",
                        style = TextStyle(
                            color = ColorProvider(0xFF60A5FA.toInt()),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
