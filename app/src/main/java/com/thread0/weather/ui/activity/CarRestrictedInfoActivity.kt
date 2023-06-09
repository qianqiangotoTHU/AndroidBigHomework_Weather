/*
 * @Copyright: 2020-2021 www.thread0.com Inc. All rights reserved.
 */
package com.thread0.weather.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.thread0.weather.R
import com.thread0.weather.data.model.Limit
import com.thread0.weather.net.service.CarRestrictedService
import kotlinx.android.synthetic.main.activity_car_restricted_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.xuqingquan.app.ScaffoldConfig
import top.xuqingquan.base.view.activity.SimpleActivity
import top.xuqingquan.extension.launch

/**
 *@ClassName: CarRestrictedInfoActivity
 *@Description: 机动车限行信息页面
 * TODO：1、获取传递进来的城市，需显示
 *       2、通过城市请求限行相关信息，并展示
 *          如：处罚规定、区域与时间、详细说明、限行的具体信息。
 *@Author: hongzf
 *@Date: 2021/6/2 10:59 下午 Created
 */
class CarRestrictedInfoActivity : SimpleActivity() {
    private lateinit var cityId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_restricted_info)
        //获取城市id
        val bundle = intent.extras
        if (bundle != null) {
            cityId = bundle.getString("id").toString()
        }
        loadData()
        // 设置点击事件
        setClickEvent()
        //设置状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //获取窗口区域
            val window: Window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            //设置显示为白色背景，黑色字体
            window.decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }

    private fun setClickEvent() {
        tb_car_info.setNavigationOnClickListener {
            finish()
        }
    }
    /**
     * 载入数据
     */
    private fun loadData() {

        val carRestrictedService =
            ScaffoldConfig.getRepositoryManager().obtainRetrofitService(CarRestrictedService::class.java)
        launch(Dispatchers.IO,{
            val result = carRestrictedService.getCarRestricted(location = cityId)
            if(result != null){
                withContext(
                    Dispatchers.Main
                ){
                    val result0 = result.results[0]
                    tv_penalty_info.text = result0.restriction.penalty
                    tv_limit_area_info.text = result0.restriction.region
                    tv_limit_time_info.text = result0.restriction.time
                    tv_remarks_info.text = result0.restriction.remarks

                    //今天
                    if(result0.restriction.limits!=null){
                        var cur = result0.restriction.limits[0].memo+"："
                        for(e in result0.restriction.limits[0].plates){
                            cur += "$e、"
                        }
                        cur = cur.substring(0,cur.length-1)
                        tv_car_limit_today_details.text = cur
                    }
                    else tv_car_limit_today_details.text = "无"
                    //明天
                    if(result0.restriction.limits!=null){
                        var cur = result0.restriction.limits[1].memo+"："
                        for(e in result0.restriction.limits[1].plates){
                            cur += "$e、"
                        }
                        cur = cur.substring(0,cur.length-1)
                        tv_car_limit_tomorrow_details.text = cur
                    }
                    else tv_car_limit_tomorrow_details.text = "无"
                    //后天
                    if(result0.restriction.limits!=null){
                        var cur = result0.restriction.limits[2].memo+"："
                        for(e in result0.restriction.limits[2].plates){
                            cur += "$e、"
                        }
                        cur = cur.substring(0,cur.length-1)
                        tv_car_limit_after_tomorrow_details.text = cur
                    }
                    else tv_car_limit_after_tomorrow_details.text = "无"
                }
            }
        },{
            it.printStackTrace()
        },{

        })

    }
}