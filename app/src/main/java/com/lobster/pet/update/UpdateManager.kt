package com.lobster.pet.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.lobster.pet.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 应用更新管理器
 * 检查GitHub Release并自动下载安装
 */
class UpdateManager(private val context: Context) {

    private val TAG = "UpdateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // GitHub仓库信息
    private val GITHUB_OWNER = "catcatboy-cyber"
    private val GITHUB_REPO = "lobster-pet"
    private val RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private val DOWNLOAD_APK_NAME = "lobster-pet-update.apk"
    
    private var downloadId: Long = -1
    private var onUpdateListener: OnUpdateListener? = null
    private var downloadReceiver: BroadcastReceiver? = null
    
    interface OnUpdateListener {
        fun onChecking()
        fun onUpdateAvailable(version: String, changelog: String, downloadUrl: String)
        fun onNoUpdate()
        fun onDownloadStarted()
        fun onDownloadProgress(progress: Int)
        fun onDownloadComplete(apkFile: File)
        fun onDownloadFailed(error: String)
        fun onInstallStarted()
        fun onError(error: String)
    }
    
    fun setOnUpdateListener(listener: OnUpdateListener) {
        this.onUpdateListener = listener
    }
    
    /**
     * 检查更新
     */
    fun checkForUpdate() {
        onUpdateListener?.onChecking()
        
        scope.launch {
            try {
                val (latestVersion, changelog, downloadUrl) = fetchLatestRelease()
                
                withContext(Dispatchers.Main) {
                    if (latestVersion != null && isNewerVersion(latestVersion)) {
                        Log.d(TAG, "New version available: $latestVersion")
                        onUpdateListener?.onUpdateAvailable(latestVersion, changelog, downloadUrl)
                    } else {
                        Log.d(TAG, "No update available")
                        onUpdateListener?.onNoUpdate()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check update", e)
                withContext(Dispatchers.Main) {
                    onUpdateListener?.onError("检查更新失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 从GitHub API获取最新Release
     */
    private suspend fun fetchLatestRelease(): Triple<String?, String, String> {
        return withContext(Dispatchers.IO) {
            val connection = URL(RELEASE_API_URL).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "LobsterPet-UpdateChecker")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            val version = json.optString("tag_name", "").removePrefix("v")
            val changelog = json.optString("body", "暂无更新说明")
            
            // 查找APK下载链接
            var downloadUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            
            Triple(version.takeIf { it.isNotEmpty() }, changelog, downloadUrl)
        }
    }
    
    /**
     * 比较版本号
     */
    private fun isNewerVersion(latestVersion: String): Boolean {
        val currentVersion = BuildConfig.VERSION_NAME
        return compareVersion(latestVersion, currentVersion) > 0
    }
    
    /**
     * 版本号比较
     * @return 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    private fun compareVersion(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
        val maxLength = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            
            when {
                num1 > num2 -> return 1
                num1 < num2 -> return -1
            }
        }
        return 0
    }
    
    /**
     * 下载更新
     */
    fun downloadUpdate(downloadUrl: String) {
        if (downloadUrl.isEmpty()) {
            onUpdateListener?.onDownloadFailed("下载链接无效")
            return
        }
        
        try {
            // 清理旧文件
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadDir, DOWNLOAD_APK_NAME)
            if (apkFile.exists()) {
                apkFile.delete()
            }
            
            // 使用系统DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("龙虾宠物更新")
                setDescription("正在下载最新版本...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(apkFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)
            
            onUpdateListener?.onDownloadStarted()
            
            // 注册下载完成监听
            registerDownloadReceiver(apkFile)
            
            // 启动进度监听
            startProgressTracking()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
            onUpdateListener?.onDownloadFailed("启动下载失败: ${e.message}")
        }
    }
    
    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver(apkFile: File) {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    downloadReceiver = null
                    
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            onUpdateListener?.onDownloadComplete(apkFile)
                        } else {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            onUpdateListener?.onDownloadFailed("下载失败 (错误码: $reason)")
                        }
                    }
                    cursor.close()
                }
            }
        }
        
        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }
    
    /**
     * 跟踪下载进度
     */
    private fun startProgressTracking() {
        scope.launch {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            while (isActive && downloadId != -1L) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                        withContext(Dispatchers.Main) {
                            onUpdateListener?.onDownloadProgress(progress)
                        }
                    }
                }
                cursor.close()
                delay(500)
            }
        }
    }
    
    /**
     * 安装APK
     */
    fun installUpdate(apkFile: File) {
        if (!apkFile.exists()) {
            onUpdateListener?.onError("APK文件不存在")
            return
        }
        
        try {
            onUpdateListener?.onInstallStarted()
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 使用FileProvider
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    // Android 7.0以下
                    setDataAndType(
                        Uri.fromFile(apkFile),
                        "application/vnd.android.package-archive"
                    )
                }
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            onUpdateListener?.onError("安装失败: ${e.message}")
        }
    }
    
    /**
     * 清理
     */
    fun cleanup() {
        scope.cancel()
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
}
