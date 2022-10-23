package me.gloeckl.fallasleep

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import java.util.logging.Logger

@RequiresApi(Build.VERSION_CODES.N)
class SleepTimerTileService : TileService() {

    private fun setState() {
        val instance = SleepTimerService.getInstance(applicationContext)
        val defaultTime = getSharedPreferences(getString(R.string.shared_prefs_name), Context.MODE_PRIVATE).getInt(getString(R.string.shared_prefs_default_time_key), resources.getInteger(R.integer.default_time_minutes))

        if (instance.isRunning()) {
            qsTile.state = Tile.STATE_ACTIVE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                qsTile.label = getString(R.string.quick_tile_stop)
                qsTile.subtitle = String.format(getString(R.string.quick_tile_start_subtitle), instance.getCurrentTimeForFormatting())
            } else {
                qsTile.label = String.format(getString(R.string.quick_tile_start_subtitle), instance.getCurrentTimeForFormatting())
            }
        } else {
            qsTile.state = Tile.STATE_INACTIVE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                qsTile.label = getString(R.string.quick_tile_start_default)
                qsTile.subtitle = String.format(getString(R.string.quick_tile_start_subtitle), defaultTime)
            } else {
                qsTile.label = String.format(getString(R.string.quick_tile_start_subtitle), defaultTime)
            }
        }

        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        this.setState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        this.setState()
    }

    override fun onClick() {
        super.onClick()

        val instance = SleepTimerService.getInstance(applicationContext)
        val defaultTime = getSharedPreferences(getString(R.string.shared_prefs_name), Context.MODE_PRIVATE).getInt(getString(R.string.shared_prefs_default_time_key), resources.getInteger(R.integer.default_time_minutes))

        if (instance.isRunning()) {
            instance.stopSleepTimer()
        } else {
            instance.startSleepTimer(defaultTime)
        }

        this.setState()
    }

}