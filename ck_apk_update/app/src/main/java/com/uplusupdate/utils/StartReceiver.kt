package com.uplusupdate.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class StartReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("@@@@@@@@@", "StartReceiver >> onReceive ")
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, AutoUpdateService::class.java))
            } else {
                context.startService(Intent(context, AutoUpdateService::class.java))
            }
        }
    }

}