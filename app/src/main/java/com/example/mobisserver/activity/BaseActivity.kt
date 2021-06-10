package com.example.mobisserver.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobisserver.MobisApplication
import com.example.mobisserver.OnFragmentInteractionListener
import com.example.mobisserver.service.ServiceManager
import org.json.JSONObject
import kotlin.math.roundToInt

open class BaseActivity : AppCompatActivity(), OnFragmentInteractionListener {
    private val logTag = "BaseActivity"
    var serviceClass: Class<*>? = null
    var mServiceManager: ServiceManager? = null
    var action: String? = null
    var isSettingView = false
    var speaker = "L"
    var isKill = false

    override fun onFragmentInteraction(header: String, body: String) {
        mServiceManager?.sendMessage(header, body)
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mServiceManager = (service as ServiceManager.LocalBinder).getService()
            mServiceManager?.addListener(mServiceManagerListener)
            if (!MobisApplication.isServerConnected) {
                mServiceManager?.onTcpConnect()
            }

            if (action == "Start") {
                Handler().postDelayed({
                    mServiceManager?.sendMessage(
                        "api-command=light&api-action=glass-transparency&api-method=post&sub-system-id=1",
                        "onoff=0"
                    )

                    mServiceManager?.sendMessage(
                        "api-command=setting&api-action=display-app&api-method=get&sub-system-id=1",
                        ""
                    )
                }, 500L)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mServiceManager?.removeListener()
            mServiceManager = null
        }
    }

