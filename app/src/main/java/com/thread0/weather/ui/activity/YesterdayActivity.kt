/*
 * @Copyright: 2020-2021 www.thread0.com Inc. All rights reserved.
 */
package com.thread0.weather.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.thread0.weather.R
import com.thread0.weather.adapter.AdapterForHourlyWeather
import com.thread0.weather.data.constant.getSky
import com.thread0.weather.data.model.*
import com.thread0.weather.net.service.WeatherService
import com.thread0.weather.ui.widget.WeatherYValueFormatter
import com.thread0.weather.ui.widget.WeatherYValueFormatter1
import kotlinx.android.synthetic.main.activity_yesterday.*
import kotlinx.android.synthetic.main.activity_yesterday.toolbar
import top.xuqingquan.app.ScaffoldConfig
import top.xuqingquan.base.view.activity.SimpleActivity
import top.xuqingquan.extension.launch

/**
 *@ClassName: YesterdayActivity
 *@Description: 昨日天气页面
 *TODO：1、通过传递进来的城市数据获取对应的昨日天气数据并展示出来
 *      2、当前界面需要显示当前城市名称、日期（天气数据的日期）
 *      3、自行思考如何布局，把用户最关心的数据显眼的展示到界面上
 *@Author: hongzf
 *@Date: 2021/6/2 11:00 下午 Created
 */
