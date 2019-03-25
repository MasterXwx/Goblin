package com.weex.commen_module.permission

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.NotificationManagerCompat
import android.text.TextUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * Created by xuwx on 2019/3/25.
 */
class PermissionHelper private constructor() {

    companion object {
        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PermissionHelper() }

        const val KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code"
        const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
        const val KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage"
    }

    /**
     * 判断通知是否打开
     *
     * @param activity
     */
    fun checkNotification(activity: FragmentActivity): Boolean {
        val manager = NotificationManagerCompat.from(activity.applicationContext)
        return manager.areNotificationsEnabled()
    }

    /**
     * 检查单项权限
     *
     * @param activity
     * @param permission
     */
    fun checkPermission(activity: Activity, permission: String) {
        val rxPermissions = RxPermissions(activity)
        if (rxPermissions.isGranted(permission) && isMIUISystemAllowed(activity, permission)) {
            if (mListener != null) {
                mListener!!.onPermissionGranted()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {

                    rxPermissions.requestEach(permission)
                            .subscribe { permission ->
                                if (permission.granted && isMIUISystemAllowed(activity, permission.name)) {
                                    if (mListener != null) {
                                        mListener!!.onPermissionGranted()
                                    }
                                } else if (permission.shouldShowRequestPermissionRationale) {
                                    //拒绝但是没有点击不再询问

                                } else {
                                    //拒绝且点击不再询问
                                    showPermissionSuggestionDialog(activity as FragmentActivity)
                                }
                            }
                } else {
                    if (mListener != null) {
                        mListener!!.onPermissionGranted()
                    }
                }
            } else {
                if (mListener != null) {
                    mListener!!.onPermissionGranted()
                }
            }
        }
    }

    /**
     * 判断小米系统是否允许
     *
     * @param name
     * @return
     */
    fun isMIUISystemAllowed(activity: Activity, name: String): Boolean {
        var name = name

        if (!isMIUI()) return true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        name = name.trim { it <= ' ' }
        if (name.contains(",")) {
            val permissionArray = name.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var index = 0
            for (permission in permissionArray) {
                if (doCheckPermission(activity, permission.trim { it <= ' ' })) {
                    index++
                }
            }
            return index == permissionArray.size
        } else {
            return doCheckPermission(activity, name)
        }
    }

    private fun doCheckPermission(activity: Activity, name: String): Boolean {
        var opsPermissionName = ""
        when (name) {
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> opsPermissionName = AppOpsManager.OPSTR_COARSE_LOCATION
            android.Manifest.permission.READ_CALL_LOG -> opsPermissionName = AppOpsManager.OPSTR_READ_CALL_LOG
            android.Manifest.permission.READ_SMS -> opsPermissionName = AppOpsManager.OPSTR_READ_SMS
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> opsPermissionName = AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE
            android.Manifest.permission.READ_EXTERNAL_STORAGE -> opsPermissionName = AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE
            android.Manifest.permission.READ_PHONE_STATE -> opsPermissionName = AppOpsManager.OPSTR_READ_PHONE_STATE
            android.Manifest.permission.READ_CONTACTS -> opsPermissionName = AppOpsManager.OPSTR_READ_CONTACTS
            android.Manifest.permission.CAMERA -> opsPermissionName = AppOpsManager.OPSTR_CAMERA
        }
        val mAppOps = activity.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val value = mAppOps.checkOp(opsPermissionName, Binder.getCallingUid(), activity.applicationContext.packageName)
        return value == AppOpsManager.MODE_ALLOWED
    }


    private fun isMIUI(): Boolean {
        val prop = Properties()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            return (!TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_CODE, ""))
                    || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_NAME, ""))
                    || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_INTERNAL_STORAGE, "")))
        } else {
            try {
                prop.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")))
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }

            return (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null)
        }
    }

    private fun getSystemProperty(key: String, defaultValue: String): String {
        try {
            val clz = Class.forName("android.os.SystemProperties")
            val get = clz.getMethod("get", String::class.java, String::class.java)
            return get.invoke(clz, key, defaultValue) as String
        } catch (e: Exception) {
        }

        return defaultValue
    }

    /**
     * 检查多项权限
     * 所有权限必须都授权
     *
     * @param activity
     * @param permissions
     */
    fun checkPermissions(activity: Activity, vararg permissions: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val rxPermissions = RxPermissions(activity)
            rxPermissions.requestEachCombined(*permissions).subscribe { permission ->
                if (permission.granted && isMIUISystemAllowed(activity, permission.name)) {
                    if (mListener != null) {
                        mListener!!.onPermissionGranted()
                    }
                } else if (permission.shouldShowRequestPermissionRationale) {

                } else {
                    showPermissionSuggestionDialog(activity as FragmentActivity)
                }
            }
        } else {
            if (mListener != null) {
                mListener!!.onPermissionGranted()
            }
        }
    }

    private fun showPermissionSuggestionDialog(fragmentActivity: FragmentActivity) {


    }

    private var mListener: OnPermissionListener? = null

    fun setListener(listener: OnPermissionListener) {
        if (mListener != null) {
            mListener = null
        }
        this.mListener = listener
    }

    interface OnPermissionListener {
        fun onPermissionGranted()
    }

}