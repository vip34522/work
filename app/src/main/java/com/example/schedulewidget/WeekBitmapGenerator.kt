package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val today = Date()
        val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        // 动态比例缩放参数 (以宽度 360 为基准，保证在任何分辨率下比例一致)
        val scale = width / 360f
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        val rectF = RectF()

        val cellWidth = width / 7f
        val topSectionHeight = 160f * scale // 日历区域高度

        // ================= 1. 画 7 天排班 =================
        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = when(shiftStr) {
                "休" -> "#EF4444"
                "白" -> "#3B82F6"
                "夜" -> "#1E293B"
                "二" -> "#0EA5E9"
                else -> "#10B981"
            }

            val centerX = cellWidth * i + cellWidth / 2
            val startY = 16f * scale

            // 高亮外框（只包住当前内容，不画到底部）
            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f * scale
                paint.color = Color.parseColor("#2563EB")
                rectF.set(cellWidth * i + 4f * scale, 4f * scale, cellWidth * (i + 1) - 4f * scale, startY + 145f * scale)
                canvas.drawRoundRect(rectF, 12f * scale, 12f * scale, paint)
            }

            // 画星期
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.textSize = 26f * scale
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, startY + 28f * scale, textPaint)

            // 画日期
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 38f * scale
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, startY + 78f * scale, textPaint)

            // 画班次小标签
            val rectWidth = cellWidth * 0.75f
            val rectHeight = 42f * scale
            val rectLeft = centerX - rectWidth / 2
            val rectTop = startY + 98f * scale
            val rectRight = centerX + rectWidth / 2
            val rectBottom = rectTop + rectHeight

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(rectF, 10f * scale, 10f * scale, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 24f * scale
            textPaint.isFakeBoldText = false
            val textY = rectTop + (rectHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(shiftStr, centerX, textY, textPaint)
        }

        // ================= 2. 画中间蓝色分割线 =================
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRect(16f * scale, topSectionHeight, width - 16f * scale, topSectionHeight + 3f * scale, paint)

        // ================= 3. 画底部详情 (上下居中) =================
        val bottomStartY = topSectionHeight + 3f * scale
        val bottomHeight = height - bottomStartY
        val bottomCenterY = bottomStartY + bottomHeight / 2 // 垂直居中点

        val leftColWidth = width * 0.28f
        val leftCenterX = leftColWidth / 2

        // 3.1 左侧日期、农历、节日
        textPaint.textAlign = Paint.Align.CENTER
        
        // 大日期
        textPaint.color = Color.parseColor("#1E293B")
        textPaint.isFakeBoldText = true
        textPaint.textSize = 36f * scale
        val dateStr = "${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日"
        canvas.drawText(dateStr, leftCenterX, bottomCenterY - 15f * scale, textPaint)
        
        // 农历
        textPaint.textSize = 22f * scale
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = false
        val lunar = LunarCalc.lunarFull(today) // 使用完整农历
        canvas.drawText(lunar, leftCenterX, bottomCenterY + 20f * scale, textPaint)

        // 节日 (如果有)
        val holiday = LunarCalc.getHoliday(today)
        var textOffset = 50f * scale
        if (!holiday.isNullOrEmpty()) {
            textPaint.color = Color.parseColor("#EF4444") // 节日用红色
            textPaint.isFakeBoldText = true
            canvas.drawText(holiday, leftCenterX, bottomCenterY + 20f * scale + 28f * scale, textPaint)
            textOffset += 28f * scale
        }

        // 星期
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = false
        val weekName = weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
        canvas.drawText("星期$weekName", leftCenterX, bottomCenterY + 20f * scale + textOffset, textPaint)

        // 3.2 中间灰色竖线
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1.5f * scale
        canvas.drawLine(leftColWidth, bottomStartY + 16f * scale, leftColWidth, height - 16f * scale, paint)

        // 3.3 右侧人员列表 (垂直居中)
        val listStartX = leftColWidth + 16f * scale
        val personsToShow = getTodayPersons(data)
        
        var listY = bottomStartY + 20f * scale
        // 如果人数超过4个，需要调整起始Y，保证居中
        val listItemHeight = 36f * scale
        val showCount = Math.min(personsToShow.size, 4)
        if (personsToShow.size <= 4) {
            listY = bottomCenterY - (showCount * listItemHeight) / 2 + 10f * scale
        }

        textPaint.textAlign = Paint.Align.LEFT
        var count = 0
        for (p in personsToShow) {
            if (count >= 4) break
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            
            // 画名字
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.textSize = 26f * scale
            textPaint.isFakeBoldText = true
            canvas.drawText(pName, listStartX, listY, textPaint)
            
            // 画班种和班次
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = 24f * scale
            canvas.drawText("$typeName  $shiftStr", listStartX + 100f * scale, listY, textPaint)
            
            listY += listItemHeight
            count++
        }
        
        // 更多
        if (personsToShow.size > 4) {
            textPaint.color = Color.parseColor("#94A3B8")
            canvas.drawText("【更多】", listStartX + 100f * scale, listY, textPaint)
        }

        return bitmap
    }

    private fun getTypeName(data: JSONObject, typeId: String): String {
        val types = data.optJSONArray("shiftTypes") ?: return "排班"
        for (i in 0 until types.length()) {
            if (types.getJSONObject(i).optString("id") == typeId) {
                return types.getJSONObject(i).optString("name")
            }
        }
        return "排班"
    }
    
    private fun getTodayPersons(data: JSONObject?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val arr = data?.optJSONArray("personnel") ?: return list
        for (i in 0 until arr.length()) {
            list.add(arr.getJSONObject(i))
        }
        return list
    }
}
