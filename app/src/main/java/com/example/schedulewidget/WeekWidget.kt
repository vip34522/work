package com.example.schedulewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import org.json.JSONObject
import java.util.*

data class WeekDayCell(val weekName: String, val day: Int, val shift: String, val lunar: String, val isToday: Boolean, val isHoliday: Boolean)
data class WeekModel(val title: String, val cells: List<WeekDayCell>)

class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore.load(context)
        val offset = WidgetDataStore.getWeekOffset(context)
        val model = buildModel(data, offset)
        
        provideContent { 
            WeekContent(context, model) 
        }
    }

    private fun buildModel(data: JSONObject?, offset: Int): WeekModel {
        val me = data?.let { ShiftCalc.getMe(it) }
        val todayMonday = ShiftCalc.mondayOf(Date())
        val monday = ShiftCalc.addDays(todayMonday, offset * 7)
        val names = listOf("一", "二", "三", "四", "五", "六", "日")

        val cells = (0 until 7).map { i ->
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val holiday = LunarCalc.getHoliday(d)
            WeekDayCell(names[i], c.get(Calendar.DAY_OF_MONTH), ShiftCalc.shiftShort(shift), holiday ?: LunarCalc.lunarShort(d), ShiftCalc.isToday(d), holiday != null)
        }

        val sunday = ShiftCalc.addDays(monday, 6)
        val c1 = Calendar.getInstance().apply { time = monday }
        val c2 = Calendar.getInstance().apply { time = sunday }
        val title = buildString {
            if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) append("${c1.get(Calendar.YEAR)}年") else append("${c1.get(Calendar.YEAR)}-${c2.get(Calendar.YEAR)}年")
            if (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)) append("${c1.get(Calendar.MONTH) + 1}月") else append("${c1.get(Calendar.MONTH) + 1}-${c2.get(Calendar.MONTH) + 1}月")
            append(" · 第${c1.get(Calendar.WEEK_OF_YEAR)}周")
        }
        return WeekModel(title, cells)
    }
}

@Composable
fun WeekContent(ctx: Context, model: WeekModel) {
    val theme = ThemeHelper.get(ctx)
    val bgColor = if (theme.isDark) 0xFF0F172A.toInt() else 0xFF1E293B.toInt()
    val cellBg = if (theme.isDark) 0xFF1E293B.toInt() else 0xFF334155.toInt()

    Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor)).padding(8.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.clickable(actionRunCallback<WeekPrevAction>()).padding(6.dp)) {
                Text("◀", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
            Box(modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
                Text(model.title, style = TextStyle(color = ColorProvider(0xFFF1F5F9.toInt()), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
            Box(modifier = GlanceModifier.clickable(actionRunCallback<WeekNextAction>()).padding(6.dp)) {
                Text("▶", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            model.cells.forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(2.dp))
                val bg = if (c.isToday) ColorProvider(theme.primary) else ColorProvider(cellBg)
                val dayColor = if (c.isToday) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFFF1F5F9.toInt())
                val shiftColor = if (c.isToday) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFF60A5FA.toInt())
                val lunarColor = if (c.isToday) ColorProvider(0xFFFFFFFF.toInt()) else if (c.isHoliday) ColorProvider(0xFFEF4444.toInt()) else ColorProvider(0xFF94A3B8.toInt())
                
                Column(modifier = GlanceModifier.fillMaxWidth().background(bg).padding(vertical = 4.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.weekName, style = TextStyle(color = if (c.isToday) ColorProvider(0xFFE0E7FF.toInt()) else ColorProvider(0xFF94A3B8.toInt()), fontSize = 8.sp))
                    Text("${c.day}", style = TextStyle(color = dayColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Text(c.shift, style = TextStyle(color = shiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Text(c.lunar.take(3), style = TextStyle(color = lunarColor, fontSize = 7.sp))
                }
            }
        }
    }
}
