package com.example.schedulewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.*
import androidx.glance.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import org.json.JSONObject
import java.util.*

data class MonthCell(val day: Int, val shift: String, val lunar: String, val isToday: Boolean, val isHoliday: Boolean)
data class MonthModel(val year: Int, val month: Int, val cells: List<MonthCell>)

class MonthWidget : GlanceAppWidget() {
    override suspend fun provideContent() {
        val ctx = applicationContext
        val data = WidgetDataStore.load(ctx)
        val offset = WidgetDataStore.getMonthOffset(ctx)
        val model = buildModel(data, offset)
        provideContent { MonthContent(ctx, model) }
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
    Val theme = ThemeHelper.get(ctx)
关于这个问题0xFF0F172A.toInt() else 0xFF1E293B.toInt()
    Val cellBg = 0xFF1E293B.toInt()

    Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor)).padding(6.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.clickable(actionRunCallback<MonthPrevAction>()).padding(6.dp)) {
                Text("◀", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
            Box(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
                Text("${model.year}年${model.month}月", style = TextStyle(color = ColorProvider(0xFFF1F5F9.toInt())，fontSize=13.sp，fontWeight=FontWeight.Bold))
            }
            Box(modifier = GlanceModifier.clickable(actionRunCallback<MonthNextAction>()).padding(6.dp)) {
                Text("▶", style = TextStyle(color = ColorProvider(theme.primary), fontSize = 12.sp))
            }
        }
时间: 时间:4(dp))

        Row(GlanceModifier.fillMaxWidth()) {
listof("一", "二", "三", "四", "五", "六", "日").forEachIndexed{i，w->
                如果(i>0) 距离 (,) 视角 (,) 宽度1(dp))
方块(GlanceModifier.defaultWeight()){文字(w，modifier=GlanceModifier.fillMaxWidth()，style=TextStyle(color=ColorProvider(0xFF94A3B8.toInt())，fontSize=9.sp,textAlign = TextAlign.Center)) } 没有一个字符串.
            }
        }
时间: 时间:2(dp))

Val行=model.cells.chunked(7)
rows.forEachIndexed{ri，row->
如果(ri>0) 距离 (,) 视角 (,) 高度 (2(dp))
现在,我们要把它变成一个新的世界.
row.forEachIndexed{i，cell->
如果(i>0)距离(，)视角(，)宽度1.dp))
子的重量和值是不同的.
如果(cell.day！=0) {
Val BG=如果(cell.isToday)ColorProvider(主题.主要)elseColorProvider(cellBg)
Val daycolor=如果(cell.isToday)ColorProvider(0xFFFFFFFF.toInt())elseColorProvider(0xFFF1F5F9.toInt())
Val shiftColor=如果(cell.isToday)ColorProvider(0xFFFFFFFF.toInt())elseColorProvider(0xFF60A5FA.toInt())
Val lunarColor=如果(cell.isToday)ColorProvider(0xFFFFFFFF.toInt())else如果(cell.isHoliday)ColorProvider(0xFFEF4444.toInt())elseColorProvider(0xFF94A3B8.toInt())
                            
字体: 字体: 字体:1。DP)，horizontalAlignment=Alignment.CenterHorizontally){
字符串: 字符串:2。DP)，垂直对齐=对齐。居中垂直){
文本("${cell.day}"，style=TextStyle(颜色=日色，fontSize=10.sp，fontWeight=FontWeight.Bold))
垫片(GlanceModifier.defaultWeight())
文本(cell.lunar.take(2)，style=TextStyle(颜色=lunarColor，fontSize=7.sp))
                                }
垫片(GlanceModifier.defaultWeight())
text(单元格.shift，style=TextStyle(color=shiftColor，fontSize=11.sp，fontWeight=FontWeight.Bold))
垫片(GlanceModifier.defaultWeight())
                            }
                        }
                    }
                }
            }
        }
    }
}
