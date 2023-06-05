/*
 * @Copyright: 2020-2021 www.thread0.com Inc. All rights reserved.
 */
package com.thread0.weather.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.thread0.weather.R
import com.thread0.weather.adapter.AdapterForAlarm
import com.thread0.weather.adapter.AdapterForHourlyWeather
import com.thread0.weather.data.constant.getSky
import com.thread0.weather.data.model.Alarm
import com.thread0.weather.data.model.HourlyWeather
import com.thread0.weather.data.model.LocationWeather
import com.thread0.weather.net.service.SunMoonService
import com.thread0.weather.net.service.WeatherService
import com.thread0.weather.util.AMapLocationUtils
import com.thread0.weather.util.TimeUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.litepal.LitePal
import org.litepal.extension.findAll
import top.xuqingquan.app.ScaffoldConfig
import top.xuqingquan.utils.startActivity
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var adapterForHourlyWeather: AdapterForHourlyWeather
    private lateinit var adapterAlarm: AdapterForAlarm
    private lateinit var weathers: MutableList<LocationWeather>
    private lateinit var locationWeather: LocationWeather
    private var index = 0
    private lateinit var locationManager: LocationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 初始化菜单栏
        toolbar.title = ""
        // 设置点击事件
        setClickEvent()
        setSupportActionBar(toolbar)
        // 初始化手势
        initRecyclerView()
        getMenu()
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        weathers = LitePal.findAll<LocationWeather>()
        if (weathers.isNotEmpty()) {
            locationWeather = weathers[index]
        } else {
            locationWeather = LocationWeather()
            weathers.add(locationWeather)
        }
        locationWeather.id = "获取不到"
        // 初始化列表
        //加载数据
        AMapLocationUtils.getInstance().startLocation()
        AMapLocationUtils.getInstance().liveData.observe(this, androidx.lifecycle.Observer {
            if (it.errorCode == 0) {
                //存放定位城市
                locationWeather.id = it.latitude.toString() + ":" + it.longitude.toString()
                if (!it.district.isNullOrBlank()) {
                    locationWeather.id = it.district
                } else {
                    if (!it.city.isNullOrBlank()) {
                        locationWeather.id = it.city
                    }
                }
                AMapLocationUtils.getInstance().stopLocation()
                fetchData()
            } else {
                if (locationWeather.id.equals("获取不到")) {
                    fetchData()
                    return@Observer
                }
            }
        })
    }
    //记录菜单选项名称的下标
    private var indexMenu = 0
    //存放选项名称
    private val textMenu = arrayOf(
        "农历、气节与生肖","空气质量","机动车尾号限行","HMS","查看昨日","查看港口"
    )
    //记录菜单选项icon的下标
    private var imageResourceIndex = 0
    //存放菜单的icon
    private val imageResources = intArrayOf(
        R.mipmap.bg_zodiac_6, R.mipmap.menu_air, R.mipmap.menu_traffic, R.mipmap.menu_camera, R.mipmap.menu_yesterday,
        R.mipmap.menu_port
    )
    //存数据的<key,value>
    var value: MutableMap<String,String> = mutableMapOf()

    //《---设置选项菜单
    private fun getMenu(){
        var boomMenuButton: BoomMenuButton =findViewById(R.id.menu_boom)
        //配置监听
        if(isNetworkConnected(this)) {
            for (i in 0 until boomMenuButton.getPiecePlaceEnum().pieceNumber()) {
                val builder = TextOutsideCircleButton.Builder()
                        .listener { index ->
                            when (index) {
                                0 -> {
                                    startActivity(Intent(this, ZodiacActivity::class.java))
                                }
                                1 -> {
                                    val intent = Intent(this, AirQualityActivity::class.java)
                                    val bundle = Bundle()
                                    bundle.putString("id", locationWeather.id)  //将城市id传过去
                                    intent.putExtras(bundle)
                                    startActivity(intent)
                                }
                                2 -> {
                                    val intent = Intent(this, CarRestrictedInfoActivity::class.java)
                                    val bundle = Bundle()
                                    bundle.putString("id", locationWeather.id)  //将城市id传过去
                                    intent.putExtras(bundle)
                                    startActivity(intent)
                                }
                                3 -> {
                                    startActivity<HmsActivity>()
                                }
                                4 -> {
                                    val intent = Intent(this, YesterdayActivity::class.java)
                                    val bundle = Bundle()
                                    bundle.putString("id", locationWeather.id)  //将城市id传过去
                                    intent.putExtras(bundle)
                                    startActivity(intent)
                                }
                                5 -> {
                                    val intent = Intent(this, PortActivity::class.java)
                                    val bundle = Bundle()
                                    bundle.putString("id", locationWeather.id)  //将城市id传过去
                                    intent.putExtras(bundle)
                                    startActivity(intent)
                                }
                            }
                        }
                        .normalImageRes(getImageResource())
                        .normalText(getext())
                boomMenuButton.addBuilder(builder)
            }
        }else{
            Toast.makeText(this,"没有网络连接",Toast.LENGTH_SHORT).show()
        }

    }
    //获取文本下标
    private fun getext(): String? {
        if (indexMenu >= textMenu.size) indexMenu = 0
        return textMenu[indexMenu++]
    }
    //获取图片下标
    private fun getImageResource(): Int {
        if (imageResourceIndex >= imageResources.size) imageResourceIndex = 0
        return imageResources[imageResourceIndex++]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 1) locationWeather = LocationWeather()
            locationWeather.id = data.getStringExtra("cityId").toString()
            locationWeather.name = data.getStringExtra("name").toString()
