package me.gloeckl.fallasleep

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat


class PermissionCheckerService(private val context: Context, private val activity: MainActivity? = null) {

    val devicePolicyManager: DevicePolicyManager = context.getSystemService(AppCompatActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        get() = field

    private val componentName = ComponentName(context, DeviceAdminReceiverImpl::class.java)
    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    private val startForResult = activity?.registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                this.checkPermission()
            }
        }

    public fun hasPermission(): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }

    public fun checkPermission() {
        // If there is no activity, we cannot show the alert dialog...
        if (activity == null || startForResult == null) return

        if (!devicePolicyManager.isAdminActive(componentName)) {
            builder
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_text)
                .setCancelable(false)
                .setPositiveButton(R.string.permission_dialog_continue
                ) { _, _ ->
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    intent.putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.permission_dialog_device_admin)
                    )

                    startForResult.launch(intent)
                }
                .setNegativeButton(R.string.permission_dialog_exit) {
                    _, _ -> activity.finish()
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
}