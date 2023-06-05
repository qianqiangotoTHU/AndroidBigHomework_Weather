package com.thread0.weather.ui.activity

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.tencent.mmkv.MMKV
import com.thread0.weather.R
import com.thread0.weather.util.AMapLocationUtils
import com.zyao89.view.zloading.ZLoadingDialog
import com.zyao89.view.zloading.Z_TYPE
import top.xuqingquan.base.view.activity.SimpleActivity
import top.xuqingquan.utils.startActivity

class EnterActivity : SimpleActivity() {
    private lateinit var locationManager: LocationManager
    private lateinit var context: Context
    private  val LocationServiceCode = 100
    private  val AppServiceCode = 101
    private var dialog = ZLoadingDialog(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter)
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        context = this
        AMapLocationUtils.getInstance().startLocation()
        MMKV.initialize(context)
        checkPermissions()
    }

    //请求权限返回结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==1){
            if(!isLocationServiceEnabled(locationManager,context)){
                Log.d("test","未开启定位服务")
                goLocationServiceSetting(context);
            }else{
                Log.d("test","已开启定位服务")
                start()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationServiceCode -> if (resultCode == RESULT_OK) {
                start()
            }else{
                start()
            }
            AppServiceCode -> if (resultCode == RESULT_OK) {
                if(!isLocationServiceEnabled(locationManager,context)){
                    Log.d("test","未开启定位服务")
                    goLocationServiceSetting(context);
                }else{
                    Log.d("test","已开启定位服务")
                    start()
                }
            }
        }
    }
    private fun start(){
        dialog.dismiss()
        startActivity<MainActivity>()
        finish()
    }
    private fun checkPermissions() {
        //权限检查
        val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Log.d("test","requestPermission")
            Log.d("test", "已授权")
//            flag = 1
            if(!isLocationServiceEnabled(locationManager,context)){
                Log.d("test","未开启定位服务")
                goLocationServiceSetting(context);
            }else{
                Log.d("test","已开启定位服务")
                start()
            }
        } else {
            Log.d("test","requestPermission")
            Log.d("test", "请授权")
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                goAppServiceSetting(context)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
    }



    private fun isLocationServiceEnabled(locationManager: LocationManager, context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                try {
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
                } catch (e: Settings.SettingNotFoundException) {
                    false
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                locationManager.isLocationEnabled
            }
            else -> {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED).isNotEmpty()
            }
        }
    }

    private fun goLocationServiceSetting(context: Context) {
        // 显示一个对话框，点去尝试跳转到权限开关页面
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("位置服务")
                .setMessage("位置服务尚未开启，请前往开启")
                .setPositiveButton("确定"){ dialogInterface: DialogInterface, i: Int ->
                    Log.d("test","dialog")
                    Log.d("test","确定开启定位服务")
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent,LocationServiceCode)
                    } else {
                        Toast.makeText(context, "该设备不支持位置服务", Toast.LENGTH_LONG).show();
                        start()
                    }
                }
                .setNegativeButton("取消"){ dialogInterface: DialogInterface, i: Int ->
                    Log.d("test","dialog")
                    Log.d("test","取消开启定位服务")
                    start()
                }
                .setCancelable(false)
                .create()
                .show()
    }

    private fun goAppServiceSetting(context: Context) {
        // 显示一个对话框，点去尝试跳转到权限开关页面
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("应用设置")
                .setMessage("定位权限尚未开启，请前往开启")
                .setPositiveButton("确定"){ dialogInterface: DialogInterface, i: Int ->
                    Log.d("test","dialog")
                    Log.d("test","确定开启定位权限")
                    val packageURI: Uri = Uri.parse("package:" + getPackageName())
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent,AppServiceCode)
                    } else {
                        Toast.makeText(context, "该设备不支持权限设置服务", Toast.LENGTH_SHORT).show();
                    }
                }
                .setNegativeButton("取消"){ dialogInterface: DialogInterface, i: Int ->
                    Log.d("test","dialog")
                    Log.d("test","取消开启定位权限")
                }
                .setCancelable(false)
                .create()
                .show()
    }
}