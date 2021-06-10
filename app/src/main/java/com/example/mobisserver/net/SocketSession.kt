package com.example.mobisserver.net

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit

object SocketSession {
    var socket: Socket? = null
    const val Tag = "SocketSession"
    private var isHeaderReceive = true
    private val headerBuff: ArrayList<Byte> = ArrayList()
    private var CRLF = "\r\n".toByteArray()
    private var isStop = false

    var dataSubject: PublishSubject<String>
    var stateSubject: BehaviorSubject<Boolean>

    private val disposables by lazy { CompositeDisposable() }

    init {
        Log.d(Tag, "init")
        dataSubject = PublishSubject.create()
        stateSubject = BehaviorSubject.createDefault(false)
    }

    fun dataInit() {
        Log.d(Tag, "dataInit")
        dataSubject = PublishSubject.create()
        stateSubject = BehaviorSubject.createDefault(false)
    }

    fun createSocket(ip: String, port: Int = 3000) {
        Log.d(Tag, "createSocket")
        Thread {
            try {
                socket = Socket(ip, port)
                isStop = false
                readData()
                true
            } catch (e: IOException) {
                false
            }.let {
                stateSubject.onNext(it)
            }
        }.start()
    }

    private fun readData() {
        Observable.interval(10, TimeUnit.MILLISECONDS)
            .repeat()
            .subscribeOn(Schedulers.io())
            .map { socket!! }
            .filter { it.isConnected }
            .map { it.getInputStream() }
            .filter { it.available() > 0 }
            .map {
                var data = ""
                for(i in 1..it.available()) {
                    if (isHeaderReceive) {
                        val msgReceiveHeaderBuff = ByteArray(1)
                        val bytesReceive: Int = it.read(msgReceiveHeaderBuff)
                        if (bytesReceive <= 0) {
                            throw Exception("Disconnected from host")
                        }

                        headerBuff.add(msgReceiveHeaderBuff[0])

                        if (headerBuff.size >= 4) {
                            if (headerBuff[headerBuff.size - 1] == CRLF[1] && headerBuff[headerBuff.size - 2] == CRLF[0]
                                && headerBuff[headerBuff.size - 3] == CRLF[1] && headerBuff[headerBuff.size - 4] == CRLF[0]
                            ) {
                                //Console.WriteLine("OK Head end");
                                isHeaderReceive = false
                                headerBuff.removeAt(headerBuff.size - 4)
                                headerBuff.removeAt(headerBuff.size - 3)
                                headerBuff.removeAt(headerBuff.size - 2)
                                headerBuff.removeAt(headerBuff.size - 1)
                            }
                        }
                    } else {
                        val header = ByteArray(headerBuff.size)

                        for (j in headerBuff.indices) {
                            header[j] = headerBuff[j]
                        }

                        val utfHeaderString = String(header)
                        data = utfHeaderString
                        val uri =
                            Uri.parse("http://localhost/?$utfHeaderString")
                        val contentLength = uri.getQueryParameter("content-length")!!.toInt()

                        val msgRecvBodyBuff = ByteArray(contentLength)
                        var copyIndex = 0

                        if (contentLength > 0)
                        {
                            val recvBuffer = ByteArray(if (contentLength >= 8192) 8192 else contentLength)
                            var readLength: Int = recvBuffer.size
                            while (copyIndex < contentLength)
                            {
                                val bytesRecv: Int = it.read(recvBuffer, 0, readLength)
                                if (bytesRecv < 0) {
                                    throw java.lang.Exception("Disconnected from host")
                                }

                                System.arraycopy(
                                    recvBuffer,
                                    0,
                                    msgRecvBodyBuff,
                                    copyIndex,
                                    bytesRecv
                                )

                                copyIndex += bytesRecv

                                val remainLength = contentLength - copyIndex
                                if (remainLength < recvBuffer.size) {
                                    readLength = remainLength
                                }
                            }
                        }

                        this.initBuffer()
                        data = data + "§" + String(msgRecvBodyBuff)

                        return@map data
                    }
                }

                data
            }.subscribe{
                dataSubject.onNext(it)
            }.let { disposables.add(it) }
    }

    private fun initBuffer() {
        isHeaderReceive = true
        headerBuff.clear()
    }

    @SuppressLint("CheckResult")
    fun sendData(msg: String) {
//        Log.d(Tag, "sendData : $msg")
//        if(msg.contains("api-command=tuio"))  {
//            Log.d(Tag, "sendData : $msg")
//        }
        Observable.just(msg)
            .subscribeOn(Schedulers.io())
            .filter { socket != null }
            .subscribe {
                socket!!.getOutputStream().write(msg.toByteArray())
            }
    }

    fun close() {
        Log.d(Tag, "close")
        socket?.let {
            it.close()
            disposables.clear()
            stateSubject.onNext(false)
            stateSubject.onComplete()
            dataSubject.onComplete()
            isStop = true
            socket = null
        }
    }
}