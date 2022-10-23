package me.gloeckl.fallasleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.getSerializableExtra(context!!.getString(R.string.action_extra_id)) as NotificationActions?) {
            NotificationActions.STOP -> {
                SleepTimerService.getInstance(context).stopSleepTimer()
            }
            NotificationActions.RESET -> {
                SleepTimerService.getInstance(context).resetTimer()
            }
            NotificationActions.EXTEND -> {
                SleepTimerService.getInstance(context).extendTimer()
            }
            else -> {}
        }
    }

}