package com.votricuong.mayhotel

import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.provider.Settings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class MainActivity : FlutterFragmentActivity() {
    private val CHANNEL = "com.votricuong.mayhotel/security"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getSecuritySnapshot" -> result.success(
                    mapOf(
                        "deviceFingerprint" to createDeviceFingerprint(),
                        "isBootloaderUnlocked" to checkBootloaderUnlocked()
                    )
                )
                // Keep the legacy method while installed app versions are being replaced.
                "isBootloaderUnlocked" -> result.success(checkBootloaderUnlocked())
                else -> result.notImplemented()
            }
        }
    }

    private fun createDeviceFingerprint(): String {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: throw IllegalStateException("ANDROID_ID is unavailable")
        val source = "$packageName:$androidId"
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
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
