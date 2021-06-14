package com.example.mobisserver

import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import com.anyractive.medroa.ev.pop.util.PreferenceUtil
import com.uber.rxdogtag.RxDogTag
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import java.io.IOException
import java.net.SocketException
import java.util.*

class MobisApplication : Application() {
    lateinit var context: Context

    init {
        instance = this
    }

    companion object {
        private var instance: MobisApplication? = null
        lateinit var prefs: PreferenceUtil
        var musicTime = 0
        var isServerConnected = false
        var isAttach = false
        var isAppStart = false
        var sensorCheck = false
        var powerCheck = false
        var isMobi = false

        var isNavigationFirst = true
        var isClimateFirst = true
        var isMediaFirst = true
        var isCallFirst = true
        var isSettingFirst = true
        var isBlink = false
        var isChangeFragment = false
        var isMute = false
        var currentVol = 6
        var isIndependenceMode = false
        var isDown = false
        var drivingMode = "drive"
        var isDrivingReady = true
        var position = "cluster"
        var recirculation = "0"
        var defroster = "0"
        var viewTrans = "0"
        var preShiftType = ""
        var light = 0F
        var isRotate = false
        lateinit var density: DisplayMetrics
        private var timerTask: Timer? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun resetMusic() {
            timerTask?.cancel()

            musicTime = 0
        }
    }

    override fun onCreate() {
        super.onCreate()
        RxDogTag.install()
        setRxJavaEooroHandler()
//        RxJavaPlugins.setErrorHandler { Log.w("APP#", it) }

//        prefs = PreferenceUtil(applicationContext)
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
}