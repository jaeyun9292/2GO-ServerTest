package com.example.mobisserver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mobisserver.MobisApplication
import com.example.mobisserver.activity.MainActivity

class PowerBroadcastReceiver : BroadcastReceiver() {
    private val logTag = "PowerBroadcastReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when {
            action.equals(Intent.ACTION_BATTERY_CHANGED) -> {
//                Log.d(logTag, "ACTION_BATTERY_CHANGED")
            }
            action.equals(Intent.ACTION_BATTERY_LOW) -> {
//                Log.d(logTag, "ACTION_BATTERY_LOW")
            }
            action.equals(Intent.ACTION_BATTERY_OKAY) -> {
//                Log.d(logTag, "ACTION_BATTERY_OKAY")
            }
            action.equals(Intent.ACTION_POWER_CONNECTED) -> {
                //            Toast.makeText(context, "ACTION_POWER_CONNECTED", Toast.LENGTH_SHORT).show()
                //            val mobis = MobilsApplication.applicationContext()
                //            MobilsApplication().startMain(mobis)
                Log.d(logTag, "ACTION_POWER_CONNECTED")
                if (!MobisApplication.isAppStart) {
                    MobisApplication.isAppStart = true
                    MobisApplication.powerCheck = false
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    i.action = "Start"
                    context.startActivity(i)
                }

                //            val pIntent = PendingIntent.getActivity(
                //                context, 0,
                //                i, PendingIntent.FLAG_UPDATE_CURRENT
                //            )
                //
                //            var not = getNotificationBuilder(context,"channel1", "1st channel")
                //            not.setContentTitle("Content Title")
                //            not.setPriority(PRIORITY_HIGH)
                //            not.setFullScreenIntent(pIntent, true)
                //            not.setAutoCancel(true)
                //
                //
                //            var notification = not.build()
                //
                //            var mng = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                //            mng.notify(10, notification)

            }
            action.equals(Intent.ACTION_POWER_DISCONNECTED) -> {
                Log.d(logTag, "ACTION_POWER_DISCONNECTED")
//                val setting = Intent(context, SettingActivity::class.java)
//                setting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                setting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                setting.action = "End"
//                context.startActivity(setting)

                if (MobisApplication.isAppStart) {
//                    Log.d("PowerBroadcastReceiver", "Enter4")
//                    MobisApplication.isAppStart = false
                    MobisApplication.powerCheck = true
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    i.action = "End"
                    context.startActivity(i)
                }
            }
        }
    }

//    fun getNotificationBuilder(context: Context ,id:String, name:String) : NotificationCompat.Builder {
//        var not : NotificationCompat.Builder? = null
//
//        var manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        var channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
//        manager.createNotificationChannel(channel)
//
//        not = NotificationCompat.Builder(context, id)
//
//        return not
//    }

}