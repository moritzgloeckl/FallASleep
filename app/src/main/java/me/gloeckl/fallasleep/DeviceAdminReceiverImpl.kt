package me.gloeckl.fallasleep

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class DeviceAdminReceiverImpl : DeviceAdminReceiver()  {
    override fun onEnabled(context: Context, intent: Intent) {
    }

    override fun onDisabled(context: Context, intent: Intent) {
        val instance = SleepTimerService.getInstance(context)
        if (instance.isRunning()) {
            SleepTimerService.getInstance(context).stopSleepTimer()
        }
    }
}