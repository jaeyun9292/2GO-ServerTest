package com.example.mobisserver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.mobisserver.MobisApplication
import com.example.mobisserver.R
import com.example.mobisserver.activity.MainActivity
import com.example.mobisserver.net.SocketSession
import com.example.mobisserver.receiver.PowerBroadcastReceiver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.apache.commons.lang3.StringUtils
import kotlin.math.sqrt

class ServiceManager : Service(), SensorEventListener {
    private val logcat = "ServiceManager"
    private val power: BroadcastReceiver = PowerBroadcastReceiver()
    private val disposables by lazy { CompositeDisposable() }
    private val crlf = "\r\n".toByteArray()
    private var ip = ""
    private var port = 0
    private val mBinder: IBinder = LocalBinder()
    private var magni = 0.0
    private var sensorCount = -1
    private var sendRy = 0F
    private var testCount = 0
    private var testX = 0F
    private var testY = 0F
    private var testZ = 0F

    //    private val smoother: Smoother = Smoother()
    private val smoother1: Smoother = Smoother()

    //    private val mListeners = ArrayList<Listener>()
    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager  //센서 매니저에대한 참조를 얻기위함
    }

    companion object {
        var mListener: Listener? = null
    }

    interface Listener {
        fun onReceviData(data: String?)
        fun onConnected(isConnected: Boolean)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(logcat, "onCreate")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        registerReceiver(power, filter)

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        startForegroundService()
    }

    class LocalBinder : Binder() {
        fun getService(): ServiceManager {
            return ServiceManager()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("servicesTest", "onBind")
        return mBinder
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // get sensor when Change
        when (event?.sensor?.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val magX = event.values[0]
                val magY = event.values[1]
                val magZ = event.values[2]

                magni = sqrt((magX * magX + magY * magY + magZ * magZ).toDouble())
//                magni = smoother.getSmoothedCursor(magni.toFloat())
                Log.e("Sensor", "magX : $magX / magY : $magY / magZ : $magZ / magni : $magni")

//                if (testCount == 5) {
//                    testX /= testCount
//                    testY /= testCount
//                    testZ /= testCount
//
//                    Log.e("Sensor", "testX : $testX / testY : $testY / testZ : $testZ")
//
//                    testCount = 0
//                    testX = 0F
//                    testY = 0F
//                    testZ = 0F
//                } else {
//                    testCount ++
//                    testX += magX
//                    testY += magY
//                    testZ += magZ
//                }

                if (magni > 500) {
                    if (!MobisApplication.isAppStart) {
                        MobisApplication.isAppStart = true
                        MobisApplication.sensorCheck = false
                        val i = Intent(this, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        i.action = "Start"
                        startActivity(i)
                    }
                } else if (magni < 200) {
                    if (MobisApplication.isAppStart && !MobisApplication.sensorCheck) {
//                        MobisApplication.isAppStart = false
                        MobisApplication.sensorCheck = true
                        val i = Intent(this, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        i.action = "End"
                        startActivity(i)
                    }
                }
            }
        }

        if (MobisApplication.isAttach) {
            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val rY = event.values?.get(1)
                    if (rY!! > -8.5 && rY < 8.5) {
//                    if(preRy != rY.toInt()) {
                        sendRy = smoother1.getSmoothedCursor(rY).toFloat()
                        sensorCount++
                        if (sensorCount == 0) {
                            Handler().postDelayed({
                                if (rY > -8.5 && rY < 8.5 && MobisApplication.isAttach) {
                                    sendRy *= 10
                                    sendMessage(
                                        "api-command=ecorner&api-action=steering-wheel&api-method=post&sub-system-id=1",
                                        "degree=$sendRy"
                                    )
//                                    Log.e("Sensor1111", "rY : $rY / smoother1 : $sendRy")
                                }
                                sensorCount = -1
                            }, 200L)
                        }
                    } else {
                        sensorCount++
                        if (sensorCount == 0) {
                            Handler().postDelayed({
                                sendRy = if (rY > 0) {
                                    85F
                                } else {
                                    -85F
                                }

                                sendMessage(
                                    "api-command=ecorner&api-action=steering-wheel&api-method=post&sub-system-id=1",
                                    "degree=$sendRy"
                                )
//                                Log.e("Sensor2222", "rY : $rY / smoother1 : $sendRy")
                                sensorCount = -1
                            }, 200L)
                        }
                    }
                }
                Sensor.TYPE_LIGHT -> {
                    MobisApplication.light = event.values?.get(0)!!
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        when(sensor?.type) {
//            Sensor.TYPE_LIGHT -> {
//                Log.e("Sensor", "onAccuracyChanged")
//                Handler().postDelayed({
//                    lightAvg = light / lightCount
//                    lightCount = 0
//                    Log.e("Sensor", "lightAvg : $lightAvg")
//                }, 5000L)
//            }
//        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("servicesTest", "onUnbind")
        return super.onUnbind(intent)
    }

    fun addListener(listener: Listener) {
        Log.d("servicesTest", "addListener")
        mListener = listener
    }

    fun removeListener() {
        Log.d("servicesTest", "removeListener")
        mListener = null
    }

    fun onTcpConnect() {
        Log.d(logcat, "onTcpConnect")
        ip = MobisApplication.prefs.getString("ip", "192.168.11.2")
        port = MobisApplication.prefs.getString("port", "8181").toInt()

        if (SocketSession.socket == null) {
            Log.d(logcat, "Enter")
            SocketSession.dataInit()
            SocketSession.createSocket(ip, port)

            readNetworkData()
            readConnectState()
        }

//        if (networkManager == null) {
//            Log.d(Tag, "1111")
//            networkManager = SocketSession()
//            if(networkManager!!.socket == null) {
//                Log.d(Tag, "2222")
//                networkManager!!.createSocket(ip, port)
//            }
//            else {
//                Log.d(Tag, "3333")
//                networkManager!!.close()
//            }
//
//            readNetworkData()
//            readConnectState()
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(logcat, "onStartCommand : $action")
        when (action) {
            "Start" -> {
                onTcpConnect()

                //            Handler().postDelayed({
                //                sendMessage("api-command=status&api-action=welcome&api-method=post&sub-system-id=1","")
                //            }, 500L)

            }
            "Welcome" -> {
                sendMessage(
                    "api-command=status&api-action=welcome&api-method=post&sub-system-id=1",
                    ""
                )
            }
            "End" -> {
                //            sendMessage("api-command=status&api-action=init&api-method=post&sub-system-id=1","")
                sendMessage(
                    "api-command=status&api-action=attach&api-method=post&sub-system-id=1",
                    "attach=1"
                )
            }
        }

        return START_STICKY
    }

    fun sendMessage(header: String, body: String) {
//        Log.d(logcat, "Enter : header - $header\n body - $body")
        val headerBuilder = StringBuilder()
        headerBuilder.append(header)

        val uri = Uri.parse("http://localhost/?$header")

        val contentLength = uri.getQueryParameter("content-length")
        if (StringUtils.isEmpty(contentLength)) {
            val bodyBytes = body.toByteArray(charset("UTF-8"))
            headerBuilder.append("&content-length=" + bodyBytes.size)
        }

        val bodyBuilder = StringBuilder()
        bodyBuilder.append(body)

        val packetBuilder = StringBuilder()
        packetBuilder.append(headerBuilder.toString())
        packetBuilder.append(String(crlf, charset("UTF-8")))
        packetBuilder.append(String(crlf, charset("UTF-8")))
        packetBuilder.append(bodyBuilder.toString())

        val packetData = packetBuilder.toString()
//        Log.i(Tag,
//            "TC [" + this.ip + ":" + this.port + "] Sending: " + if (packetData.length >= 1024) packetData.substring(
//                0,
//                1024
//            ) + "..." else packetData
//        )

//        networkManager?.sendData(packetData)
        SocketSession.sendData(packetData)
    }

    private fun readNetworkData() {
        Log.e(logcat, "readNetworkData")
        SocketSession.dataSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                it.trim()
            }
            .map {
                if (mListener != null) {
//                    Log.d(logcat, "it : $it")
                    mListener?.onReceviData(it)
                } else {
                    it
                }
            }
            .onErrorReturn {
                Log.e(logcat, "onErrorReturn : ${it.printStackTrace()}")
                readNetworkData()
            }
            .doOnError {
                Log.e(logcat, "doOnError : ${it.printStackTrace()}")
            }
            .subscribe {
//                Log.d(logcat, "mListener : $mListener")
//                if (mListener != null) {
//
//                } else
//                {
//                    Toast.makeText(this, "환영합니다", Toast.LENGTH_SHORT).show()
//                }
            }.let {
                disposables.add(it)
            }
    }

    private fun readConnectState() {
        Log.d(logcat, "readConnectState")
        SocketSession.stateSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                mListener?.onConnected(it)
//                val data = when(it){
//                    true -> R.drawable.main_state_line_back to "연결 성공"
//                    false -> R.drawable.main_state_line_fail_back to "연결 실패"
//                }
//
//                showToast(data.second)
            }.let { disposables.add(it) }
    }

    private fun startForegroundService() {
        Log.d(logcat, "startForegroundService")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val remoteViews = RemoteViews(
            packageName,
            R.layout.notification_service
        )

        val builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= 26) {
            val channelId = "snwodeer_service_channel"
            val channel = NotificationChannel(
                channelId,
                "SnowDeer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this, null.toString())
        }

        builder.setSmallIcon(R.drawable.state_icon)
            .setContent(remoteViews)
            .setContentIntent(pendingIntent)

        startForeground(1, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(logcat, "onDestroy")

        unregisterReceiver(power)
        sensorManager.unregisterListener(this)
        SocketSession.close()
        disposables.clear()
    }
}