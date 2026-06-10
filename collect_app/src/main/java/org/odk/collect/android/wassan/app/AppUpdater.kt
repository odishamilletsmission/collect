package org.odk.collect.android.wassan.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import org.odk.collect.android.R
import java.io.File
import java.io.IOException

class AppUpdater(
    private val activity: Activity,
    private val updateJsonUrl: String, // URL to version.json
    private val forceUpdate: Boolean = false
) {
    private val client = OkHttpClient()
    private var downloadJob: Job? = null

    fun checkForUpdate() {
        val currentVersionCode = activity.packageManager
            .getPackageInfo(activity.packageName, 0).versionCode

        val request = Request.Builder().url(updateJsonUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // optional: silently fail
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonStr ->
                    val json = JSONObject(jsonStr)
                    val latestVersion = json.getInt("versionCode")
                    val apkUrl = json.getString("apkUrl")
                    val releaseNotes = json.optString("releaseNotes", "")

                    if (latestVersion > currentVersionCode) {
                        activity.runOnUiThread {
                            showUpdateDialog(apkUrl, releaseNotes)
                        }
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(apkUrl: String, releaseNotes: String) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_update, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.progressText)
        val releaseNotesText = dialogView.findViewById<TextView>(R.id.releaseNotes)
        releaseNotesText.text = releaseNotes

        builder.setView(dialogView)
            .setTitle("Update Available")
            .setPositiveButton("Update", null)

        if (!forceUpdate) {
            builder.setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
        } else {
            builder.setCancelable(false)
        }

        val dialog = builder.create()
        dialog.show()

        // Override positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            downloadJob = CoroutineScope(Dispatchers.IO).launch {
                downloadAndInstall(apkUrl, progressBar, progressText)
            }
        }
    }

    private suspend fun downloadAndInstall(apkUrl: String, progressBar: ProgressBar, progressText: TextView) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "app-update.apk"
                val destination = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (destination.exists()) destination.delete()

                val request = Request.Builder().url(apkUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: return@withContext

                val total = body.contentLength()
                var downloaded = 0L

                val input = body.byteStream()
                destination.outputStream().use { fileOut ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        fileOut.write(buffer, 0, read)
                        downloaded += read
                        val progress = ((downloaded * 100) / total).toInt()
                        withContext(Dispatchers.Main) {
                            progressBar.progress = progress
                            progressText.text = "$progress%"
                        }
                    }
                    fileOut.flush()
                }

                withContext(Dispatchers.Main) {
                    installApk(destination)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(intent)
    }
}