class YesterdayActivity : SimpleActivity() {
    private lateinit var adapterForHourlyWeather: AdapterForHourlyWeather
    // view binding
    private lateinit var cityId: String

    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yesterday)
        initRecyclerView()
        val bundle = intent.extras
        if (bundle != null) {
            cityId = bundle.getString("id").toString()
        }
        getYesterdayWeather()
        getYesterdayhumdity()
        loadData()
        // 设置点击事件
        setClickEvent()

    }

     private fun getYesterdayWeather(){
        val weatherService =
            ScaffoldConfig.getRepositoryManager().obtainRetrofitService(WeatherService::class.java)
        //获取24小时天气结果
         launch{
        val yesterdayResult = weatherService.getHistoryWeather(cityId)

        val result= yesterdayResult!!.results[0].hourlyHistory
        //获取昨日天气最值
        var max:Int = result.elementAt(0).temperature.toInt()
        var min:Int = result.elementAt(0).temperature.toInt()
        for (i in result.indices) {
            if (max<result[i].temperature.toInt()){
                max=result[i].temperature.toInt()
            }
            if (min>result[i].temperature.toInt()){
                min=result[i].temperature.toInt()
            }
        }
        val titleTextView: TextView = findViewById(R.id.weather_max)
        titleTextView.text =  "昨日最高温度为" + max + "℃" + "昨日最低温度为" + min + "℃"
        //获取xy的string值
        var xText:ArrayList<String> = ArrayList()

        //<x,y>
        val entries: MutableList<Entry> = ArrayList()
        for (i in result.indices) {
            val data = result[i].temperature
            val time = result[i].lastUpdate.split("T")[1].split("[+]")[0].split(":")[0]
            xText.add(time+"：00")
            entries.add(Entry(i.toFloat(), data.toFloat()))
        }

        //修改描述
        var des: Description =main_yesterday_24_chart.description
        des.text="昨日温度"
             //设置背景图
             layout_yesterday.background=getDrawable(getSky(result[23].code).bg)
        //设置折线
        val dataSet = LineDataSet(entries, "℃")

        //获取x轴
        val xLine: XAxis = main_yesterday_24_chart.getXAxis()
        //数值相隔1
        xLine.setGranularity(1f)
        //设置x轴位置
        xLine.setPosition(XAxis.XAxisPosition.BOTTOM)
        //去掉竖线
        xLine.setDrawGridLines(false);
        //避免剪掉最值
        xLine.setAvoidFirstLastClipping(true)
        //设置x格式
        xLine.setValueFormatter(IndexAxisValueFormatter(xText))

        //去掉y轴和值
        val yLineLeft: YAxis =main_yesterday_24_chart.axisLeft
        val yLineRight: YAxis =main_yesterday_24_chart.axisRight
        yLineLeft.setDrawAxisLine(false)
        yLineRight.setDrawAxisLine(false)
        yLineLeft.setDrawLabels(false)
        yLineRight.setDrawLabels(false)
        yLineLeft.setDrawGridLines(false);
        yLineRight.setDrawGridLines(false);
        yLineLeft.valueFormatter= WeatherYValueFormatter (true)
        //设置点数据字体
        dataSet.setValueTextSize(15F)
        //关闭高亮
        dataSet.setHighlightEnabled(false)
        //设置数据格式
        dataSet.valueFormatter= WeatherYValueFormatter (true)

        //线性数据
        val lineData = LineData(dataSet)

        //设置数据
        main_yesterday_24_chart.setData(lineData)
        //禁止缩放图标坐标
        main_yesterday_24_chart.setDoubleTapToZoomEnabled(false)
        //启动滚动图表
        main_yesterday_24_chart.setDragDecelerationEnabled(true)
        //拖动惯性
        main_yesterday_24_chart.setDragDecelerationFrictionCoef(0.5F)
        //禁止双击缩放图表
        main_yesterday_24_chart.setDoubleTapToZoomEnabled(false)
        //设置可用
        main_yesterday_24_chart.setEnabled(true)
        //设置滚动
        main_yesterday_24_chart.setVisibleXRange(0f,5f)

        //绘制
        main_yesterday_24_chart.invalidate();
         }
    }
    private fun loadData(){
        val weatherService =
                ScaffoldConfig.getRepositoryManager().obtainRetrofitService(WeatherService::class.java)
        val data = ArrayList<HourlyWeather>()
        launch{
            val yesterdayResult = weatherService.getHistoryWeather(cityId)
            if(yesterdayResult!=null){
                val result0=yesterdayResult.results[0].hourlyHistory
                for (i in 23 downTo 0) {
                    data.add(
                            HourlyWeather(
                                    result0[i].lastUpdate.substring(11, 14) + "00",
                                    result0[i].text,
                                    result0[i].code,
                                    result0[i].temperature,
                                    result0[i].humidity,
                                    result0[i].windDirection,
                                    result0[i].windSpeed
                            )
                    )
                }
                adapterForHourlyWeather.setData(data)
                rv_weather_time.scrollToPosition(0)
            }
        }
    }
    private fun initRecyclerView() {
        rv_weather_time.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        adapterForHourlyWeather = AdapterForHourlyWeather()
        rv_weather_time.adapter = adapterForHourlyWeather

    }
    private fun getYesterdayhumdity(){
        val weatherService =
                ScaffoldConfig.getRepositoryManager().obtainRetrofitService(WeatherService::class.java)
        //获取24小时天气结果
        launch{
            val yesterdayResult = weatherService.getHistoryWeather(cityId)

            val result= yesterdayResult!!.results[0].hourlyHistory
            //获取昨日天气最值
            var max:Int = result.elementAt(0).humidity.toInt()
            var min:Int = result.elementAt(0).humidity.toInt()
            for (i in result.indices) {
                if (max<result[i].humidity.toInt()){
                    max=result[i].humidity.toInt()
                }
                if (min>result[i].humidity.toInt()){
                    min=result[i].humidity.toInt()
                }
            }
            val titleTextView: TextView = findViewById(R.id.humidity_max)
            titleTextView.text =  "昨日最高湿度为" + max + "%" + "昨日最低湿度为" + min + "%"
            //获取xy的string值
            var xText:ArrayList<String> = ArrayList()

            //<x,y>
            val entries: MutableList<BarEntry> = ArrayList()
            for (i in result.indices) {
                val data = result[i].humidity
                val time = result[i].lastUpdate.split("T")[1].split("[+]")[0].split(":")[0]
                xText.add(time+"：00")
                entries.add(BarEntry(i.toFloat(), data.toFloat()))
            }

            //修改描述
            var des: Description =main_yesterday_24_humidity.description
            des.text="昨日湿度"
            //设置折线
            val dataSet = BarDataSet(entries, "%")

            //获取x轴
            val xLine: XAxis = main_yesterday_24_humidity.getXAxis()
            //数值相隔1
            xLine.setGranularity(1f)
            //设置x轴位置
            xLine.setPosition(XAxis.XAxisPosition.BOTTOM)
            //去掉竖线
            xLine.setDrawGridLines(false);
            //避免剪掉最值
            xLine.setAvoidFirstLastClipping(true)
            //设置x格式
            xLine.setValueFormatter(IndexAxisValueFormatter(xText))

            //去掉y轴和值
            val yLineLeft: YAxis =main_yesterday_24_humidity.axisLeft
            val yLineRight: YAxis =main_yesterday_24_humidity.axisRight
            yLineLeft.setDrawAxisLine(false)
            yLineRight.setDrawAxisLine(false)
            yLineLeft.setDrawLabels(false)
            yLineRight.setDrawLabels(false)
            yLineLeft.setDrawGridLines(false);
            yLineRight.setDrawGridLines(false);
            yLineLeft.valueFormatter= WeatherYValueFormatter1 (true)
            //设置点数据字体
            dataSet.setValueTextSize(15F)
            //关闭高亮
            dataSet.setHighlightEnabled(false)
            //设置数据格式
            dataSet.valueFormatter= WeatherYValueFormatter1 (true)

            //线性数据
            val barData = BarData(dataSet)

            //设置数据
            main_yesterday_24_humidity.setData(barData)
            //禁止缩放图标坐标
            main_yesterday_24_humidity.setDoubleTapToZoomEnabled(false)
            //启动滚动图表
            main_yesterday_24_humidity.setDragDecelerationEnabled(true)
            //拖动惯性
            main_yesterday_24_humidity.setDragDecelerationFrictionCoef(0.35F)
            //禁止双击缩放图表
            main_yesterday_24_humidity.setDoubleTapToZoomEnabled(false)
            //设置可用
            main_yesterday_24_humidity.setEnabled(true)
            //设置滚动
            main_yesterday_24_humidity.setVisibleXRange(0f,5f)

            //绘制
            main_yesterday_24_humidity.invalidate();
        }
    }

    private fun setClickEvent() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}