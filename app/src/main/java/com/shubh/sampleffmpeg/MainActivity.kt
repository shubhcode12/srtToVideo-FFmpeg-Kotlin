package com.shubh.sampleffmpeg

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var buttonSelectVideo: Button
    private lateinit var buttonSelectSrt: Button
    private lateinit var buttonBurnSubtitles: Button
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var progressBar: ProgressBar
    private lateinit var pickVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickSrtLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoView: VideoView


    private var videoUri: Uri? = null
    private var srtUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSelectVideo = findViewById<Button>(R.id.buttonSelectVideo)
        buttonSelectSrt = findViewById<Button>(R.id.buttonSelectSrt)
        buttonBurnSubtitles = findViewById<Button>(R.id.buttonBurnSubtitles)
        progressBar = findViewById(R.id.progressBar)
        videoView = findViewById(R.id.videoView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        // Link MediaController with VideoView
        videoView.setMediaController(mediaController)

        // Initialize permissions
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                if (!allGranted) {
                    Toast.makeText(
                        this, "Permissions are required to burn subtitles.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        // Initialize file pickers
        pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        videoUri = uri
                        videoView.setVideoURI(uri)
                        videoView.start()
                    }
                    buttonSelectVideo.text = "Video Selected"
                    Toast.makeText(this, "Video selected!", Toast.LENGTH_SHORT).show()
                }
            }

        pickSrtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    srtUri = result.data?.data
                    buttonSelectSrt.text = "SRT Selected"
                    Toast.makeText(this, "SRT file selected!", Toast.LENGTH_SHORT).show()
                }
            }


        val fontsCopied = copyFontsToInternalStorage()

        buttonSelectVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"
            pickVideoLauncher.launch(intent)
        }

        buttonSelectSrt.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "*/*"
            pickSrtLauncher.launch(intent)
        }

        buttonBurnSubtitles.setOnClickListener {
            if (videoUri != null && srtUri != null) {

                if (fontsCopied) {
                    progressBar.visibility = View.VISIBLE
                    burnSubtitles()
                } else {
                    Toast.makeText(
                        this, "Fonts are being set up. Please wait...", Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                Toast.makeText(this, "Please select both video and SRT files.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun copyFontsToInternalStorage(): Boolean {
        val assetManager = assets
        val fontsDir = File(getExternalFilesDir(null), "fonts")

        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        return try {
            // List all font files in the assets folder
            val fontFiles = assetManager.list("")?.filter { it.endsWith(".ttf") }

            fontFiles?.forEach { fontFileName ->
                val inputStream = assetManager.open(fontFileName)
                val outputFile = File(fontsDir, fontFileName)
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                inputStream.close()
                outputStream.close()

                // Show a toast for each font copied
                Toast.makeText(
                    this, "$fontFileName copied to internal storage.", Toast.LENGTH_SHORT
                ).show()
            }

            true // Return true after all fonts are copied
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to copy fonts.", Toast.LENGTH_SHORT).show()
            false
        }
    }


    private fun burnSubtitles() {
        val inputVideoPath = getPathFromUri(videoUri!!)
        val inputSrtPath = getPathFromUri(srtUri!!)
        Log.d("MainActivity", "Input video path: $inputVideoPath")
        Log.d("MainActivity", "Input SRT path: $inputSrtPath")
        val randomNumber = Random().nextInt(10000)
        val outputVideoPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "output_video_$randomNumber.mp4"
        ).absolutePath
        val fontsDir = File(getExternalFilesDir(null), "fonts").absolutePath

        val command =
            "-i \"$inputVideoPath\" -vf \"subtitles='$inputSrtPath':fontsdir='$fontsDir':force_style='FontName=Komika Title - Brush,FontSize=24,PrimaryColour=&H03FCFF,Alignment=10'\" -c:v mpeg4 -c:a copy -movflags +faststart \"$outputVideoPath\""

        FFmpegKit.executeAsync(command) { session ->
            runOnUiThread {
                progressBar.visibility = View.GONE
            }
            val returnCode = session.returnCode
            val output = session.output

            if (returnCode.isValueSuccess) {
                Log.d("MainActivity", "Subtitles burned successfully!")
                addVideoToGallery(outputVideoPath)
                runOnUiThread {
                    videoView.setVideoPath(outputVideoPath)
                    videoView.visibility = View.VISIBLE
                    videoView.start()
                }
            } else {
                Log.e("MainActivity", "Failed to burn subtitles: $output")
                runOnUiThread {
                    Toast.makeText(this, "Failed to burn subtitles.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun getPathFromUri(uri: Uri?): String? {
        uri?.let {
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    return cursor.getString(columnIndex)
                }
            }
        }
        return null
    }

    private fun addVideoToGallery(videoPath: String) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DATA, videoPath)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, "output_video")
        }
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }
}
