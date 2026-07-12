package org.waxmoon

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.hack.opensdk.BuildConfig
import com.hack.opensdk.HackApi

object SmartQCloner {

    const val TAG = "ShadowClone"
    const val SMARTQ_PACKAGE = "com.thesmartq.smartq"
    const val CLONE_USER_ID = 0
    const val INSTALL_SUCCEEDED = 1
    const val INSTALL_FAILED_ALREADY_EXISTS = -1
    const val START_SUCCESS = 0

    data class CloneResult(
        val success: Boolean,
        val installCode: Int? = null,
        val launchCode: Int? = null,
        val message: String,
    )

    fun cloneAndLaunch(context: Context): CloneResult {
        Log.i(TAG, "cloneAndLaunch start package=$SMARTQ_PACKAGE userId=$CLONE_USER_ID")
        logHostIdentity()

        val pm = context.packageManager
        val appInfo = try {
            pm.getApplicationInfo(SMARTQ_PACKAGE, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "SmartQ not installed on host", e)
            return CloneResult(false, message = "SmartQ ($SMARTQ_PACKAGE) is not installed on this device.")
        }

        logHostSplitApks(appInfo)

        return try {
            val alreadyInstalled = isInstalledInVirtualSpace(CLONE_USER_ID)
            Log.i(TAG, "virtual alreadyInstalled=$alreadyInstalled userId=$CLONE_USER_ID")

            val installCode = ensureVirtualInstall(alreadyInstalled)
            if (installCode != INSTALL_SUCCEEDED && installCode != INSTALL_FAILED_ALREADY_EXISTS) {
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    message = "installPackageFromHost failed with code $installCode",
                )
            }

            var launchCode = launchLikeSampleApp()
            if (launchCode != START_SUCCESS && alreadyInstalled) {
                Log.w(
                    TAG,
                    "launch failed on existing clone; relinking via installPackageFromHost(force=true)",
                )
                val relink = HackApi.installPackageFromHost(SMARTQ_PACKAGE, CLONE_USER_ID, true)
                Log.i(TAG, "HackApi.installPackageFromHost relink result=$relink")
                launchCode = launchLikeSampleApp()
            }

            if (launchCode != START_SUCCESS) {
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    launchCode = launchCode,
                    message = "Install OK but HackApi.startActivity failed with code $launchCode",
                )
            }

            Log.i(TAG, "cloneAndLaunch success install=$installCode launch=$launchCode")
            CloneResult(
                success = true,
                installCode = installCode,
                launchCode = launchCode,
                message = "SmartQ cloned and launched (install=$installCode launch=$launchCode)",
            )
        } catch (e: Exception) {
            Log.e(TAG, "cloneAndLaunch failed", e)
            CloneResult(success = false, message = e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Mirrors MultiApp sample [AppListActivity.startApp] for host-installed apps:
     * PackageManager.getLaunchIntentForPackage + HackApi.startActivity(intent, userId).
     */
    private fun launchLikeSampleApp(): Int {
        val intent = MoonApplication.INSTANCE().packageManager
            .getLaunchIntentForPackage(SMARTQ_PACKAGE)
        intent?.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        Log.i(TAG, "PackageManager.getLaunchIntentForPackage -> $intent")
        if (intent == null) {
            Log.e(TAG, "PackageManager.getLaunchIntentForPackage returned null")
            return -1
        }
        Log.i(TAG, "calling HackApi.startActivity(intent, userId=$CLONE_USER_ID)")
        val launchCode = HackApi.startActivity(intent, CLONE_USER_ID)
        Log.i(TAG, "HackApi.startActivity result=$launchCode")
        return launchCode
    }

    /**
     * Mirrors MultiApp sample [AppListActivity.install] for system-installed apps.
     * installPackageFromHost reads base + splits from the host — no 166MB staging copy.
     */
    private fun ensureVirtualInstall(alreadyInstalled: Boolean): Int {
        if (alreadyInstalled) {
            Log.i(TAG, "skipping installPackageFromHost; clone already in virtual space")
            return INSTALL_SUCCEEDED
        }
        Log.i(
            TAG,
            "calling HackApi.installPackageFromHost($SMARTQ_PACKAGE, userId=$CLONE_USER_ID, forceInstall=false)",
        )
        val installCode = HackApi.installPackageFromHost(SMARTQ_PACKAGE, CLONE_USER_ID, false)
        Log.i(TAG, "HackApi.installPackageFromHost result=$installCode")
        return installCode
    }

    private fun isInstalledInVirtualSpace(userId: Int): Boolean {
        val installed = HackApi.getInstalledPackages(0, userId)
        Log.i(TAG, "HackApi.getInstalledPackages userId=$userId -> $installed")
        if (installed?.contains(SMARTQ_PACKAGE) == true) {
            return true
        }
        val pkgInfo = HackApi.getPackageInfo(SMARTQ_PACKAGE, userId, 0)
        Log.i(TAG, "HackApi.getPackageInfo userId=$userId -> $pkgInfo")
        return pkgInfo != null
    }

    private fun logHostIdentity() {
        Log.i(TAG, "BuildConfig.MASTER_PACKAGE=${BuildConfig.MASTER_PACKAGE}")
        Log.i(TAG, "BuildConfig.ASSIST_PACKAGE=${BuildConfig.ASSIST_PACKAGE}")
        Log.i(TAG, "host applicationId=${MoonApplication.INSTANCE().packageName}")
        val props = HackApi.getRuntimeProperties()
        Log.i(TAG, "HackApi.getRuntimeProperties -> $props")
    }

    private fun logHostSplitApks(appInfo: android.content.pm.ApplicationInfo) {
        val baseApk = appInfo.sourceDir
        val splitApks = appInfo.splitSourceDirs?.toList().orEmpty()
        Log.i(TAG, "host packageName=$SMARTQ_PACKAGE")
        Log.i(TAG, "host sourceDir=$baseApk")
        Log.i(TAG, "host splitSourceDirs=${splitApks.joinToString()}")
        if (splitApks.isEmpty()) {
            Log.w(TAG, "host reports no split APKs")
        }
    }
}
