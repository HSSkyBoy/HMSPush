package one.yufz.hmspush.hook.system

import android.app.AndroidAppHelper
import android.app.Notification
import android.os.Binder
import android.os.Build
import android.os.Process
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.findMethodExact
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.HookCallback
import one.yufz.xposed.HookContext
import one.yufz.xposed.hook
import one.yufz.xposed.hookMethod

object NmsPermissionHooker {
    private const val TAG = "NmsPermissionHooker"

    private fun fromHms(): Boolean = runCatching {
        Binder.getCallingUid() == getPackageUid(HMS_PACKAGE_NAME)
    }.getOrDefault(false)

    private fun getPackageUid(packageName: String): Int =
        AndroidAppHelper.currentApplication().packageManager.getPackageUid(packageName, 0)

    private fun tryHookPermission(packageName: String): Boolean {
        return if (packageName != HMS_PACKAGE_NAME && fromHms()) {
            Binder.clearCallingIdentity()
            true
        } else false
    }

    private fun hookPermission(targetPackageNameParamIndex: Int = 0, hookExtra: (HookContext.() -> Unit)? = null): HookCallback = {
        doBefore {
            if (tryHookPermission(args[targetPackageNameParamIndex] as String)) {
                hookExtra?.invoke(this)
            }
        }
    }

    fun hook(classINotificationManager: Class<*>) {
        // 提取共用的預設 hook
        val hookDefault = hookPermission()

        findMethodExact(classINotificationManager, "areNotificationsEnabledForPackage", String::class.java, Int::class.java).hook(hookDefault)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        //NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, String conversationId, boolean includeDeleted);
            findMethodExact(classINotificationManager, "getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java).hook(hookDefault)
        } else {
            //NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, boolean includeDeleted);
            findMethodExact(classINotificationManager, "getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, Boolean::class.java).hook(hookDefault)
        }

        //void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int userId)
        findMethodExact(classINotificationManager, "enqueueNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Notification::class.java, Int::class.java)
            .hook(hookPermission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) args[1] = ANDROID_PACKAGE_NAME
            })

        findMethodExact(classINotificationManager, "createNotificationChannelsForPackage", String::class.java, Int::class.java, findClass("android.content.pm.ParceledListSlice", null)).hook(hookDefault)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            findMethodExact(classINotificationManager, "cancelNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Int::class.java)
                .hook(hookPermission { args[1] = ANDROID_PACKAGE_NAME })
        } else {
            findMethodExact(classINotificationManager, "cancelNotificationWithTag", String::class.java, String::class.java, Int::class.java, Int::class.java).hook(hookDefault)
        }

        findMethodExact(classINotificationManager, "deleteNotificationChannel", String::class.java, String::class.java).hook(hookDefault)
        findMethodExact(classINotificationManager, "getAppActiveNotifications", String::class.java, Int::class.java).hook(hookDefault)
        findMethodExact(classINotificationManager, "getNotificationChannelsForPackage", String::class.java, Int::class.java, Boolean::class.java).hook(hookDefault)
        hookDeleteNotificationChannel(classINotificationManager)
    }

    private fun hookDeleteNotificationChannel(classINotificationManager: Class<*>) {
        val classLoader = classINotificationManager.classLoader
        val deleteNotificationChannelHook: HookContext.() -> Unit = {
            doBefore {
                val packageName = args[0] as String
                if (packageName != HMS_PACKAGE_NAME && Binder.getCallingUid() == Process.SYSTEM_UID) {
                    args[1] = getPackageUid(packageName)
                }
            }
        }
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                runCatching {
                    findClass("com.android.server.notification.PreferencesHelper", classLoader)
                    //public boolean deleteNotificationChannel(String pkg, int uid, String channelId, int callingUid, boolean fromSystemOrSystemUi)
                        .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, Int::class.java, Boolean::class.java, callback = deleteNotificationChannelHook)
                }.onFailure {
                //Samsung One UI 7 delete this method
                 XLog.d(TAG, "hook deleteNotificationChannel error, NoSuchMethodError") }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                findClass("com.android.server.notification.PreferencesHelper", classLoader)
                //public boolean deleteNotificationChannel(String pkg, int uid, String channelId)
                    .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, callback = deleteNotificationChannelHook)
            }
            else -> {
                findClass("com.android.server.notification.RankingHelper", classLoader)
                    .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, callback = deleteNotificationChannelHook)
            }
        }
    }
}
