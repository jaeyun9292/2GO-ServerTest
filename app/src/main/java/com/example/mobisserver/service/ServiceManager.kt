package com.example.mobisserver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.mobisserver.R
import com.example.mobisserver.activity.MainActivity
import com.example.mobisserver.net.SocketSession
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.apache.commons.lang3.StringUtils

class ServiceManager : Service() {
    private val logcat = "ServiceManager"
    private val disposables by lazy { CompositeDisposable() }
    private val crlf = "\r\n".toByteArray()
    private var ip = ""
    private var port = 0
    private val mBinder: IBinder = LocalBinder()

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
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand: ")
        val action = intent?.action
        Log.d(logcat, "onStartCommand : $action")
        when (action) {
            "Start" -> {
                onTcpConnect()
            }
        }

        return START_STICKY
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

        SocketSession.close()
        disposables.clear()
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
        ip = MainActivity.prefs.getString("ip", "192.168.11.24")
        port = MainActivity.prefs.getString("port", "8181").toInt()
        Log.d(logcat, "onTcpConnect ip: " + ip + " port: " + port)

        if (SocketSession.socket == null) {
            Log.d(logcat, "Enter")
            SocketSession.dataInit()
            SocketSession.createSocket(ip, port)

            readNetworkData()
            readConnectState()
        }
    }

    fun sendMessage(header: String, body: String) {
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
            .subscribe {}.let {
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
            }.let { disposables.add(it) }
    }
}