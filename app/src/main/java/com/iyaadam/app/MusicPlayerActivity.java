package com.iyaadam.app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity {

private static final String TAG = "DDKitchenMusic";

private static final String MUSIC_API =
        "https://iyaadam.uhd.com.ng/api/music.php";

private MediaPlayer mediaPlayer;

private final List<Music> playlist = new ArrayList<>();

private int currentTrackIndex = 0;

private TextView trackTitle;
private TextView trackArtist;
private TextView currentTime;
private TextView totalTime;

private SeekBar seekBar;

private ImageButton playPauseBtn;
private ImageButton nextBtn;
private ImageButton prevBtn;
private ImageButton closeBtn;

private ImageView albumArt;

private RecyclerView playlistRecycler;

private MusicAdapter musicAdapter;

private final Handler handler =
        new Handler(Looper.getMainLooper());

private boolean isPlaying = false;
private boolean isPreparing = false;
private boolean isReleased = false;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_music_player);

    initViews();
    setupMediaPlayer();
    setupListeners();
    loadPlaylistFromServer();
}

private void initViews() {

    trackTitle = findViewById(R.id.track_title);
    trackArtist = findViewById(R.id.track_artist);

    currentTime = findViewById(R.id.current_time);
    totalTime = findViewById(R.id.total_time);

    seekBar = findViewById(R.id.seek_bar);

    playPauseBtn = findViewById(R.id.btn_play_pause);
    nextBtn = findViewById(R.id.btn_next);
    prevBtn = findViewById(R.id.btn_prev);
    closeBtn = findViewById(R.id.btn_close);

    albumArt = findViewById(R.id.album_art);

    playlistRecycler =
            findViewById(R.id.playlist_recycler);

    playlistRecycler.setLayoutManager(
            new LinearLayoutManager(this)
    );

    playPauseBtn.setImageResource(
            android.R.drawable.ic_media_play
    );

    trackTitle.setText("Loading music...");
    trackArtist.setText("Please wait");
    currentTime.setText("00:00");
    totalTime.setText("00:00");
}

private void setupMediaPlayer() {

    mediaPlayer = new MediaPlayer();

    mediaPlayer.setOnCompletionListener(mp -> {

        isPlaying = false;
        isPreparing = false;

        playPauseBtn.setImageResource(
                android.R.drawable.ic_media_play
        );

        if (!playlist.isEmpty()) {
            playNext();
        }
    });

    mediaPlayer.setOnErrorListener((mp, what, extra) -> {

        Log.e(TAG,
                "Playback error. what=" + what
                        + ", extra=" + extra);

        isPlaying = false;
        isPreparing = false;

        playPauseBtn.setImageResource(
                android.R.drawable.ic_media_play
        );

        Toast.makeText(
                MusicPlayerActivity.this,
                "Unable to play this track. Try another song.",
                Toast.LENGTH_LONG
        ).show();

        return true;
    });
}

private void setupListeners() {

    playPauseBtn.setOnClickListener(v ->
            togglePlayPause());

    nextBtn.setOnClickListener(v ->
            playNext());

    prevBtn.setOnClickListener(v ->
            playPrevious());

    closeBtn.setOnClickListener(v ->
            finish());

    seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(
                        SeekBar bar,
                        int progress,
                        boolean fromUser) {

                    if (fromUser
                            && mediaPlayer != null
                            && isPlaying
                            && mediaPlayer.isPlaying()) {

                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(
                        SeekBar bar) {
                }

                @Override
                public void onStopTrackingTouch(
                        SeekBar bar) {
                }
            }
    );
}

