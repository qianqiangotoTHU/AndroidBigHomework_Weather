/*
 * @Copyright: 2020-2021 www.thread0.com Inc. All rights reserved.
 */
package com.thread0.weather.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thread0.weather.R
import com.thread0.weather.adapter.AdapterForAirQuaH
import com.thread0.weather.adapter.AdapterForAirQuaV
import com.thread0.weather.data.model.AirQualityDailyBean
import com.thread0.weather.data.model.AirQualityHourlyBean
import com.thread0.weather.net.service.AirQualityrService
import com.thread0.weather.util.AQIUtil
import com.thread0.weather.util.TimeUtils
import kotlinx.android.synthetic.main.activity_air_quality.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.xuqingquan.app.ScaffoldConfig
import top.xuqingquan.base.view.activity.SimpleActivity
import top.xuqingquan.extension.launch
import kotlin.collections.ArrayList

/**
 *@ClassName: AirQualityActivity
 *@Description: 空气质量页面
 * TODO：1、获取
 *          1.1、当前空气质量
 *          1.2、逐日空气质量
 *          1.3、逐小时空气质量
 *          1.4、历史逐小时空气质量
 *          按重要程度进行展示，如布局所示使用NestedScrollView包括展示以上数据的各个view，使其可以滚动展示大量数据
 *      2、当然在获取这些数据前需要获取传递进来的城市，并在界面上显示
 *      3、界面中需要放置一个进入AirQualityRankActivity界面的按钮，按钮文字——空气质量排行榜
 *@Author: hongzf
 *@Date: 2021/6/2 10:58 下午 Created
 */
class AirQualityActivity : SimpleActivity() {

    private lateinit var adapterH: AdapterForAirQuaH
    private lateinit var adapterV: AdapterForAirQuaV
    private lateinit var cityId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_air_quality)
        //初始化列表
        initRecyclerView()
        //获取城市id
        val bundle = intent.extras
        if (bundle != null) {
            cityId = bundle.getString("id").toString()
        }
        //加载数据
        loadData()
        // 设置点击事件
        setClickEvent()

    }

    private fun setClickEvent() {
        tb_air_quality.setNavigationOnClickListener {
            finish()
        }
        //空气质量排行榜
        btn_airRank.setOnClickListener {
            startActivity(Intent(this, AirQualityRankActivity::class.java))
        }
        //下拉刷新
        srl_air_quality.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
           loadData()
           Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
          srl_air_quality.isRefreshing = false
       })
    }
    /**
     * 初始化列表
     */
    private fun initRecyclerView() {
        rv_time_air.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        adapterH = AdapterForAirQuaH()
        rv_time_air.adapter = adapterH

        rv_day_air.layoutManager = LinearLayoutManager(this)
        adapterV = AdapterForAirQuaV()
        rv_day_air.adapter = adapterV
    }

    /**
     * 载入数据
     */
    private fun loadData() {
        //当前空气质量
        val airQualityService =
            ScaffoldConfig.getRepositoryManager().obtainRetrofitService(AirQualityrService::class.java)
        launch {
            val result = airQualityService.getAirQuality(location = cityId)
            if (result != null) {
                withContext(
                    Dispatchers.Main
                ) {
                    val result0 = result.results[0]
                    tv_pm25Val.text = result0.air.city.pm25
                    tv_pm10Val.text = result0.air.city.pm10
                    tv_so2Val.text = result0.air.city.so2
                    tv_no2Val.text = result0.air.city.no2
                    tv_o3Val.text = result0.air.city.o3
                    tv_coVal.text = result0.air.city.co
                    tv_area.text = result0.location.name
                    tv_airLevel.text = result0.air.city.quality
//                    tv_airQuality.text = result0.air.city.aqi.toString()
                    tv_day.text = result0.lastUpdate.substring(0, 10)
                    tv_time.text = result0.lastUpdate.substring(11, 16)
                    val color = AQIUtil.getColorOx(result0.air.city.aqi)  //获取AQI对应颜色
                    tv_airLevel.setTextColor(Color.parseColor(color))
                    if(result0.air.city.aqi>=50){
                        airquality_layout.setBackgroundResource(R.mipmap.bg_fog)
                    }
                }
            }
        }

        //逐日空气质量
        launch {
            val result = airQualityService.getAirQualityDaily(location = cityId)
            val airQualityDaily = ArrayList<AirQualityDailyBean>()
            if(result!=null){
                for((index,e) in result.results[0].daily.withIndex()){
                    val week = TimeUtils.getWeekByDateStr(e.date)
                    val date = when(index){
                        0->"今天"
                        1->"明天"
                        2->"后天"
                        else->e.date.substring(5,10)
                    }
                    val color = AQIUtil.getColor(e.aqi)
                    val cur = AirQualityDailyBean(week,date,e.aqi.toString(), e.quality,color)
                    airQualityDaily.add(cur)
                }
                withContext(
                    Dispatchers.Main
                ){
                    adapterV.setData(airQualityDaily)
                }
            }
        }

        //历史逐小时空气质量和逐小时空气质量
        launch {
            //历史逐小时空气质量
            val result = airQualityService.getAirQualityHourlyHistory(location = cityId)
            val airQualityHourly = ArrayList<AirQualityHourlyBean>()
            if(result!=null){
                val e = result.results[0].hourlyHistory
                for(i in 23 downTo 0){
                    val time = when(i){
                        0->"现在"
                        else->e[i].city.lastUpdate.substring(11,16)
                    }
                    val color = AQIUtil.getColor(e[i].city.aqi)
                    val cur = AirQualityHourlyBean(time,e[i].city.quality,e[i].city.aqi.toString(),color)
                    airQualityHourly.add(cur)
                }
            }
            //逐小时空气质量
            val result2 = airQualityService.getAirQualityHourly(location = cityId)
            if(result2!=null){
                val e = result2.results[0].hourly
                for(i in 0..23){
                    var time = e[i].time.substring(11,16)
                    if(time == "00:00")time="明天"
                    val color = AQIUtil.getColor(e[i].aqi)
                    val cur = AirQualityHourlyBean(time,e[i].quality,e[i].aqi.toString(),color)
                    airQualityHourly.add(cur)
                }
            }
            withContext(
                Dispatchers.Main
            ){
                adapterH.setData(airQualityHourly)
               rv_time_air.scrollToPosition(20)
            }
        }
    }
}