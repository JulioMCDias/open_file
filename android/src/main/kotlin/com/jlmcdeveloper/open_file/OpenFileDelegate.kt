package com.jlmcdeveloper.open_file

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class OpenFileDelegate(private val activity: Activity)
    : PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private val permissionManager: PermissionManager
    private lateinit var solicitation: FileExplorerInfo
    var result: MethodChannel.Result? = null

    init {
        permissionManager = object : PermissionManager {
            override fun isPermissionGranted(permissionName: String?): Boolean {
                return (ActivityCompat.checkSelfPermission(activity, permissionName!!)
                        == PackageManager.PERMISSION_GRANTED)
            }
            override fun askForPermission(permissionName: String?, requestCode: Int) {
                ActivityCompat.requestPermissions(activity, arrayOf(permissionName), requestCode)
            }
        }

    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if ((requestCode == OpenFileFramework.EDIT_REQUEST_CODE ||
                        requestCode == OpenFileFramework.WRITE_REQUEST_CODE) &&
                resultCode == Activity.RESULT_OK) {

            data?.data?.also { uri ->
                val file = OpenFileFramework.dumpMetaData(uri, activity)
                val path = FileUtils.getPath(uri, activity)

                result?.success(mutableMapOf(
                        "path" to path,
                        "nameFile" to file.nameFile,
                        "size" to file.size))
            }
            return true
        }
        return false
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {

        if (OpenFileFramework.EDIT_REQUEST_CODE != requestCode
                || OpenFileFramework.WRITE_REQUEST_CODE != requestCode) {
            return false
        }

        val permissionGranted = grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if(permissionGranted)
            solicitation.start()

        return true
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    var editFile = {
        OpenFileFramework.editDocument(solicitation.type, solicitation.extension, activity)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    val createFile: () -> Unit = {
        if (solicitation.name != null){
            OpenFileFramework.createFile(
                    solicitation.type,
                    solicitation.extension,
                    solicitation.name!!, activity
            )
        }else
            Log.e(OpenFileFramework.TAG, "Name == null")

    }


    fun startFileExplorer(solicitation: FileExplorerInfo){
        this.solicitation = solicitation
        this.result = solicitation.result
        if(startFileExplorer())
            solicitation.start()
    }


    private fun startFileExplorer(): Boolean{
        if (!permissionManager.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) ||
            !permissionManager.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            permissionManager.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    OpenFileFramework.WRITE_REQUEST_CODE)

            permissionManager.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    OpenFileFramework.EDIT_REQUEST_CODE)
             return false
        }
        return true
    }


    data class FileExplorerInfo(
            val type: String,
            val extension: String,
            val result: MethodChannel.Result,
            val name: String?,
            val start: () -> Unit)

    internal interface PermissionManager {
        fun isPermissionGranted(permissionName: String?): Boolean
        fun askForPermission(permissionName: String?, requestCode: Int)
    }
}