private void loadPlaylistFromServer() {

    trackTitle.setText("Loading music...");
    trackArtist.setText("Connecting to music library");

    new Thread(() -> {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(MUSIC_API);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200
                    && responseCode < 300) {

                inputStream = connection.getInputStream();

            } else {

                inputStream = connection.getErrorStream();

                String errorBody = "";

                if (inputStream != null) {

                    StringBuilder errorText =
                            new StringBuilder();

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 inputStream,
                                                 StandardCharsets.UTF_8
                                         )
                                 )) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            errorText.append(line);
                        }
                    }

                    errorBody = errorText.toString();
                }

                throw new Exception(
                        "HTTP " + responseCode
                                + (errorBody.isEmpty()
                                ? ""
                                : ": " + errorBody)
                );
            }

            StringBuilder responseText =
                    new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         inputStream,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }

            String jsonText = responseText.toString().trim();

            if (jsonText.isEmpty()) {
                throw new Exception(
                        "The music server returned an empty response"
                );
            }

            Log.d(TAG, "Music API response: " + jsonText);

            JSONObject response =
                    new JSONObject(jsonText);

            if (!response.optBoolean("success", false)) {

                throw new Exception(
                        response.optString(
                                "message",
                                "The music server reported a failure"
                        )
                );
            }

            JSONArray items =
                    response.optJSONArray("items");

            if (items == null) {
                throw new Exception(
                        "The server response has no items array"
                );
            }

            List<Music> serverPlaylist =
                    new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {

                JSONObject item =
                        items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String id =
                        getJsonString(item, "id", "");

                String title =
                        getJsonString(item, "title", "Untitled");

                String artist =
                        getJsonString(
                                item,
                                "artist",
                                "DD Kitchen"
                        );

                String albumArt =
                        getJsonString(
                                item,
                                "album_art_url",
                                ""
                        );

                // Support both API artwork field names.
                if (albumArt.isEmpty()) {
                    albumArt = getJsonString(
                            item,
                            "album_art",
                            ""
                    );
                }

                String audioUrl =
                        getJsonString(
                                item,
                                "audio_url",
                                ""
                        );

                // Use audio_file only if it is a full URL.
                if (audioUrl.isEmpty()) {
                    String audioFile =
                            getJsonString(
                                    item,
                                    "audio_file",
                                    ""
                            );

                    if (audioFile.startsWith("https://")
                            || audioFile.startsWith("http://")) {

                        audioUrl = audioFile;
                    }
                }

                int duration =
                        item.optInt("duration", 0);

                if (!audioUrl.isEmpty()
                        && (audioUrl.startsWith("https://")
                        || audioUrl.startsWith("http://"))) {

                    serverPlaylist.add(
                            new Music(
                                    id,
                                    title,
                                    artist,
                                    albumArt,
                                    audioUrl,
                                    duration
                            )
                    );

                } else {

                    Log.w(
                            TAG,
                            "Skipping track with invalid audio URL: "
                                    + title
                    );
                }
            }

            runOnUiThread(() -> {

                if (isFinishing() || isReleased) {
                    return;
                }

                playlist.clear();
                playlist.addAll(serverPlaylist);

                musicAdapter = new MusicAdapter(
                        MusicPlayerActivity.this,
                        playlist,
                        position -> {

                            if (position >= 0
                                    && position < playlist.size()) {

                                currentTrackIndex = position;
                                playTrack();
                            }
                        }
                );

                playlistRecycler.setAdapter(musicAdapter);

                if (playlist.isEmpty()) {

                    trackTitle.setText("No music available");
                    trackArtist.setText(
                            "Please check back later"
                    );

                    totalTime.setText("00:00");
                    currentTime.setText("00:00");

                    Toast.makeText(
                            MusicPlayerActivity.this,
                            "No playable music was found",
                            Toast.LENGTH_LONG
                    ).show();

                } else {

                    currentTrackIndex = 0;

                    showTrackInfo(playlist.get(0));

                    trackArtist.setText(
                            playlist.get(0).artist
                                    + " • "
                                    + playlist.size()
                                    + " tracks available"
                    );

                    Log.d(
                            TAG,
                            "Successfully loaded "
                                    + playlist.size()
                                    + " tracks"
                    );
                }
            });

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to load music playlist",
                    e
            );

            String errorMessage = e.getMessage();

            if (errorMessage == null
                    || errorMessage.trim().isEmpty()) {

                errorMessage =
                        e.getClass().getSimpleName();
            }

            final String displayedError = errorMessage;

            runOnUiThread(() -> {

                if (isFinishing() || isReleased) {
                    return;
                }

                trackTitle.setText("Music unavailable");

                trackArtist.setText(
                        "Unable to connect to the music library"
                );

                Toast.makeText(
                        MusicPlayerActivity.this,
                        "Music loading failed: "
                                + displayedError,
                        Toast.LENGTH_LONG
                ).show();
            });

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

    }).start();
}

private String getJsonString(
        JSONObject object,
        String key,
        String defaultValue) {

    if (object == null
            || !object.has(key)
            || object.isNull(key)) {

        return defaultValue;
    }

    String value = object.optString(key, defaultValue);

    if (value == null
            || value.trim().isEmpty()
            || "null".equalsIgnoreCase(value.trim())) {

        return defaultValue;
    }

    return value.trim();
}

private void showTrackInfo(Music track) {

    trackTitle.setText(
            track.title == null
                    || track.title.trim().isEmpty()
                    ? "Untitled"
                    : track.title
    );

    trackArtist.setText(
            track.artist == null
                    || track.artist.trim().isEmpty()
                    ? "DD Kitchen"
                    : track.artist
    );

    currentTime.setText("00:00");

    if (track.duration > 0) {

        totalTime.setText(
                formatTime(track.duration * 1000)
        );

        seekBar.setMax(track.duration * 1000);

    } else {

        totalTime.setText("--:--");
        seekBar.setMax(0);
    }

    seekBar.setProgress(0);

    if (track.albumArt != null
            && !track.albumArt.isEmpty()
            && !"null".equalsIgnoreCase(track.albumArt)) {

        Glide.with(this)
                .load(track.albumArt)
                .placeholder(
                        android.R.drawable.ic_menu_gallery
                )
                .error(
                        android.R.drawable.ic_menu_gallery
                )
                .centerCrop()
                .into(albumArt);

    } else {

        Glide.with(this).clear(albumArt);

        albumArt.setImageResource(
                android.R.drawable.ic_menu_gallery
        );
    }
}

