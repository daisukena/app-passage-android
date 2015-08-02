package hive.android.hebron;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FadeOutTestActivity extends Activity {

    protected final String TAG = "FadeOutTestActivity";
    private List<MediaPlayer> audio;
    private volatile float volume;

    {
        createSystemInitialState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        MediaPlayer mediaPlayer = audio.get(0);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();


        Runnable t = new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer2 = audio.get(3);
                try {
                    AssetFileDescriptor raw = getApplicationContext().getAssets().openFd("raw/0005.mp3");
                    mediaPlayer2.setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
                    mediaPlayer2.prepare();
                    fadeOut((float) 0.8, 5000, mediaPlayer2, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            AssetFileDescriptor raw = getApplicationContext().getAssets().openFd("raw/0002.mp3");
            mediaPlayer.setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fadeOut((float) 0.0, 5000, mediaPlayer, t);

        MediaPlayer mediaPlayer2 = audio.get(1);
        try {
            AssetFileDescriptor raw2 = getApplicationContext().getAssets().openFd("raw/0003.mp3");
            mediaPlayer2.setDataSource(raw2.getFileDescriptor(), raw2.getStartOffset(), raw2.getLength());
            mediaPlayer2.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fadeOut((float) 0, 2000, mediaPlayer2, null);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void fadeOut(final float targetVolume, final float duration, final MediaPlayer audio, final Runnable task) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            private float time = duration;
            private float newVolume = volume;
            private float delta = (volume - targetVolume) / (time / 100);

            @Override
            public void run() {
                if (audio.isPlaying()) {
                    time -= 100;
                    newVolume -= delta;
                    audio.setVolume(newVolume, newVolume);
                    if (time > 0) h.postDelayed(this, 100);
                    else {
                        if (targetVolume == 0) audio.pause();
                        if (task != null) task.run();
                    }
                } else {
                    if (task != null) task.run();
                }
            }
        }, 100);
    }

    private void createSystemInitialState() {
        volume = (float) 1.0;
        audio = new ArrayList<MediaPlayer>();

        for (int i = 0; i < 10; i++) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            audio.add(mediaPlayer);
        }
    }
}
