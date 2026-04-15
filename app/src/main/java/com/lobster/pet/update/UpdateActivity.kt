package com.lobster.pet.update

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * 更新检查Activity
 * 提供更新UI和权限处理
 */
class UpdateActivity : AppCompatActivity() {

    private lateinit var updateManager: UpdateManager
    private var progressDialog: ProgressDialog? = null
    private var downloadUrl: String = ""
    
    companion object {
        const val REQUEST_INSTALL_PACKAGES = 1001
        const val REQUEST_STORAGE_PERMISSION = 1002
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        updateManager = UpdateManager(this).apply {
            setOnUpdateListener(object : UpdateManager.OnUpdateListener {
                override fun onChecking() {
                    showProgressDialog("正在检查更新...")
                }
                
                override fun onUpdateAvailable(version: String, changelog: String, downloadUrl: String) {
                    dismissProgressDialog()
                    this@UpdateActivity.downloadUrl = downloadUrl
                    showUpdateDialog(version, changelog)
                }
                
                override fun onNoUpdate() {
                    dismissProgressDialog()
                    Toast.makeText(this@UpdateActivity, "已经是最新版本", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
                override fun onDownloadStarted() {
                    showProgressDialog("正在下载更新...", true)
                }
                
                override fun onDownloadProgress(progress: Int) {
                    progressDialog?.setProgress(progress)
                }
                
                override fun onDownloadComplete(apkFile: File) {
                    dismissProgressDialog()
                    checkInstallPermission(apkFile)
                }
                
                override fun onDownloadFailed(error: String) {
                    dismissProgressDialog()
                    Toast.makeText(this@UpdateActivity, error, Toast.LENGTH_LONG).show()
                    finish()
                }
                
                override fun onInstallStarted() {
                    Toast.makeText(this@UpdateActivity, "开始安装...", Toast.LENGTH_SHORT).show()
                }
                
                override fun onError(error: String) {
                    dismissProgressDialog()
                    Toast.makeText(this@UpdateActivity, error, Toast.LENGTH_LONG).show()
                    finish()
                }
            })
        }
        
        // 开始检查更新
        checkStoragePermission()
    }
    
    /**
     * 检查存储权限
     */
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED -> {
                    updateManager.checkForUpdate()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    AlertDialog.Builder(this)
                        .setTitle("需要存储权限")
                        .setMessage("下载更新需要存储权限")
                        .setPositiveButton("确定") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                REQUEST_STORAGE_PERMISSION
                            )
                        }
                        .setNegativeButton("取消") { _, _ -> finish() }
                        .show()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION
                    )
                }
            }
        } else {
            updateManager.checkForUpdate()
        }
    }
    
    /**
     * 检查安装权限
     */
    private fun checkInstallPermission(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                AlertDialog.Builder(this)
                    .setTitle("需要安装权限")
                    .setMessage("请允许安装未知来源应用以完成更新")
                    .setPositiveButton("去设置") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivityForResult(intent, REQUEST_INSTALL_PACKAGES)
                    }
                    .setNegativeButton("取消") { _, _ -> finish() }
                    .show()
            } else {
                updateManager.installUpdate(apkFile)
                finish()
            }
        } else {
            updateManager.installUpdate(apkFile)
            finish()
        }
    }
    
    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(version: String, changelog: String) {
        AlertDialog.Builder(this)
            .setTitle("发现新版本 v$version")
            .setMessage(changelog)
            .setPositiveButton("立即更新") { _, _ ->
                updateManager.downloadUpdate(downloadUrl)
            }
            .setNegativeButton("稍后") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 显示进度对话框
     */
    private fun showProgressDialog(message: String, showProgress: Boolean = false) {
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            if (showProgress) {
                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                setProgress(0)
                setMax(100)
            }
            show()
        }
    }
    
    /**
     * 关闭进度对话框
     */
    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateManager.checkForUpdate()
                } else {
                    Toast.makeText(this, "需要存储权限才能下载更新", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    // 重新尝试安装
                    val downloadDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val apkFile = File(downloadDir, "lobster-pet-update.apk")
                    if (apkFile.exists()) {
                        updateManager.installUpdate(apkFile)
                    }
                }
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        updateManager.cleanup()
        dismissProgressDialog()
    }
}
