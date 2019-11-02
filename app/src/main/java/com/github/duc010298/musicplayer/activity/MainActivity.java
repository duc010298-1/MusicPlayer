package com.github.duc010298.musicplayer.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.duc010298.musicplayer.R;
import com.github.duc010298.musicplayer.adapter.SongAdapter;
import com.github.duc010298.musicplayer.model.Song;
import com.github.duc010298.musicplayer.service.SoundService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ArrayList<Song> songListInDevice;
    private ArrayList<Song> songListDisplay;
    private ListView songView;
    private EditText searchInput;
    private ImageButton playPause;
    private SoundService soundService;
    private Intent playIntent;
    private boolean musicBound = false;
    private SeekBar timeLine;
    private TextView seekbarHint;
    private int totalDuration = 0;
    private boolean isLoop = true;
    private boolean isMix = false;
    private ImageButton btnLoop;
    private ImageButton btnMix;
    private TextView songName;

    private BroadcastReceiver receiveData = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String timeLineDuration = bundle.getString("timeLineDuration");
                if (timeLineDuration != null && !timeLineDuration.equals("null")) {
                    updateTimeline(timeLineDuration);
                    setSeekBarHint(Integer.valueOf(timeLineDuration));
                }
                String max = bundle.getString("maxDuration");
                if (max != null && !max.equals("null")) {
                    timeLine.setMax(Integer.valueOf(max));
                    totalDuration = Integer.valueOf(max);
                }
                String name = bundle.getString("name");
                if (name != null) {
                    songName.setText(name);
                }
            }
        }
    };

    public void setSeekBarHint(int currentDuration) {
        String total = convertDuration(totalDuration);
        String current = convertDuration(currentDuration);
        seekbarHint.setText(current + " / " + total);
    }

    private String convertDuration(long timeSec) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeSec),
                TimeUnit.MILLISECONDS.toSeconds(timeSec) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeSec))
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectView();
        searchInput.clearFocus();
        initVariable();
        getSongListOnDevice();
        registerReceiver(receiveData, new IntentFilter("musicRequest"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, SoundService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SoundService.MusicBinder binder = (SoundService.MusicBinder) service;
            soundService = binder.getService();
            soundService.setList(songListInDevice);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private void initVariable() {
        songListInDevice = new ArrayList<>();
        songListDisplay = new ArrayList<>();
        SongAdapter songAdt = new SongAdapter(this, songListDisplay);
        songView.setAdapter(songAdt);
    }

    private void connectView() {
        songView = findViewById(R.id.listSong);
        searchInput = findViewById(R.id.inputSearch);
        searchInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                String strSearch = editable.toString();
                songListDisplay.clear();
                for (Song s : songListInDevice) {
                    if (containsIgnoreCase(s.getTitle(), strSearch)) {
                        songListDisplay.add(s);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String strSearch = searchInput.getText().toString();
                songListDisplay.clear();
                for (Song song : songListInDevice) {
                    if (containsIgnoreCase(song.getTitle(), strSearch)) {
                        songListDisplay.add(song);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String strSearch = searchInput.getText().toString();
                songListDisplay.clear();
                for (Song song : songListInDevice) {
                    if (containsIgnoreCase(song.getTitle(), strSearch)) {
                        songListDisplay.add(song);
                    }
                }
            }
        });
        playPause = findViewById(R.id.btnPlayPause);
        timeLine = findViewById(R.id.timeLine);
        this.timeLine.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // Khi giá trị progress thay đổi.
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            }

            // Khi người dùng bắt đầu cử chỉ kéo thanh gạt.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // Khi người dùng kết thúc cử chỉ kéo thanh gạt.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int duration = seekBar.getProgress();
                soundService.setDurationSong(duration);
            }
        });

        seekbarHint = findViewById(R.id.timePlay);
        btnLoop = findViewById(R.id.btnLoop);
        btnMix = findViewById(R.id.btnMix);
        songName = findViewById(R.id.songName);
    }

    public void getSongListOnDevice() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int pathColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long duration = musicCursor.getLong(durationColumn);
                String path = musicCursor.getString(pathColumn);
                songListInDevice.add(new Song(thisId, thisTitle, thisArtist, duration, path));
                songListDisplay.add(new Song(thisId, thisTitle, thisArtist, duration, path));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void search(View view) {
        searchInput.clearFocus();
        String strSearch = searchInput.getText().toString();
        songListDisplay.clear();
        for (Song s : songListInDevice) {
            if (containsIgnoreCase(s.getTitle(), strSearch)) {
                songListDisplay.add(s);
            }
        }
    }

    private boolean containsIgnoreCase(String str, String subString) {
        return str.toLowerCase().contains(subString.toLowerCase());
    }

    public void songPicked(View view) {
        playPause.setImageResource(android.R.drawable.ic_media_pause);
        soundService.setSong(Integer.parseInt(view.getTag().toString()));
        soundService.playSong();
    }

    public void updateTimeline(String duration) {
        timeLine.setProgress(Integer.valueOf(duration));
    }

    public void mixClick(View view) {
        isMix = !isMix;
        soundService.toggleMix();
    }

    public void loopClick(View view) {
        isLoop = !isLoop;
        soundService.toggleLoop();
    }

    public void playPauseClick(View view) {
        if (soundService.isPlay) {
            playPause.setImageResource(android.R.drawable.ic_media_play);
            soundService.pauseSong();
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
            soundService.resumeSong();
        }
    }

    public void nextClick(View view) {
        soundService.playNext();
    }

    public void previousClick(View view) {
        soundService.playPrevious();
    }
}
