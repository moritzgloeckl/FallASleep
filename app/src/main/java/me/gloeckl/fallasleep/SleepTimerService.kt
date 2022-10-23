package me.gloeckl.fallasleep

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.CountDownTimer
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.roundToLong

class SleepTimerService(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat
    ) {

    private var countDownTimer: CountDownTimer? = null

    private var originalSleepTimeMillis: Long = 0
    private var currentSleepTimeMillis: Long = 0
    private var extensionTimeMillis: Long = 0

    private var permissionCheckerService: PermissionCheckerService = PermissionCheckerService(context)

    private lateinit var onStartFn: () -> Unit
    private lateinit var onStopFn: (b: Boolean) -> Unit
    private lateinit var onTickFn: (n: Long) -> Unit

    private val pendingIntent = TaskStackBuilder.create(context).run {
        var intent = Intent(context, MainActivity::class.java)
        intent.flags = (FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addNextIntentWithParentStack(intent)
        getPendingIntent(0, 0)
    }
    private val extendBroadcastIntent: Intent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(context.getString(R.string.action_extra_id), NotificationActions.EXTEND)
    }
    private val resetBroadcastIntent: Intent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(context.getString(R.string.action_extra_id), NotificationActions.RESET)
    }
    private val stopBroadcastIntent: Intent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(context.getString(R.string.action_extra_id), NotificationActions.STOP)
    }

    private val sleepTimerNotification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
        .setContentTitle(context.getString(R.string.notification_content_title))
        .setSmallIcon(R.drawable.ic_quick_tile)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setAutoCancel(false)

    public fun startSleepTimer(sleepInMinutes: Int) {
        if (countDownTimer != null) return

        originalSleepTimeMillis = (sleepInMinutes * 60000).toLong()
        currentSleepTimeMillis = originalSleepTimeMillis

        extensionTimeMillis = if (sleepInMinutes <= 3) {
            60000
        } else {
            floor(originalSleepTimeMillis / 3.0).roundToLong()
        }

        sleepTimerNotification.clearActions()
        sleepTimerNotification
            .addAction(R.drawable.ic_quick_tile, context.getString(R.string.stop_action_title), PendingIntent.getBroadcast(context, 1, stopBroadcastIntent, PendingIntent.FLAG_IMMUTABLE))
            .addAction(R.drawable.ic_quick_tile, String.format(context.getString(R.string.extend_action_title), (extensionTimeMillis / 60000.0).toInt()), PendingIntent.getBroadcast(context, 2, extendBroadcastIntent, PendingIntent.FLAG_IMMUTABLE))
            .addAction(R.drawable.ic_quick_tile, context.getString(R.string.reset_action_title), PendingIntent.getBroadcast(context, 3, resetBroadcastIntent, PendingIntent.FLAG_IMMUTABLE))

        createTimer(originalSleepTimeMillis)
    }

    public fun isRunning(): Boolean {
        return countDownTimer != null
    }

    public fun resetTimer() {
        createTimer(originalSleepTimeMillis)
    }

    public fun getRemainingTime(): Long {
        return currentSleepTimeMillis
    }

    public fun extendTimer() {
        var newSleepTime = (getCurrentTimeForFormatting() + (extensionTimeMillis / 60000)) * 60000

        // Max 60 minutes
        if (newSleepTime > (60 * 60000)) {
            newSleepTime = 60 * 60000
        }

        createTimer(newSleepTime)
    }

    public fun stopSleepTimer() {
        countDownTimer?.cancel()
        notificationManager.cancel(context.resources.getInteger(R.integer.notification_id))
        countDownTimer = null;

        TileService.requestListeningState(context, ComponentName(context, SleepTimerTileService::class.java))

        if (::onStopFn.isInitialized) onStopFn(true)
    }

    public fun setupOnStartFn(fn: () -> Unit) {
        onStartFn = fn

        if (this.isRunning() && ::onStartFn.isInitialized) {
            onStartFn()
        }
    }

    public fun setupOnTickFn(fn: (n: Long) -> Unit) {
        onTickFn = fn
    }

    public fun setupOnStopFn(fn: (b: Boolean) -> Unit) {
        onStopFn = fn
    }

    public fun getCurrentTimeForFormatting(): Int {
        return (currentSleepTimeMillis.toFloat() / 60000f).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
    }

    private fun createTimer(sleepTimeMillis: Long) {
        if (countDownTimer != null) countDownTimer!!.cancel()

        countDownTimer = object : CountDownTimer(sleepTimeMillis, 60000) {

            override fun onTick(millisUntilFinished: Long) {
                currentSleepTimeMillis = millisUntilFinished

                if (millisUntilFinished > 60000) {
                    sleepTimerNotification.setContentText(String.format(context.getString(R.string.notification_content_text_min), getCurrentTimeForFormatting()))
                } else {
                    sleepTimerNotification.setContentText(context.getString(R.string.notification_content_text_sec))
                }
                notificationManager.notify(context.resources.getInteger(R.integer.notification_id), sleepTimerNotification.build())

                TileService.requestListeningState(context, ComponentName(context, SleepTimerTileService::class.java))

                if (::onTickFn.isInitialized) onTickFn(currentSleepTimeMillis)
            }

            override fun onFinish() {
                notificationManager.cancel(context.resources.getInteger(R.integer.notification_id))
                countDownTimer = null

                TileService.requestListeningState(context, ComponentName(context, SleepTimerTileService::class.java))

                if (::onStopFn.isInitialized) onStopFn(false)

                if (permissionCheckerService.hasPermission()) permissionCheckerService.devicePolicyManager.lockNow()
            }
        }.start()
        if (::onStartFn.isInitialized) onStartFn()
    }

    companion object {
        @Volatile private var INSTANCE: SleepTimerService? = null

        public fun getInstance(context: Context): SleepTimerService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SleepTimerService(context, NotificationManagerCompat.from(context)).also { INSTANCE = it }
            }
    }
}