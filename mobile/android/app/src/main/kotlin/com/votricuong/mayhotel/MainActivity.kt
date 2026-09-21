package com.votricuong.mayhotel

import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : FlutterFragmentActivity() {
    private val CHANNEL = "com.votricuong.mayhotel/security"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "isBootloaderUnlocked") {
                val isUnlocked = checkBootloaderUnlocked()
                result.success(isUnlocked)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun checkBootloaderUnlocked(): Boolean {
        try {
            // Cách 1: Kiểm tra thuộc tính ro.boot.flash.locked (0 nghĩa là đã unlock)
            val process = Runtime.getRuntime().exec("getprop ro.boot.flash.locked")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val locked = reader.readLine()
            reader.close()
            process.destroy()

            if (locked != null && locked == "0") {
                return true
            }
            
            // Cách 2: Kiểm tra thuộc tính ro.boot.verifiedbootstate (orange nghĩa là đã unlock)
            val process2 = Runtime.getRuntime().exec("getprop ro.boot.verifiedbootstate")
            val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
            val state = reader2.readLine()
            reader2.close()
            process2.destroy()

            if (state != null && state == "orange") {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}