private void playTrack() {

    if (playlist.isEmpty()
            || mediaPlayer == null
            || isReleased) {

        return;
    }

    if (currentTrackIndex < 0
            || currentTrackIndex >= playlist.size()) {

        return;
    }

    Music currentTrack =
            playlist.get(currentTrackIndex);

    showTrackInfo(currentTrack);

    isPlaying = false;
    isPreparing = true;

    playPauseBtn.setImageResource(
            android.R.drawable.ic_media_play
    );

    try {

        mediaPlayer.reset();

        mediaPlayer.setOnPreparedListener(mp -> {

            if (isReleased) {
                return;
            }

            isPreparing = false;

            mp.start();

            isPlaying = true;

            seekBar.setMax(mp.getDuration());

            totalTime.setText(
                    formatTime(mp.getDuration())
            );

            playPauseBtn.setImageResource(
                    android.R.drawable.ic_media_pause
            );

            updateSeekBar();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {

            Log.e(
                    TAG,
                    "Audio playback failed: what="
                            + what + ", extra=" + extra
            );

            isPlaying = false;
            isPreparing = false;

            playPauseBtn.setImageResource(
                    android.R.drawable.ic_media_play
            );

            Toast.makeText(
                    MusicPlayerActivity.this,
                    "Unable to play: " + currentTrack.title,
                    Toast.LENGTH_LONG
            ).show();

            return true;
        });

        mediaPlayer.setDataSource(currentTrack.audioUrl);
        mediaPlayer.prepareAsync();

    } catch (Exception e) {

        isPlaying = false;
        isPreparing = false;

        Log.e(TAG, "Error preparing audio track", e);

        Toast.makeText(
                this,
                "Could not open this track. Try another song.",
                Toast.LENGTH_LONG
        ).show();
    }
}

private void togglePlayPause() {

    if (playlist.isEmpty()
            || mediaPlayer == null
            || isReleased) {

        return;
    }

    try {

        if (isPreparing) {
            return;
        }

        if (mediaPlayer.isPlaying()) {

            mediaPlayer.pause();

            isPlaying = false;

            playPauseBtn.setImageResource(
                    android.R.drawable.ic_media_play
            );

        } else {

            if (mediaPlayer.getCurrentPosition() >= 0
                    && mediaPlayer.getDuration() > 0) {

                mediaPlayer.start();

                isPlaying = true;

                playPauseBtn.setImageResource(
                        android.R.drawable.ic_media_pause
                );

                updateSeekBar();

            } else {

                playTrack();
            }
        }

    } catch (Exception e) {

        Log.e(TAG, "Play/pause failed", e);

        playTrack();
    }
}

private void playNext() {

    if (playlist.isEmpty()) {
        return;
    }

    currentTrackIndex =
            (currentTrackIndex + 1) % playlist.size();

    playTrack();
}

private void playPrevious() {

    if (playlist.isEmpty()) {
        return;
    }

    currentTrackIndex =
            (currentTrackIndex - 1 + playlist.size())
                    % playlist.size();

    playTrack();
}

private void updateSeekBar() {

    if (mediaPlayer == null
            || isReleased
            || !isPlaying) {

        return;
    }

    try {

        if (mediaPlayer.isPlaying()) {

            int position =
                    mediaPlayer.getCurrentPosition();

            seekBar.setProgress(position);

            currentTime.setText(
                    formatTime(position)
            );

            handler.postDelayed(
                    this::updateSeekBar,
                    500
            );
        }

    } catch (Exception e) {

        Log.e(TAG, "Seek bar update failed", e);
    }
}

private String formatTime(int milliseconds) {

    int seconds = Math.max(0, milliseconds / 1000);

    int minutes = seconds / 60;

    seconds = seconds % 60;

    return String.format(
            java.util.Locale.getDefault(),
            "%02d:%02d",
            minutes,
            seconds
    );
}

@Override
protected void onDestroy() {

    isReleased = true;
    isPlaying = false;
    isPreparing = false;

    handler.removeCallbacksAndMessages(null);

    if (mediaPlayer != null) {

        try {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);

            mediaPlayer.release();

        } catch (Exception e) {

            Log.e(TAG, "Error releasing media player", e);
        }

        mediaPlayer = null;
    }

    super.onDestroy();
}

}