//            location.save()
            if (requestCode == 1) weathers.add(locationWeather)
            loadData()
            fetchData()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    //判断是否有网络连接
    fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
        }
        return false
    }

    /**
     * 初始化点击事件
     */
    private fun setClickEvent() {


        search_button.setOnClickListener{
            startActivityForResult(Intent(this,SearchActivity::class.java),1)
        }
        //查看天气
        btn_see_weather.setOnClickListener {
            val intent = Intent(this, FutureWeatherActivity::class.java)
            val bundle = Bundle()
            bundle.putString("id", locationWeather.id)  //将城市id传过去
            intent.putExtras(bundle)
            startActivity(intent)
        }

        srl_main.setOnRefreshListener(OnRefreshListener {
            fetchData()
            Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
            srl_main.isRefreshing = false
        })
    }
    //下拉刷新

    /**
     * 初始化列表
     */
    private fun initRecyclerView() {
        rv_time.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        adapterForHourlyWeather = AdapterForHourlyWeather()
        rv_time.adapter = adapterForHourlyWeather

        rv_alarm.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        adapterAlarm = AdapterForAlarm()
        rv_alarm.adapter = adapterAlarm
    }


    /**
     * 载入数据
     */
    private fun loadData() {
        tv_temperature.text = locationWeather.temperature
        tv_weather.text = locationWeather.weather
        tv_title.text = locationWeather.name
        bg_weather.background=getDrawable(getSky(locationWeather.code).icon)
        cl_main.background = getDrawable(getSky(locationWeather.code).bg)
        tv_sun_rise_info.text = "日出" + locationWeather.sunRise
        tv_sun_set_info.text = "日落" + locationWeather.sunSet
        tv_moon_rise_info.text = "月出" + locationWeather.moonRise
        tv_moon_set_info.text = "月落" + locationWeather.moonSet

    }

    /**
     * 获取数据
     */
    private fun fetchData() {
        val weatherService =
                ScaffoldConfig.getRepositoryManager().obtainRetrofitService(WeatherService::class.java)
        val sunMoonService =
                ScaffoldConfig.getRepositoryManager().obtainRetrofitService(SunMoonService::class.java)
        val data = ArrayList<HourlyWeather>()
        CoroutineScope(Dispatchers.Main).launch {
            supervisorScope {
                //过去24小时天气
                val historyWeatherJob =
                        async { weatherService.getHistoryWeather(location = locationWeather.id) }
                val hourlyWeatherJob =
                        async { weatherService.getHourlyWeather(location = locationWeather.id) }
                val sunInfoJob = async { sunMoonService.getSun(location = locationWeather.id) }
                val moonInfoJob = async { sunMoonService.getMoon(location = locationWeather.id) }
                val currentWeatherJob =
                        async { weatherService.getLocationCurrentWeather(location = locationWeather.id) }
                val dailyWeatherJob = async {
                    weatherService.getDailyWeather(
                            location = locationWeather.id,
                            start = "-1",
                            days = "4"
                    )
                }
                val alarmJob = async { weatherService.getAlarm(location = locationWeather.id) }
                try {
                    val historyWeather = historyWeatherJob.await()
                    val hourlyWeather = hourlyWeatherJob.await()
                    val sunInfo = sunInfoJob.await()
                    val moonInfo = moonInfoJob.await()
                    val currentWeather = currentWeatherJob.await()
                    val dailyWeather = dailyWeatherJob.await()
                    val alarm = alarmJob.await()
                    //未来24小时天气
                    if (hourlyWeather != null) {
                        val result0 = hourlyWeather.results[0].hourly
                        for (i in 1..23) {
                            val cur = result0[i]
                            cur.time = cur.time.substring(11, 16)
                            data.add(cur)
                        }
                        date_locatl.text = result0[0].time.substring(5, 7) + "月" + result0[0].time.substring(8, 10) + "日"
                        week_locatl.text = TimeUtils.getWeekByDateStr(result0[0].time)
                    }
                    adapterForHourlyWeather.setData(data)
                    rv_time.scrollToPosition(22)
                    // 日出日落
                    if (sunInfo != null) {
                        val result0 = sunInfo.results[0].sun[0]
                        locationWeather.sunRise = result0.sunrise
                        locationWeather.sunSet = result0.sunset
                    }
                    //月出月落
                    if (moonInfo != null) {
                        val result0 = moonInfo.results[0].moon[0]
                        locationWeather.moonRise = result0.rise
                        locationWeather.moonSet = result0.set
                    }
                    //当前天气实况
                    if (currentWeather != null) {
                        val result = currentWeather.results[0]
                        locationWeather.temperature = result.now.temperature.toString()
                        locationWeather.weather = result.now.weather
                        locationWeather.code = result.now.code.toString()
                        locationWeather.name = result.location.name
                        locationWeather.id = result.location.id
                    }
                    //今日最高最低温度
                    if (dailyWeather != null) {
                        //昨今明后气温
                        val result0 = dailyWeather.results[0].daily[0]
                        max_min_tempauture.text = result0.low + "℃/" + result0.high + "℃"
                        humidity.text = result0.humidity + "%"
                        wind_speed.text = result0.windDirection + "风 " + result0.windSpeed + "km/h"
                    }
                    //气象预警
                    val list = ArrayList<Alarm>()
                    if (alarm != null) {
                        for (e in alarm.results[0].alarms) {
                            list.add(e)
                        }
                    }
                    adapterAlarm.setData(list)
                }catch (e:Exception){
                    e.printStackTrace()

                }
            }

                locationWeather.lastUpdate = Date()
//                location.save() // 保存数据
                loadData() // 加载数据
            }
        }
    }