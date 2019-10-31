package com.github.duc010298.musicplayerrebuild.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.duc010298.musicplayerrebuild.R;
import com.github.duc010298.musicplayerrebuild.model.Song;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout songLayout = (LinearLayout) songInf.inflate(R.layout.song_display_layout, parent, false);
        //get title and artist views
        TextView songView = (TextView) songLayout.findViewById(R.id.songTitle);
        TextView artistView = (TextView) songLayout.findViewById(R.id.songArtist);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLayout.setTag(position);
        return songLayout;
    }

}
