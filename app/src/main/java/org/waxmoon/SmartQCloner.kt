package org.waxmoon

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.hack.opensdk.HackApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

        try {
            context.packageManager.getApplicationInfo(SMARTQ_PACKAGE, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "SmartQ not installed on host", e)
            return CloneResult(false, message = "SmartQ ($SMARTQ_PACKAGE) is not installed on this device.")
        }

        return try {
            val alreadyInstalled = isInstalledInVirtualSpace(CLONE_USER_ID)
            Log.i(TAG, "virtual alreadyInstalled=$alreadyInstalled userId=$CLONE_USER_ID")

            val installCode = if (alreadyInstalled) {
                Log.i(TAG, "skipping staging/install; clone already present in virtual space")
                INSTALL_SUCCEEDED
            } else {
                installCloneFromHostApks(context)
            }

            if (installCode != INSTALL_SUCCEEDED && installCode != INSTALL_FAILED_ALREADY_EXISTS) {
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    message = "Install failed with code $installCode",
                )
            }

            launchVirtualClone(installCode)
        } catch (e: Exception) {
            Log.e(TAG, "cloneAndLaunch failed", e)
            CloneResult(success = false, message = e.message ?: e.javaClass.simpleName)
        }
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

    private fun installCloneFromHostApks(context: Context): Int {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(SMARTQ_PACKAGE, 0)
        val baseApk = appInfo.sourceDir
        val splitApks = appInfo.splitSourceDirs?.toList().orEmpty()

        Log.i(TAG, "packageName=$SMARTQ_PACKAGE")
        Log.i(TAG, "sourceDir=$baseApk")
        Log.i(TAG, "splitSourceDirs=${splitApks.joinToString()}")

        if (splitApks.isEmpty()) {
            Log.w(TAG, "No split APKs reported; proceeding with base APK only")
        }

        val stagingDir = File(context.cacheDir, "shadow_clone/$SMARTQ_PACKAGE")
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
        }
        stagingDir.mkdirs()
        Log.i(TAG, "stagingDir=${stagingDir.absolutePath}")

        copyApk(File(baseApk), File(stagingDir, File(baseApk).name))
        for (split in splitApks) {
            copyApk(File(split), File(stagingDir, File(split).name))
        }

        val stagedFiles = stagingDir.listFiles()?.filter { it.extension == "apk" }.orEmpty()
        Log.i(TAG, "staged apk count=${stagedFiles.size} files=${stagedFiles.map { it.name }}")

        Log.i(TAG, "calling installApkFiles dir=${stagingDir.absolutePath} userId=$CLONE_USER_ID forceInstall=true")
        val installCode = HackApi.installApkFiles(stagingDir.absolutePath, CLONE_USER_ID, true)
        Log.i(TAG, "installApkFiles result=$installCode")
        return installCode
    }

    /**
     * Launch ONLY through the engine virtual PM — never the host PackageManager.
     * Host intents carry the real component and make startActivity return -92.
     */
    private fun launchVirtualClone(installCode: Int): CloneResult {
        val launchIntent = resolveVirtualLaunchIntent(CLONE_USER_ID)
        if (launchIntent != null) {
            Log.i(TAG, "calling HackApi.startActivity intent=$launchIntent userId=$CLONE_USER_ID")
            val launchCode = HackApi.startActivity(launchIntent, CLONE_USER_ID)
            Log.i(TAG, "HackApi.startActivity result=$launchCode")
            if (launchCode == START_SUCCESS) {
                Log.i(TAG, "cloneAndLaunch success install=$installCode launch=$launchCode method=startActivity")
                return CloneResult(
                    success = true,
                    installCode = installCode,
                    launchCode = launchCode,
                    message = "SmartQ cloned and launched (install=$installCode launch=$launchCode)",
                )
            }
        } else {
            Log.w(TAG, "no virtual launch intent resolved; falling back to HackApi.startPackage")
        }

        Log.i(TAG, "calling HackApi.startPackage package=$SMARTQ_PACKAGE userId=$CLONE_USER_ID")
        val started = HackApi.startPackage(SMARTQ_PACKAGE, CLONE_USER_ID)
        Log.i(TAG, "HackApi.startPackage result=$started")
        if (started) {
            Log.i(TAG, "cloneAndLaunch success install=$installCode method=startPackage")
            return CloneResult(
                success = true,
                installCode = installCode,
                launchCode = null,
                message = "SmartQ launched via startPackage (install=$installCode)",
            )
        }

        return CloneResult(
            success = false,
            installCode = installCode,
            launchCode = launchIntent?.let { -1 },
            message = "Install OK but virtual launch failed (startActivity/startPackage)",
        )
    }

    private fun resolveVirtualLaunchIntent(userId: Int): Intent? {
        repeat(5) { attempt ->
            val engineIntent = HackApi.getLaunchIntentForPackage(SMARTQ_PACKAGE, userId)
            Log.i(
                TAG,
                "HackApi.getLaunchIntentForPackage attempt=${attempt + 1} userId=$userId -> $engineIntent",
            )
            if (engineIntent != null) {
                return engineIntent
            }

            val query = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(SMARTQ_PACKAGE)
            }
            val resolves = HackApi.queryIntentActivities(query, null, 0, userId)
            Log.i(
                TAG,
                "HackApi.queryIntentActivities attempt=${attempt + 1} userId=$userId count=${resolves?.size}",
            )
            if (!resolves.isNullOrEmpty()) {
                val activity = resolves[0].activityInfo
                val built = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(activity.packageName, activity.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                Log.i(TAG, "built virtual launch intent from query -> $built")
                return built
            }

            if (attempt < 4) {
                Thread.sleep(300)
            }
        }
        return null
    }

    private fun copyApk(source: File, dest: File) {
        Log.i(TAG, "copy ${source.absolutePath} -> ${dest.absolutePath}")
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        Log.i(TAG, "copied ${dest.length()} bytes to ${dest.name}")
    }
}
