package me.gloeckl.fallasleep

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import me.gloeckl.fallasleep.databinding.ActivityMainBinding
import me.gloeckl.fallasleep.SleepTimerTileService
import me.tankery.lib.circularseekbar.CircularSeekBar
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor
    private lateinit var permissionCheckerService: PermissionCheckerService

    private var setTime: Int = 0
    private var originalTime: Int = 0

    private var animatorSet: AnimatorSet = AnimatorSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
        createNotificationChannel()
        setupAnimations()

        val instance = SleepTimerService.getInstance(applicationContext)
        instance.setupOnStartFn { onTimerStart() }
        instance.setupOnStopFn { onTimerStop(it) }
        instance.setupOnTickFn { onTick(it) }

        permissionCheckerService = PermissionCheckerService(this, this)
        permissionCheckerService.checkPermission()

        var sharedPrefs = getSharedPreferences(getString(R.string.shared_prefs_name), Context.MODE_PRIVATE)
        sharedPrefsEditor = sharedPrefs.edit()
        setTime = sharedPrefs.getInt(getString(R.string.shared_prefs_default_time_key), resources.getInteger(R.integer.default_time_minutes))
        originalTime = setTime

        binding.seekBar.progress = setTime.toFloat()
        binding.timeTextView.text = setTime.toString()

        binding.seekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {
                setTime = floor(progress).roundToInt()
                circularSeekBar?.progress = setTime.toFloat()
                binding.timeTextView.text = setTime.toString()
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
                sharedPrefsEditor.putInt(getString(R.string.shared_prefs_default_time_key), setTime).apply()
                TileService.requestListeningState(applicationContext, ComponentName(applicationContext, SleepTimerTileService::class.java))
            }
        })

        binding.viewWrapper.setOnClickListener {
            if (instance.isRunning()) {
                instance.stopSleepTimer()
            } else {
                startSleepTimer()
            }
        }
    }

    private fun startSleepTimer() {
        SleepTimerService.getInstance(applicationContext).startSleepTimer(setTime)
    }

    private fun onTimerStart() {
        binding.seekBar.isEnabled = false
        binding.seekBar.pointerColor = getColor(R.color.invisible)
        animatorSet.start()
    }

    private fun onTick(progress: Long) {
        binding.seekBar.progress = ceil(progress / 60000.0).toFloat()
        binding.timeTextView.text = SleepTimerService.getInstance(applicationContext).getCurrentTimeForFormatting().toString()
    }

    private fun onTimerStop(hasBeenCancelled: Boolean) {
        binding.seekBar.isEnabled = true
        binding.seekBar.progress = originalTime.toFloat()
        binding.timeTextView.text = originalTime.toString()
        animatorSet.cancel()
        resetView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW)
                )
        }
    }

    private fun resetView() {
        binding.viewWrapper.alpha = 1.0f
        binding.seekBar.circleProgressColor = getColor(R.color.salmon)
        binding.seekBar.circleColor = getColor(R.color.light_gray)
        binding.seekBar.pointerColor = getColor(R.color.brickred)
    }

    private fun setupAnimations() {
        val fadingAnimation = ObjectAnimator.ofFloat(binding.viewWrapper, View.ALPHA, 1.0f, 0.4f).apply {
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            duration = 1500
        }

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), getColor(R.color.salmon), getColor(R.color.salmon_complimentary)).apply {
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            duration = 1500
            this.addUpdateListener {
                binding.seekBar.circleProgressColor = it.animatedValue as Int
            }
        }

        val fadeOutAnimation = ValueAnimator.ofObject(ArgbEvaluator(), getColor(R.color.light_gray), getColor(R.color.darkest)).apply {
            duration = 1500
            this.addUpdateListener {
                binding.seekBar.circleColor = it.animatedValue as Int
            }
        }

        animatorSet.playTogether(fadingAnimation, colorAnimation, fadeOutAnimation)
    }
}