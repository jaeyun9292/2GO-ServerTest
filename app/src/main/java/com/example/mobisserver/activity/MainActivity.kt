package com.example.mobisserver.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anyractive.medroa.ev.pop.util.PreferenceUtil
import com.example.mobisserver.OnFragmentInteractionListener
import com.example.mobisserver.R
import com.example.mobisserver.databinding.ActivityMainBinding
import com.example.mobisserver.service.ServiceManager
import com.uber.rxdogtag.RxDogTag
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import java.io.IOException
import java.net.SocketException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), View.OnClickListener, OnFragmentInteractionListener {
    private val logTag = "MainActivity"
    var isbinding = false
    var serviceClass: Class<*>? = null
    var mServiceManager: ServiceManager? = null
    var isKill = false
    var isServerConnected = false
    lateinit var ip: String
    lateinit var port: String
    lateinit var binding: ActivityMainBinding

    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        RxDogTag.install()
        setRxJavaEooroHandler()

        serviceClass = ServiceManager::class.java
        prefs = PreferenceUtil(applicationContext)

        binding.serverStart.setOnClickListener(this)
        binding.serverStop.setOnClickListener(this)
        binding.rotateStart.setOnClickListener(this)
        binding.rotateEnd.setOnClickListener(this)
        binding.stepOne.setOnClickListener(this)
        binding.stepTwo.setOnClickListener(this)
        binding.stepThree.setOnClickListener(this)
        binding.stop.setOnClickListener(this)
        binding.turnLeft.setOnClickListener(this)
        binding.turnRight.setOnClickListener(this)
        binding.parkingStart.setOnClickListener(this)
        binding.parkingEnd.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.server_start -> {
                // 소켓 연결 시작
                Log.e(TAG, "btn_start_click: ")
                if (!isbinding) {
                    ip = binding.editIp.text.toString()
                    port = binding.editPort.text.toString()
                    prefs.setString("ip", ip)
                    prefs.setString("port", port)
                    setBind()
                    isbinding = true
                    Log.e(TAG, "isbinding: " + isbinding)
                }
            }

            R.id.server_stop -> {
                // 소켓 연결 중지
                Log.e(TAG, "btn_stop_click: ")
                if (isbinding) {
                    setunBind()
                    isbinding = false
                    Log.e(TAG, "isbinding: " + isbinding)
                }
            }

            R.id.rotate_start -> {
                // 180도 회전 시작
                onFragmentInteraction(
                    "api-command=ecorner&api-action=rotate-half&api-method=post&sub-system-id=1",
                    "onoff=1"
                )
            }
            R.id.rotate_end -> {
                // 180도 회전 끝
                onFragmentInteraction(
                    "api-command=ecorner&api-action=rotate-half&api-method=post&sub-system-id=1",
                    "onoff=0"
                )
            }
            R.id.step_one -> {
                // 1단계
                onFragmentInteraction(
                    "api-command=status&api-action=init&api-method=post&sub-system-id=1",
                    ""
                )
            }
            R.id.step_two -> {
                // 2단계
                onFragmentInteraction(
                    "api-command=status&api-action=welcome&api-method=post&sub-system-id=1",
                    ""
                )
            }
            R.id.step_three -> {
                // 3단계
                onFragmentInteraction(
                    "api-command=status&api-action=attach&api-method=post&sub-system-id=1",
                    "attach=1"
                )
            }
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////

//            }R.id.step_one -> {
//                // 1단계
//                mServiceManager?.sendMessage(
//                    "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
//                    "level=1&dir=forward"
//                )
//            }
//            R.id.step_two -> {
//                // 2단계
//                mServiceManager?.sendMessage(
//                    "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
//                    "level=2&dir="
//                )
//            }
//            R.id.step_three -> {
//                // 3단계
//                mServiceManager?.sendMessage(
//                    "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
//                    "level=3&dir="
//                )
//            }
            R.id.stop -> {
                // 정차
                Handler().postDelayed({
                    onFragmentInteraction(
                        "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
                        "level=0&dir=forward"
                    )
                }, 500L)
            }
            R.id.turn_left -> {
                // 좌회전
                onFragmentInteraction(
                    "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
                    "level=2&dir=left"
                )
            }
            R.id.turn_right -> {
                // 우회전
                onFragmentInteraction(
                    "api-command=ecorner&api-action=speed&api-method=post&sub-system-id=1",
                    "level=2&dir=right"
                )
            }
            R.id.parking_start -> {
                // 오토파킹 시작
                Handler().postDelayed({
                    onFragmentInteraction(
                        "api-command=ecorner&api-action=parallel-park&api-method=post&sub-system-id=1",
                        "onoff=1"
                    )
                }, 5000L)
            }
            R.id.parking_end -> {
                // 오토파킹 끝
                Handler().postDelayed({
                    onFragmentInteraction(
                        "api-command=ecorner&api-action=parallel-park&api-method=post&sub-system-id=1",
                        "onoff=0"
                    )
                }, 18000L)
            }
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.e("mServiceConnection", "onServiceConnected: ")
            mServiceManager = (service as ServiceManager.LocalBinder).getService()
            mServiceManager?.addListener(mServiceManagerListener)
            if (!isServerConnected) {
                mServiceManager?.onTcpConnect()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("mServiceConnection", "onServiceDisConnected: ")
            mServiceManager?.removeListener()
            mServiceManager = null
        }
    }

    // 리스너
    private val mServiceManagerListener: ServiceManager.Listener =
        object : ServiceManager.Listener {
            override fun onReceviData(data: String?) {
                Log.e(TAG, "onReceviData:" + data)
                val maxLen = 2000 // 2000 bytes 마다 끊어서 출력
                val len: Int = data!!.length
                if (len > maxLen) {
                    var idx = 0
                    var nextIdx = 0
                    while (idx < len) {
                        nextIdx += maxLen
                        idx = nextIdx
                    }
                }
                val header = data.split("§")[0]
                val body = data.split("§")[1]
                val api = header.split("&")
                val command = api[0].split("=")[1]
                val action = api[1].split("=")[1]
                val method = api[2].split("=")[1]
                val result = body.split("&")
            }

            override fun onConnected(isConnected: Boolean) {
                when {
                    isConnected -> {
                        Log.e(TAG, "onConnected: true")
                        isServerConnected = true
                    }
                    else -> {
                        Log.e(TAG, "onConnected: false")
                        isServerConnected = false
                    }
                }
            }
        }

    private fun setRxJavaEooroHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            var error = e
            if (error is UndeliverableException) {
                error = e.cause
            }
            if (error is IOException || error is SocketException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (error is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if (error is NullPointerException || error is IllegalArgumentException) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
            if (error is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
        }
    }

    override fun onFragmentInteraction(header: String, body: String) {
        mServiceManager?.sendMessage(header, body)
    }

    private fun setBind() {
        Log.e(TAG, "setBind: ")
        bindService(
            Intent(this, serviceClass),
            mServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    private fun setunBind() {
        Log.e(TAG, "setunBind: ")
        unbindService(
            mServiceConnection
        )
    }

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
        if (mServiceManager != null) {
            unbindService(mServiceConnection)
            mServiceManager?.removeListener()
            mServiceManager = null
            isServerConnected = false
        }

        if (isKill) {
            moveTaskToBack(true)
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}