    private val mServiceManagerListener: ServiceManager.Listener =
        object : ServiceManager.Listener {
            override fun onReceviData(data: String?) {
                val maxLen = 2000 // 2000 bytes 마다 끊어서 출력
                val len: Int = data!!.length
                if (len > maxLen) {
                    var idx = 0
                    var nextIdx = 0
                    while (idx < len) {
                        nextIdx += maxLen
                        idx = nextIdx
                    }
                } else {
//                    Log.e(logTag, data)
                }

                val header = data.split("§")[0]
                val body = data.split("§")[1]
                val api = header.split("&")
                val command = api[0].split("=")[1]
                val action = api[1].split("=")[1]
                val method = api[2].split("=")[1]
                val result = body.split("&")
                if (command == "ecorner") {
                    when (method) {
                        "push" -> {
//                            Log.i(logTag, "command : $command / action : $action / method : $method")
//                            Log.i(logTag, "body : $body")
                            when (action) {
                                "steering-position" -> {
                                    if (MobisApplication.isAttach) {
                                        val direction = result[1].split("=")[1]
                                        pushSteeringPosition(direction)

                                        mServiceManager?.sendMessage(
                                            "api-command=status&api-action=steering-position&api-method=post&sub-system-id=1",
                                            "direction=$direction"
                                        )

                                        mServiceManager?.sendMessage(
                                            "api-command=light&api-action=light&api-method=post&sub-system-id=1",
                                            "type=G"
                                        )
                                    }
                                }

                                "gear" -> {
                                    if (MobisApplication.isAttach) {
                                        val gear = result[1].split("=")[1]
                                        if (gear != "") {
                                            changeGear(gear)

//                                            mServiceManager?.sendMessage(
//                                                "api-command=ecorner&api-action=gear&api-method=post&sub-system-id=1",
//                                                "gear=$gear"
//                                            )
                                        }
                                    }
                                }

                                "lamp" -> {
//                                    if(MobisApplication.isAttach) {
//                                        val lamp = result[1].split("=")[1]
//                                        val value = result[2].split("=")[1]
//                                        when(lamp) {
//                                            "turnleft" -> {
//                                                if(value == "1") {
//                                                    mServiceManager?.sendMessage(
//                                                        "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
//                                                        "level=0&dir=left"
//                                                    )
//                                                }
//                                            }
//                                            "turnright" -> {
//                                                if(value == "1") {
//                                                    mServiceManager?.sendMessage(
//                                                        "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
//                                                        "level=0&dir=right"
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
                                }
                            }
                        }
                    }
                } else {
                    when (method) {
                        "response" -> {
                            val code = result[0].split("=")[1]
                            if (code == "0") {
                                if (action == "driving-safety") {
                                    setDataSyn(action, result[2])
                                } else {
                                    if (result.size > 2) {
                                        val values = ArrayList<String>()
                                        for (i in 2 until result.size) {
                                            values.add(result[i])
                                        }

                                        when (action) {
                                            "display-app" -> {
                                                if (!isSettingView && !MobisApplication.isAttach) {
                                                    onFragmentInteraction(
                                                        "api-command=status&api-action=attach&api-method=post&sub-system-id=1",
                                                        "attach=1"
                                                    )
                                                    onFragmentInteraction(
                                                        "api-command=light&api-action=light&api-method=post&sub-system-id=1",
                                                        "type=D"
                                                    )
                                                }
                                            }
                                            "attach" -> {
                                                val attach = result[2].split("=")[1]
                                                if (attach == "1") {
                                                    MobisApplication.isAttach = true
                                                    onFragmentInteraction(
                                                        "api-command=setting&api-action=profile&api-method=get&sub-system-id=1",
                                                        ""
                                                    )

                                                    Handler().postDelayed({
                                                        onFragmentInteraction(
                                                            "api-command=setting&api-action=cluster&api-method=get&sub-system-id=1",
                                                            ""
                                                        )
                                                    }, 500L)
                                                } else {
                                                    MobisApplication.isAttach = false
                                                }
                                            }
                                        }

                                        setDataSyn(action, values)
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "서버와 통신상태를 확인해 주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        "push" -> {
                            when (action) {
                                "welcome" -> {

                                }
                                "attach" -> {
                                    if (MobisApplication.isAttach) {
                                        val navi = result[5].split("=")[1]
                                        val jsonObject1 = JSONObject(navi)
                                        var destination = jsonObject1.getString("destination")
                                        var route = jsonObject1.getString("route")

                                        if (destination == "null") {
                                            destination = "0"
                                        }
                                        if (route == "null") {
                                            route = "0"
                                        }

                                        setMapData(destination, route)

                                        val climate = result[7].split("=")[1]
                                        val jsonObject2 = JSONObject(climate)
                                        val power = jsonObject2.getString("power_onoff")
                                        val ac = jsonObject2.getString("ac_onoff")
                                        val hot = jsonObject2.getString("hotwire_onoff")
                                        val valve = jsonObject2.getString("valve")

                                        MobisApplication.defroster = hot
                                        MobisApplication.recirculation = valve

                                        setInitClimateDate(power, ac, hot, valve)

                                        val media = result[9].split("=")[1]
                                        val jsonObject3 = JSONObject(media)
                                        val type = jsonObject3.getString("type")
                                        val value = jsonObject3.getString("value")

                                        playMedia(type, value.toInt())

                                        val volumePop = result[11].split("=")[1]
                                        val volume = JSONObject(volumePop)
                                        val vol = volume.getString("volume").toFloat()
                                        MobisApplication.currentVol =
                                            (((vol * 100).roundToInt() / 100f) / 6.6).roundToInt()
                                        MobisApplication.isMute =
                                            volume.getString("muteonoff") != "0"
                                    }
                                }
                                "stt" -> {
                                    if (MobisApplication.isAttach) {
//                                    val id = result[0].split("=")[1]
                                        val text = result[1].split("=")[1]
                                        val direction = result[2].split("=")[1]
                                        speaker = direction

                                        if (text == "Hello mobi") {
                                            MobisApplication.isMobi = true
                                            receiveMobi(text, direction)
                                        } else {
                                            setSttDataSyn(text, direction)
                                        }
                                    }
                                }

                                "init" -> {
                                    finish()

                                    val setting =
                                        Intent(applicationContext, SettingActivity::class.java)
                                    setting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    setting.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    setting.action = "End"
                                    applicationContext.startActivity(setting)
                                }

                                "steering-position" -> {
                                    if (MobisApplication.isAttach) {
                                        val direction = result[1].split("=")[1]
                                        pushSteeringPosition(direction)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onConnected(isConnected: Boolean) {
                when {
                    isConnected -> {
                        MobisApplication.isServerConnected = true
                    }
                    else -> {
                        MobisApplication.isServerConnected = false
                    }
                }
            }
        }

    open fun pushSteeringPosition(type: String) {

    }

    open fun changeGear(gear: String) {

    }

    open fun setDataSyn(action: String, values: ArrayList<String>?) {

    }

    open fun setDataSyn(action: String, json: String) {

    }

    open fun setSttDataSyn(text: String, direction: String) {

    }

    open fun receiveMobi(text: String, direction: String) {

    }

    open fun setInitClimateDate(power: String, ac: String, hot: String, valve: String) {

    }

    open fun playMedia(type: String, value: Int) {

    }

    open fun setMapData(destination: String, route: String) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemUI()

        action = intent.action

        serviceClass = ServiceManager::class.java
    }

    fun setBind() {
        bindService(
            Intent(this, serviceClass),
            mServiceConnection,
            BIND_AUTO_CREATE
        )
    }

//    fun isServiceRunning(serviceClass: Class<*>): Boolean {
//        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//
//        // Loop through the running services
//        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.name == service.service.className) {
//                // If the service is running then return true
//                return true
//            }
//        }
//        return false
//    }

    @SuppressLint("NewApi")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    fun hideSystemUI() {
        val decoView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decoView.systemUiVisibility = uiOptions
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(logTag, "onDestroy")

        MobisApplication.isRotate = false

        // Stop Cloud Speech API
        if (mServiceManager != null) {
            unbindService(mServiceConnection)
            mServiceManager?.removeListener()
            mServiceManager = null
            MobisApplication.isServerConnected = false
        }

        if (isKill) {
            moveTaskToBack(true)
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
