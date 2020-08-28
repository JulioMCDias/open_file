package com.jlmcdeveloper.open_file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File


object OpenFileFramework {
    val WRITE_REQUEST_CODE: Int = OpenFilePlugin::class.java.hashCode() + 43 and 0x0000ffff
    val EDIT_REQUEST_CODE: Int = OpenFilePlugin::class.java.hashCode() + 44 and 0x0000ffff
    const val TAG = "TAG OPEN FILE"

    data class File(val nameFile: String, val size: String)


    // ------------ criar arquivo ------
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun createFile(mimeType: String, extension: String, fileName: String, activity: Activity) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

            // Create a file with the requested MIME type.
            type = mimeType + extension
            putExtra(Intent.EXTRA_TITLE, "$fileName.$extension")
        }
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
        } else
            Log.e(TAG, "Can't find a valid activity to handle the request. Make sure you've a file explorer installed.")
    }


    // ------------ editar arquivo ----------
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun editDocument(mimeType: String, extension: String, activity: Activity) {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = mimeType + extension
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, EDIT_REQUEST_CODE)
        } else
            Log.e(TAG, "Can't find a valid activity to handle the request. Make sure you've a file explorer installed.")

    }


    // ler metadados da leitura (name file, size)
    fun dumpMetaData(uri: Uri, context: Context): File {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {

                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                val size: String = if (!it.isNull(sizeIndex)) {
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                return File(displayName, size)
            }
        }
        return File("Unknown", "Unknown")
    }
}