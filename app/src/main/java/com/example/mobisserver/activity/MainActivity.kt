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
import com.example.mobisserver.databinding.ActivityMainBinding
import com.example.mobisserver.service.ServiceManager
import com.uber.rxdogtag.RxDogTag
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import java.io.IOException
import java.net.SocketException

class MainActivity : AppCompatActivity() {
    private val logTag = "MainActivity"
    var isbinding = false
    var serviceClass: Class<*>? = null
    var mServiceManager: ServiceManager? = null
    var isKill = false
    var isServerConnected = false
    lateinit var ip: String
    lateinit var port: String

    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        RxDogTag.install()
        setRxJavaEooroHandler()

        serviceClass = ServiceManager::class.java

        prefs = PreferenceUtil(applicationContext)

        binding.start.setOnClickListener {
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

        binding.stop.setOnClickListener {
            Log.e(TAG, "btn_stop_click: ")
            if (isbinding) {
                setunBind()
                isbinding = false
                Log.e(TAG, "isbinding: " + isbinding)
            }
        }

        binding.send.setOnClickListener {
            Log.e(TAG, "btn_send_click: ")
            if (isServerConnected) {
                Log.e(TAG, "sendMessage")
                Handler().postDelayed({
                    mServiceManager?.sendMessage(   //EXAMPLE
                        "api-command= YOURCODE1 &api-action= YOURCODE2 &api-method= YOURCODE3 &sub-system-id= YOURCODE4 ",
                        "onoff=0"
                    )
                }, 500L)
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
                    Log.e(TAG, "onReceviData:" + data)
                }

                val header = data.split("§")[0]
                val body = data.split("§")[1]
                val api = header.split("&")
                val command = api[0].split("=")[1]
                val action = api[1].split("=")[1]
                val method = api[2].split("=")[1]
                val result = body.split("&")
                if (header == null) {
                    Toast.makeText(
                        applicationContext,
                        "서버와 통신상태를 확인해 주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                    .uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
            if (error is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler
                    .uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
        }
    }

    fun setBind() {
        Log.e(TAG, "setBind: ")
        bindService(
            Intent(this, serviceClass),
            mServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    fun setunBind() {
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

    @Suppress("DEPRECATION")
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