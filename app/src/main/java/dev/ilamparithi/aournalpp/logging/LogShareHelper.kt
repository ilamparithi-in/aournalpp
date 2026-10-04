package dev.ilamparithi.aournalpp.logging

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object LogShareHelper {

    fun shareTextFile(
        context: Context,
        fileName: String,
        content: String,
        subject: String,
        chooserTitle: String
    ) {
        try {
            val sharedLogsDir = File(context.cacheDir, "shared_logs").apply { mkdirs() }
            val file = File(sharedLogsDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(subject, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to share log: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        toastMessage: String
    ) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to copy: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
