package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

data class ApkInfo(
    val file: File,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val iconDrawable: Drawable?,
    val fileSizeFormatted: String,
    val isValid: Boolean,
    val isMultiApk: Boolean = false,
    val statusMessage: String = ""
)

object ApkInspectorEngine {

    suspend fun inspectApk(context: Context, file: File): ApkInfo = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) {
            return@withContext ApkInfo(
                file = file,
                appName = file.name,
                packageName = "unknown",
                versionName = "-",
                versionCode = 0,
                minSdk = 0,
                targetSdk = 0,
                iconDrawable = null,
                fileSizeFormatted = formatFileSize(file.length()),
                isValid = false,
                statusMessage = "الملف غير موجود أو لا يمكن قراءته"
            )
        }

        val ext = file.extension.lowercase()
        val isBundle = ext == "apks" || ext == "xapk" || ext == "apkm"

        // If it's XAPK or APKS, check zip contents
        if (isBundle) {
            try {
                ZipFile(file).use { zip ->
                    val entries = zip.entries().asSequence().map { it.name }.toList()
                    val hasApk = entries.any { it.endsWith(".apk") }
                    return@withContext ApkInfo(
                        file = file,
                        appName = file.nameWithoutExtension,
                        packageName = "حزمة منقسمة (${ext.uppercase()})",
                        versionName = "يتضمن ${entries.count { it.endsWith(".apk") }} ملف APK",
                        versionCode = 1,
                        minSdk = 21,
                        targetSdk = 34,
                        iconDrawable = null,
                        fileSizeFormatted = formatFileSize(file.length()),
                        isValid = hasApk,
                        isMultiApk = true,
                        statusMessage = if (hasApk) "حزمة متعددة الأجزاء صالحة" else "حزمة تالفة"
                    )
                }
            } catch (e: Exception) {
                // fall through
            }
        }

        try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val flags = PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES
            val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, flags)

            if (packageInfo != null && packageInfo.applicationInfo != null) {
                val appInfo = packageInfo.applicationInfo!!
                appInfo.sourceDir = file.absolutePath
                appInfo.publicSourceDir = file.absolutePath

                val appName = try {
                    appInfo.loadLabel(pm).toString()
                } catch (_: Exception) {
                    file.nameWithoutExtension
                }

                val icon = try {
                    appInfo.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                ApkInfo(
                    file = file,
                    appName = appName,
                    packageName = packageInfo.packageName ?: "com.app",
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = vCode,
                    minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21,
                    targetSdk = appInfo.targetSdkVersion,
                    iconDrawable = icon,
                    fileSizeFormatted = formatFileSize(file.length()),
                    isValid = true,
                    statusMessage = "ملف APK سليم وجاهز للتثبيت"
                )
            } else {
                ApkInfo(
                    file = file,
                    appName = file.name,
                    packageName = "غير معروف",
                    versionName = "-",
                    versionCode = 0,
                    minSdk = 0,
                    targetSdk = 0,
                    iconDrawable = null,
                    fileSizeFormatted = formatFileSize(file.length()),
                    isValid = false,
                    statusMessage = "تعذر قراءة بيانات حزمة APK، قد يكون الملف تالفاً أو غير مدعوم."
                )
            }
        } catch (e: Exception) {
            ApkInfo(
                file = file,
                appName = file.name,
                packageName = "خطأ",
                versionName = "-",
                versionCode = 0,
                minSdk = 0,
                targetSdk = 0,
                iconDrawable = null,
                fileSizeFormatted = formatFileSize(file.length()),
                isValid = false,
                statusMessage = e.localizedMessage ?: "فشل فحص الملف"
            )
        }
    }

    fun launchInstaller(context: Context, file: File): Boolean {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format("%.2f ميجابايت", mb)
        } else {
            String.format("%.1f كيلوبايت", bytes / 1024.0)
        }
    }
}
