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

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

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

        requestNotificationPermission()

        chooseMusicButton.setOnClickListener {
            openMusicPicker()
        }

        playButton.setOnClickListener {

            mediaController?.let { controller ->

                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
        }

        connectToMusicService()
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun openMusicPicker() {
        musicPicker.launch("audio/*")
    }

    private fun connectToMusicService() {

        val sessionToken = SessionToken(
            this,
            ComponentName(
                this,
                MusicService::class.java
            )
        )

        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            ).buildAsync()

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
                                    if (isPlaying) {
                                        "PAUSE"
                                    } else {
                                        "PLAY"
                                    }
                            }

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int
                            ) {

                                songText.text =
                                    mediaItem?.mediaMetadata?.title
                                        ?: "Playing music"

                                artistText.text =
                                    mediaItem?.mediaMetadata?.artist
                                        ?: "Local file"
                            }
                        }
                    )

                } catch (exception: Exception) {
                    exception.printStackTrace()
                }

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    override fun onDestroy() {

        playerView.player = null

        mediaController?.release()

        controllerFuture?.cancel(false)

        super.onDestroy()
    }
}
