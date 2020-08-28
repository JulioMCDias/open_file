package com.jlmcdeveloper.open_file

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** OpenFilePlugin */
class OpenFilePlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

  private var activityBinding: ActivityPluginBinding? = null

  private lateinit var channel : MethodChannel
  private lateinit var activity: Activity
  private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
  private var application: Application? = null
  private var delegate: OpenFileDelegate? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "open_file")
    this.flutterPluginBinding = flutterPluginBinding
    channel.setMethodCallHandler(this)
  }



  @RequiresApi(Build.VERSION_CODES.KITKAT)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when(call.method){
      "openFile" -> {
        val type = resolveType(call.argument<String>("type"))
        val extension = call.argument<String>("extension")?: ""
        delegate?.startFileExplorer(OpenFileDelegate
                .FileExplorerInfo(type, extension, result, null, delegate!!.editFile))
      }

      "createFile" -> {
        val type = resolveType(call.argument<String>("type"))
        val extension = call.argument<String>("extension") ?: ""
        val name = call.argument<String>("nameFile") ?: ""
        delegate?.startFileExplorer(OpenFileDelegate
                .FileExplorerInfo(type, extension, result, name, delegate!!.createFile))

      }else ->
        result.notImplemented()
    }
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }


  private fun resolveType(type: String?): String {
    return when (type) {
      "audio" -> "audio/"
      "text" -> "text/"
      "image" -> "image/"
      "video" -> "video/"
      "any", "custom" -> "*/*"
      else -> "*/*"
    }
  }


  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
    activity = binding.activity
    application = flutterPluginBinding.applicationContext as Application
    delegate = OpenFileDelegate(activity)
    delegate?.let {
      binding.addActivityResultListener(it)
      binding.addRequestPermissionsResultListener(it)
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
    tearDown()
  }



  private fun tearDown() {
    delegate?.let {
      activityBinding?.removeActivityResultListener(it)
      activityBinding?.removeRequestPermissionsResultListener(it)
    }
    this.activityBinding = null
    delegate = null
    channel.setMethodCallHandler(null)
    application = null
  }
}
