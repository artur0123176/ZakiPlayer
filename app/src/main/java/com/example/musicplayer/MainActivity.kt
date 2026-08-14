package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var songText: TextView
    private lateinit var artistText: TextView
    private lateinit var musicList: RecyclerView

    private lateinit var musicAdapter: MusicAdapter

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadMusic()
            } else {
                songText.text = "Musiqalarga ruxsat berilmadi"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        songText = findViewById(R.id.songText)
        artistText = findViewById(R.id.artistText)
        musicList = findViewById(R.id.musicList)

        setupRecyclerView()

        requestMusicPermission()

        connectToMusicService()
    }

    private fun setupRecyclerView() {

        musicAdapter = MusicAdapter(
            emptyList()
        ) { song ->

            playSong(song)
        }

        musicList.layoutManager =
            LinearLayoutManager(this)

        musicList.adapter = musicAdapter
    }

    private fun requestMusicPermission() {

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                Manifest.permission.READ_MEDIA_AUDIO

            } else {

                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            loadMusic()

        } else {

            permissionLauncher.launch(permission)
        }
    }

    private fun loadMusic() {

        lifecycleScope.launch {

            songText.text = "Musiqalar qidirilmoqda..."

            val repository =
                MusicRepository(contentResolver)

            val songs =
                repository.getAllSongs()

            musicAdapter.updateSongs(songs)

            if (songs.isEmpty()) {

                songText.text =
                    "Musiqa topilmadi"

                artistText.text =
                    "Telefon xotirasida audio fayl yo‘q"

            } else {

                songText.text =
                    "${songs.size} ta musiqa"

                artistText.text =
                    "Qo‘shiqni tanlang"
            }
        }
    }

    private fun connectToMusicService() {

        val sessionToken =
            SessionToken(
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

                    mediaController =
                        controllerFuture?.get()

                    playerView.player =
                        mediaController

                } catch (e: Exception) {

                    e.printStackTrace()
                }

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun playSong(song: Song) {

        val metadata =
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .build()

        val mediaItem =
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(metadata)
                .build()

        mediaController?.apply {

            setMediaItem(mediaItem)

            prepare()

            play()
        }

        songText.text =
            song.title

        artistText.text =
            song.artist
    }

    override fun onDestroy() {

        playerView.player = null

        mediaController?.release()

        controllerFuture?.cancel(false)

        super.onDestroy()
    }
}
