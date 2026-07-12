package org.waxmoon

import android.content.Context
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
    const val START_SUCCESS = 0

    data class CloneResult(
        val success: Boolean,
        val installCode: Int? = null,
        val launchCode: Int? = null,
        val message: String,
    )

    fun cloneAndLaunch(context: Context): CloneResult {
        Log.i(TAG, "cloneAndLaunch start package=$SMARTQ_PACKAGE userId=$CLONE_USER_ID")

        val pm = context.packageManager
        val appInfo = try {
            pm.getApplicationInfo(SMARTQ_PACKAGE, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "SmartQ not installed on host", e)
            return CloneResult(false, message = "SmartQ ($SMARTQ_PACKAGE) is not installed on this device.")
        }

        val baseApk = appInfo.sourceDir
        val splitApks = appInfo.splitSourceDirs?.toList().orEmpty()
        Log.i(TAG, "packageName=$SMARTQ_PACKAGE")
        Log.i(TAG, "sourceDir=$baseApk")
        Log.i(TAG, "splitSourceDirs=${splitApks.joinToString()}")

        if (splitApks.isEmpty()) {
            Log.w(TAG, "No split APKs reported; proceeding with base APK only")
        }

        val stagingDir = File(context.cacheDir, "shadow_clone/$SMARTQ_PACKAGE")
        try {
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

            if (installCode != INSTALL_SUCCEEDED) {
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    message = "Install failed with code $installCode",
                )
            }

            val launchIntent = HackApi.getLaunchIntentForPackage(SMARTQ_PACKAGE, CLONE_USER_ID)
            if (launchIntent == null) {
                Log.e(TAG, "getLaunchIntentForPackage returned null for userId=$CLONE_USER_ID")
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    message = "Install succeeded but no launch intent found for clone userId=$CLONE_USER_ID",
                )
            }

            Log.i(TAG, "launchIntent=$launchIntent")
            val launchCode = HackApi.startActivity(launchIntent, CLONE_USER_ID)
            Log.i(TAG, "startActivity result=$launchCode")

            if (launchCode != START_SUCCESS) {
                return CloneResult(
                    success = false,
                    installCode = installCode,
                    launchCode = launchCode,
                    message = "Install OK but launch failed with code $launchCode",
                )
            }

            Log.i(TAG, "cloneAndLaunch success install=$installCode launch=$launchCode")
            return CloneResult(
                success = true,
                installCode = installCode,
                launchCode = launchCode,
                message = "SmartQ cloned and launched (install=$installCode launch=$launchCode)",
            )
        } catch (e: Exception) {
            Log.e(TAG, "cloneAndLaunch failed", e)
            return CloneResult(success = false, message = e.message ?: e.javaClass.simpleName)
        }
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
