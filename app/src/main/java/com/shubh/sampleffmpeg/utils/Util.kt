package com.shubh.sampleffmpeg.utils

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Util {
//    private fun configureFFmpegKit() {
//        val fontsDir = File(getExternalFilesDir(null), "fonts").absolutePath
//        Log.d("MainActivity", "Fonts directory: $fontsDir")
//
//        val fontConfigPath = File(getExternalFilesDir(null), "fonts.conf").absolutePath
//        Log.d("MainActivity", "Fontconfig path: $fontConfigPath")
//
//        // Create fonts.conf file if it does not exist
//        createFontConfig(fontConfigPath, fontsDir)
//
//        // Map font names to font files
//        val fontNameMapping = mapOf(
//            "Impact" to "impact.ttf",
//            "Bobaland" to "bobaland.ttf",
//            "Komika Title - Brush" to "komtibr.ttf"
//        )
//
//        // Set the font directory and fontconfig configuration
//        FFmpegKitConfig.setFontDirectory(this@MainActivity, fontsDir, fontNameMapping)
//        FFmpegKitConfig.setFontconfigConfigurationPath(fontConfigPath)
//    }
//
//    private fun createFontConfig(configPath: String, fontDir: String) {
//        val configContent = """
//            <?xml version="1.0"?>
//            <fontconfig>
//                <dir>$fontDir</dir>
//            </fontconfig>
//        """.trimIndent()
//
//        try {
//            FileOutputStream(configPath).use { outputStream ->
//                outputStream.write(configContent.toByteArray())
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
}