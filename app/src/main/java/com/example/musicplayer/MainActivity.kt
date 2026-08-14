package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var chooseMusicButton: Button
    private lateinit var playButton: Button
    private lateinit var songText: TextView
    private lateinit var artistText: TextView

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                openMusicPicker()
            }
        }

    private val musicPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                val mediaItem = MediaItem.fromUri(uri)

                mediaController?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }

                songText.text = "Playing music"
                artistText.text = "Local file"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        chooseMusicButton = findViewById(R.id.chooseMusicButton)
        playButton = findViewById(R.id.playButton)
        songText = findViewById(R.id.songText)
        artistText = findViewById(R.id.artistText)

        chooseMusicButton.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        playButton.setOnClickListener {
            mediaController?.let { controller ->

                if (controller.isPlaying) {
                    controller.pause()
                    playButton.text = "PLAY"
                } else {
                    controller.play()
                    playButton.text = "PAUSE"
                }
            }
        }

        connectToMusicService()
    }

    private fun connectToMusicService() {

        val sessionToken = SessionToken(
            this,
            ComponentName(this, MusicService::class.java)
        )

        controllerFuture =
            MediaController.Builder(this, sessionToken)
                .buildAsync()

        controllerFuture?.addListener(
            {
                try {

                    mediaController = controllerFuture?.get()

                    playerView.player = mediaController

                    mediaController?.addListener(
                        object : Player.Listener {

                            override fun onIsPlayingChanged(
                                isPlaying: Boolean
                            ) {
                                playButton.text =
                                    if (isPlaying) "PAUSE" else "PLAY"
                            }
                        }
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun checkPermissionAndOpenPicker() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissionLauncher.launch(
                    Manifest.permission.READ_MEDIA_AUDIO
                )

            } else {
                openMusicPicker()
            }

        } else {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

            } else {
                openMusicPicker()
            }
        }
    }

    private fun openMusicPicker() {

        musicPicker.launch("audio/*")
    }

    override fun onDestroy() {

        playerView.player = null

        mediaController?.release()
        controllerFuture?.cancel(false)

        super.onDestroy()
    }
}
