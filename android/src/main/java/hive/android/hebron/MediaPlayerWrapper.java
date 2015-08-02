package hive.android.hebron;

import android.media.MediaPlayer;

import java.io.FileDescriptor;
import java.io.IOException;

public class MediaPlayerWrapper {

    private MediaPlayer mediaPlayer;

    private volatile float volume = 1;

    public MediaPlayerWrapper(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        mediaPlayer.setVolume(volume, volume);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void stop() {
        mediaPlayer.stop();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void release() {
        mediaPlayer.release();
    }

    public void reset() {
        mediaPlayer.reset();
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        mediaPlayer.setOnCompletionListener(listener);
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        mediaPlayer.setOnPreparedListener(listener);
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        mediaPlayer.setOnErrorListener(listener);
    }

    public void prepare() throws IOException {
        mediaPlayer.prepare();
    }

    public void seekTo(int var1) throws IllegalStateException {
        mediaPlayer.seekTo(var1);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, IllegalStateException {
        mediaPlayer.setDataSource(fd, offset, length);
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setAudioStreamType(int var1) {
        mediaPlayer.setAudioStreamType(var1);
    }


}

