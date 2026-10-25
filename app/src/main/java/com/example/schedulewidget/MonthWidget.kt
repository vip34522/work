package com.example.schedulewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.*
import androidx.glance.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import org.json.JSONObject
import java.util.*

data class MonthCell(val day: Int, val shift: String, val lunar: String, val isToday: Boolean, val isHoliday: Boolean)
data class MonthModel(val year: Int, val month: Int, val cells: List<MonthCell>)

class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore.load(context)
        val offset = WidgetDataStore.getMonthOffset(context)
        val model = buildModel(data, offset)
        
        provideContent { 
            MonthContent(context, model) 
        }
    }

    private fun buildModel(data: JSONObject?, offset: Int): MonthModel {
        val me = data?.let { ShiftCalc.getMe(it) }
        val base = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        base.add(Calendar.MONTH, offset)
        val y = base.get(Calendar.YEAR)
        val m = base.get(Calendar.MONTH) + 1

        val first = Calendar.getInstance().apply { set(Calendar.YEAR, y); set(Calendar.MONTH, m - 1); set(Calendar.DAY_OF_MONTH, 1) }
        val dim = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dow = first.get(Calendar.DAY_OF_WEEK)
        val startPad = if (dow == Calendar.SUNDAY) 6 else dow - 2

        val cells = mutableListOf<MonthCell>()
        repeat(startPad) { cells.add(MonthCell(0, "", "", false, false)) }

        for (d in 1..dim) {
            val date = Calendar.getInstance().apply { set(Calendar.YEAR, y); set(Calendar.MONTH, m - 1); set(Calendar.DAY_OF_MONTH, d) }.time
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, date) else null
            val holiday = LunarCalc.getHoliday(date)
            cells.add(MonthCell(d, ShiftCalc.shiftShort(shift), holiday ?: LunarCalc.lunarShort(date), ShiftCalc.isToday(date), holiday != null))
        }
        while (cells.size % 7 != 0) { cells.add(MonthCell(0, "", "", false, false)) }
        return MonthModel(y, m, cells)
    }
}

@Composable
fun MonthContent(ctx: Context, model: MonthModel) {
    val theme = ThemeHelper.get(ctx)
    val bgColor = if (theme.isDark) 0xFF0F172A.toInt() else 0xFF1E293B.toInt()
    val cellBg = 0xFF1E293B.toInt()

    Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor)).padding(6.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.clickable(actionRunCallback<MonthPrevAction>()).padding(6.dp)) {
                Text("◀", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
            Box(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
                Text("${model.year}年${model.month}月", style = TextStyle(color = ColorProvider(0xFFF1F5F9.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Box(modifier = GlanceModifier.clickable(actionRunCallback<MonthNextAction>()).padding(6.dp)) {
                Text("▶", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
        }
        Spacer(GlanceModifier.height(4.dp))

        Row(GlanceModifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, w ->
                if (i > 0) Spacer(GlanceModifier.width(1.dp))
                Box(GlanceModifier.defaultWeight()) { Text(w, modifier = GlanceModifier.fillMaxWidth(), style = TextStyle(color = ColorProvider(0xFF94A3B8.toInt()), fontSize = 9.sp, textAlign = TextAlign.Center)) }
            }
        }
        Spacer(GlanceModifier.height(2.dp))

        val rows = model.cells.chunked(7)
        rows.forEachIndexed { ri, row ->
            if (ri > 0) Spacer(GlanceModifier.height(2.dp))
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                row.forEachIndexed { i, cell ->
                    if (i > 0) Spacer(GlanceModifier.width(1.dp))
                    Box(GlanceModifier.defaultWeight().fillMaxHeight()) {
                        if (cell.day != 0) {
                            val bg = if (cell.isToday) ColorProvider(theme.primary) else ColorProvider(cellBg)
                            val dayColor = if (cell.isToday) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFFF1F5F9.toInt())
                            val shiftColor = if (cell.isToday) ColorProvider(0xFFFFFFFF.toInt()) else ColorProvider(0xFF60A5FA.toInt())
                            val lunarColor = if (cell.isToday) ColorProvider(0xFFFFFFFF.toInt()) else if (cell.isHoliday) ColorProvider(0xFFEF4444.toInt()) else ColorProvider(0xFF94A3B8.toInt())
                            
                            Column(modifier = GlanceModifier.fillMaxSize().background(bg).padding(1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${cell.day}", style = TextStyle(color = dayColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                    Spacer(GlanceModifier.defaultWeight())
                                    Text(cell.lunar.take(2), style = TextStyle(color = lunarColor, fontSize = 7.sp))
                                }
                                Spacer(GlanceModifier.defaultWeight())
                                Text(cell.shift, style = TextStyle(color = shiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                Spacer(GlanceModifier.defaultWeight())
                            }
                        }
                    }
                }
            }
        }
    }
}
