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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        val w = width.toFloat()
        val h = height.toFloat()

        // 获取文字在指定 Y 坐标垂直居中时的 BaseLine
        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // ================= 1. 画上方日历区 (占 55% 高度) =================
        val topH = h * 0.55f
        val cellW = w / 7f
        
        // 将上方区域按比例分为三层中心线
        val line1Y = topH * 0.25f // 星期
        val line2Y = topH * 0.55f // 日期
        val line3Y = topH * 0.82f // 班次块

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)

            val centerX = cellW * i + cellW / 2f

            // 今天高亮外框
            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#2563EB")
                rectF.set(cellW * i + 4f, line1Y - 15f, cellW * (i + 1) - 4f, line3Y + 18f)
                canvas.drawRoundRect(rectF, 10f, 10f, paint)
            }

            // 第一层：周几
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = Math.min(cellW * 0.35f, 36f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, getCenteredY(line1Y, textPaint), textPaint)

            // 第二层：日期数字
            textPaint.textSize = Math.min(cellW * 0.55f, 60f)
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, getCenteredY(line2Y, textPaint), textPaint)

            // 第三层：班次色块
            val rectW = cellW * 0.8f
            val rectH = Math.min(topH * 0.22f, 40f)
            rectF.set(centerX - rectW / 2f, line3Y - rectH / 2f, centerX + rectW / 2f, line3Y + rectH / 2f)
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)

            textPaint.textSize = Math.min(rectH * 0.65f, 28f)
            textPaint.color = Color.WHITE
            textPaint.isFakeBoldText = false
            canvas.drawText(shiftStr, centerX, getCenteredY(line3Y, textPaint), textPaint)
        }

        // 中间蓝色分割线
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRect(0f, topH, w, topH + 3f, paint)

        // ================= 2. 画下方详情区 (占 45% 高度) =================
        val botStartY = topH + 3f
        val botH = h - botStartY
        val leftW = w * 0.32f // 左侧区域宽度

        // ---------------- 2.1 左下角：日期、农历、星期、节日 ----------------
        val dateItems = mutableListOf<String>()
        val dateColors = mutableListOf<String>()
        
        // 1. 日期
        dateItems.add("${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日")
        dateColors.add("#1E293B")
        // 2. 农历 (使用完整版)
        dateItems.add(LunarCalc.lunarFull(today))
        dateColors.add("#64748B")
        // 3. 星期
        dateItems.add("星期${weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]}")
        dateColors.add("#64748B")
        // 4. 节日 (如果有)
        val holiday = LunarCalc.getHoliday(today)
        if (!holiday.isNullOrEmpty()) {
            dateItems.add(holiday)
            dateColors.add("#EF4444")
        }

        // 动态计算每一行的高度并整体居中
        val itemCount = dateItems.size
        val textRowH = Math.min(botH / 4.5f, 45f) // 每行最大高度
        val totalTextH = itemCount * textRowH
        var currentY = botStartY + (botH - totalTextH) / 2f + textRowH / 2f // 整体垂直居中起始 Y

        textPaint.textAlign = Paint.Align.CENTER
        for (i in 0 until itemCount) {
            val text = dateItems[i]
            textPaint.color = Color.parseColor(dateColors[i])
            // 日期稍微大点，其他稍微小点
            textPaint.textSize = if (i == 0) Math.min(textRowH * 0.7f, 40f) else Math.min(textRowH * 0.5f, 30f)
            textPaint.isFakeBoldText = (i == 0 || i == 3) // 日期和节日加粗
            
            canvas.drawText(text, leftW / 2f, getCenteredY(currentY, textPaint), textPaint)
            currentY += textRowH
        }

        // 中间灰色竖线
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 2f
        canvas.drawLine(leftW, botStartY + 10f, leftW, h - 10f, paint)

        // ---------------- 2.2 右下角：人员列表 ----------------
        val personsToShow = getTodayPersons(data)
        val showCount = Math.min(personsToShow.size, 3)
        val listRowH = Math.min(botH / 3.2f, 55f) // 每行人员最大高度
        val listStartY = botStartY + (botH - (showCount * listRowH)) / 2f + listRowH / 2f // 垂直居中起始

        val rightStartX = leftW + 15f
        val rightW = w - rightStartX

        var count = 0
        for (p in personsToShow) {
            if (count >= 3) break
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            val shiftColor = getShiftColor(shiftStr)

            val rowCenterY = listStartY + count * listRowH

            // 1. 姓名 (靠左)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.isFakeBoldText = true
            textPaint.textSize = Math.min(listRowH * 0.45f, 36f)
            canvas.drawText(pName, rightStartX, getCenteredY(rowCenterY, textPaint), textPaint)

            // 2. 班种 (居中)
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = Math.min(listRowH * 0.35f, 28f)
            val typeX = rightStartX + (rightW * 0.35f) // 定位在 35% 处
            canvas.drawText(typeName, typeX, getCenteredY(rowCenterY, textPaint), textPaint)

            // 3. 班次色块 (靠右)
            val badgeW = Math.min(rightW * 0.18f, 50f)
            val badgeH = Math.min(listRowH * 0.6f, 34f)
            val badgeRight = w - 15f
            val badgeLeft = badgeRight - badgeW
            
            // 以 rowCenterY 为绝对中心画框
            rectF.set(badgeLeft, rowCenterY - badgeH / 2f, badgeRight, rowCenterY + badgeH / 2f)
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 6f, 6f, paint)

            // 色块里的文字
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = Math.min(badgeH * 0.65f, 24f)
            canvas.drawText(shiftStr, badgeLeft + badgeW / 2f, getCenteredY(rowCenterY, textPaint), textPaint)

            count++
        }

        if (personsToShow.size > 3) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#94A3B8")
            textPaint.textSize = Math.min(listRowH * 0.35f, 24f)
            canvas.drawText("...", rightStartX, getCenteredY(listStartY + 3 * listRowH, textPaint), textPaint)
        }

        return bitmap
    }

    private fun getShiftColor(shiftStr: String): String {
        return when(shiftStr) {
            "休" -> "#EF4444"
            "白" -> "#3B82F6"
            "夜" -> "#1E293B"
            "二" -> "#0EA5E9"
            else -> "#10B981"
        }
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
