package com.fenchtose.movieratings.base

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog

// Not used as of now. We may use it later.
@Suppress("unused")
abstract class BasePermissionFragment: BaseFragment() {

    protected fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    protected fun showRationaleDialog(@StringRes title: Int, @StringRes content: Int, permission: String, code: Int) {
        AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.yes) {
                    dialog, _ ->
                        dialog.dismiss()
                        askPermission(permission, code)
                }
                .setNegativeButton(android.R.string.no) {
                    dialog, _ -> dialog.dismiss()
                }
                .show()
    }

    private fun askPermission(permission: String, code: Int) {
        requestPermissions(arrayOf(permission), code)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onRequestGranted(requestCode)
        }
    }

    abstract fun onRequestGranted(code: Int)
}