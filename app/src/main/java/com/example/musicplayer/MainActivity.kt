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

    private var allSongs: List<Song> = emptyList()

    private var mediaController: MediaController? = null
    private var controllerFuture:
        ListenableFuture<MediaController>? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadMusic()
            } else {
                songText.text =
                    "Musiqalarga ruxsat berilmadi"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        playerView =
            findViewById(R.id.playerView)

        songText =
            findViewById(R.id.songText)

        artistText =
            findViewById(R.id.artistText)

        musicList =
            findViewById(R.id.musicList)

        setupRecyclerView()

        requestMusicPermission()

        connectToMusicService()
    }

    private fun setupRecyclerView() {

        musicAdapter =
            MusicAdapter(emptyList()) { song ->

                playSong(song)
            }

        musicList.layoutManager =
            LinearLayoutManager(this)

        musicList.adapter =
            musicAdapter
    }

    private fun requestMusicPermission() {

        val permission =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
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

            songText.text =
                "Musiqalar qidirilmoqda..."

            val repository =
                MusicRepository(contentResolver)

            allSongs =
                repository.getAllSongs()

            musicAdapter.updateSongs(
                allSongs
            )

            if (allSongs.isEmpty()) {

                songText.text =
                    "Musiqa topilmadi"

                artistText.text =
                    "Telefon xotirasida audio fayl yo‘q"

            } else {

                songText.text =
                    "${allSongs.size} ta musiqa"

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

        val controller =
            mediaController ?: return

        if (allSongs.isEmpty()) {
            return
        }

        val mediaItems =
            allSongs.map { currentSong ->

                val metadata =
                    MediaMetadata.Builder()
                        .setTitle(
                            currentSong.title
                        )
                        .setArtist(
                            currentSong.artist
                        )
                        .setAlbumTitle(
                            currentSong.album
                        )
                        .build()

                MediaItem.Builder()
                    .setUri(currentSong.uri)
                    .setMediaMetadata(metadata)
                    .build()
            }

        val selectedIndex =
            allSongs.indexOfFirst {
                it.id == song.id
            }

        controller.setMediaItems(
            mediaItems,
            selectedIndex,
            0L
        )

        controller.prepare()

        controller.play()

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
