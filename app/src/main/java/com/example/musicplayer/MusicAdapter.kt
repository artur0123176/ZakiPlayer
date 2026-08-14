package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<MusicAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title: TextView = view.findViewById(R.id.songTitle)
        val artist: TextView = view.findViewById(R.id.songArtist)
        val duration: TextView = view.findViewById(R.id.songDuration)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SongViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_song,
                parent,
                false
            )

        return SongViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SongViewHolder,
        position: Int
    ) {

        val song = songs[position]

        holder.title.text = song.title
        holder.artist.text = song.artist
        holder.duration.text = formatDuration(song.duration)

        holder.itemView.setOnClickListener {
            onSongClick(song)
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    private fun formatDuration(milliseconds: Long): String {

        val totalSeconds = milliseconds / 1000

        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format(
            "%d:%02d",
            minutes,
            seconds
        )
    }
}
