package com.thread0.weather.ui.widget

import com.github.mikephil.charting.formatter.ValueFormatter

class WeatherYValueFormatter     /* mFormat = new DecimalFormat("###,###,###,##0");*/(  /*private DecimalFormat mFormat;*/
    private val drawValue: Boolean
) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return super.getFormattedValue(value) + "℃"
    }
}