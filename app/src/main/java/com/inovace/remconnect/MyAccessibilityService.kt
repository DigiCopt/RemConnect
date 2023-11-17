package com.inovace.remconnect
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {

    private val allowedPackageNames = ArrayList<String>()
    private var launcherPackageName: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.WINDOWS_CHANGE_ACTIVE) {
            val foregroundPackageName = event.packageName.toString()
            Log.d("FG", foregroundPackageName)

            if (MySingleton.getSomeData()) {
                if (!isEssentialApp(foregroundPackageName) && !allowedPackageNames.contains(foregroundPackageName
                    )
                ) {
                    // Check if the current foreground app is the launcher (home) app
                    if (foregroundPackageName != launcherPackageName) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                }
            }
        }
    }

    private fun isEssentialApp(packageName: String): Boolean {
        val dialerAppPackageName = getDialerAppPackageName()

        // Add the package names of other essential, emergency, and system apps here
        val allowedApps = mutableListOf(
            dialerAppPackageName,
            "com.android.settings",
            // Add other essential apps here
            "com.inovace.remconnect",
            // Add other emergency apps here
            "com.example.emergencyapp1"
        )

        // Allow all system apps
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                allowedApps.add(packageInfo.packageName)
            }
        }

        return allowedApps.contains(packageName)
    }

    private fun getDialerAppPackageName(): String {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName ?: ""
    }


    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        setServiceInfo(info)

        // Get the launcher (home) package name
        launcherPackageName = getLauncherPackageName()
    }

    private fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName ?: ""
    }

    private fun updateAllowedPackageNames(packageNames: ArrayList<String>) {
        allowedPackageNames.clear()
        allowedPackageNames.addAll(packageNames)
    }

    private fun updateAllowedPackageNamesFromFCM(packageNamesData: String?) {
        val packageNamesArray =
            packageNamesData!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val packageNamesList = ArrayList(packageNamesArray.toList())
        updateAllowedPackageNames(packageNamesList)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && "UPDATE_PACKAGE_NAMES" == intent.action) {
            val packageNamesData = intent.getStringExtra("package_names")!!
            updateAllowedPackageNamesFromFCM(packageNamesData)
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
