package hive.android.hebron;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseIntArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hive.android.hebron.utils.MediaPlayerWrapper;

public class TestActivity extends Activity {
    protected final String TAG = "TestActivity";
    Context context;
    private Map<String, Integer> beaconID;
    private Map<String, String> ctrlData;

    private SparseIntArray PTresume;
    private Map<String, Integer> BGresume;
    private SparseIntArray total;

    private Integer SCENE;

    private Integer newPOS;
    private Integer cPOS;
    private Integer pPOS;

    private String newTRACK;

    private volatile float volume;

    private String newBGTRACK;
    private String cBGTRACK;

    private Integer newBGplayer;
    private Integer cBGplayer;

    private String ctrl;
    private Integer ctrlrsv;

    private List<MediaPlayerWrapper> audio;

    {
        createSystemInitialState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        newPOS = 6;
        List<Integer> positions = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17);
        final Iterator<Integer> iterator = positions.iterator();

        final Handler h2 = new Handler(Looper.getMainLooper());
        h2.postDelayed(new Runnable() {
            @Override
            public void run() {
                newPOS = iterator.next();
                h2.postDelayed(this, 8000);

            }
        }, 8000);

        final Handler h1 = new Handler(Looper.getMainLooper());
        h1.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!newPOS.equals(cPOS) && !newPOS.equals(0)) {
                    newTRACK = String.format("%04d", SCENE * 100 + newPOS);
                    PTctrlGet();
                    if (ctrlrsv.equals(0) && (ctrl.contains("N") || ctrl.contains("P") || ctrl.contains("S") || ctrl.contains("V"))) {
                        ctrlrsv = 1;
                        h1.postDelayed(this, 100);
                        return;
                    } else {
                        ctrlrsv = 0;
                    }

                    caseCtrlContainsP();
                    caseCtrlContainsS();
                    caseCtrlContainsV();

                    if (ctrl.contains("E")) {
                        pPOS = cPOS;
                        cPOS = newPOS;
                        h1.postDelayed(this, 100);
                        return;
                    }

                    caseCtrlContainsN();
                    play(pPOS, newPOS, newTRACK);
                    cPOS = newPOS;
                    pPOS = cPOS;
                }
                h1.postDelayed(this, 100);
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void PTctrlGet() {
        if (ctrlData.get(newTRACK) == null) ctrl = "Y";
        else ctrl = ctrlData.get(newTRACK);
    }

    private void caseCtrlContainsN() {
        if (ctrl.contains("N")) {
            ++SCENE;
            initializeResume();
            newTRACK = String.format("%04d", SCENE * 100 + newPOS);
        }
    }

    private void play(final Integer previousPosititon, final Integer nextPosition, final String nextTrack) {
        if (audio.get(previousPosititon).isPlaying())
            PTresume.put(previousPosititon, audio.get(previousPosititon).getCurrentPosition() - 2000);

        Runnable createCallback = new Runnable() {
            @Override
            public void run() {
                fadeOut((float) 0, 1000, audio.get(previousPosititon), null);
                try {
                    MediaPlayerWrapper mediaPlayer = audio.get(nextPosition);
                    playMedia(1, mediaPlayer, "raw/" + nextTrack + ".mp3", new Runnable() {
                        @Override
                        public void run() {
                            PTfadein(nextPosition);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        fadeOut((float) 0.2, 800, audio.get(previousPosititon), createCallback);
    }

    private void caseCtrlContainsP() {
        if (!ctrl.contains("P")) return;

        int loc = ctrl.indexOf("P");
        newBGTRACK = mid(ctrl, loc + 2, 2);
        volume = ((float) Integer.valueOf(mid(ctrl, loc + 4, 3))) / 100;

        final MediaPlayerWrapper mediaPlayer = audio.get(cBGplayer);

        if (mediaPlayer.isPlaying())
            BGresume.put(cBGTRACK, mediaPlayer.getCurrentPosition() - 2000);

        if (!newBGTRACK.equals(cBGTRACK)) {
            fadeOut((float) 0.2, 800, mediaPlayer, new Runnable() {
                @Override
                public void run() {
                    fadeOut((float) 0, 1000, mediaPlayer, new Runnable() {
                        @Override
                        public void run() {
                            if (audio.get(cBGplayer).isPlaying()) {
                                audio.get(cBGplayer).stop();
                            }
                            if (cBGplayer == 27) {
                                cBGplayer = 26;
                                newBGplayer = 27;
                            } else {
                                cBGplayer = 27;
                                newBGplayer = 26;
                            }
                            cBGTRACK = newBGTRACK;
                        }
                    });
                    if (audio.get(newBGplayer).isPlaying()) audio.get(newBGplayer).stop();
                    try {
                        MediaPlayerWrapper mediaPlayer = audio.get(newBGplayer);
                        playMedia(volume, mediaPlayer, "raw/bg" + newBGTRACK + ".mp3", new Runnable() {
                            @Override
                            public void run() {
                                BGfadein();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            fadeOut(volume, 800, mediaPlayer, null);
        }
    }

    private void caseCtrlContainsS() {
        if (ctrl.contains("S")) {
            MediaPlayerWrapper mediaPlayer = audio.get(cBGplayer);
            if (mediaPlayer.isPlaying())
                BGresume.put(cBGTRACK, mediaPlayer.getCurrentPosition() - 2000);
            fadeOut(0, 5000, mediaPlayer, null);
        }
    }

    private void caseCtrlContainsV() {
        if (ctrl.contains("V")) {
            int loc = ctrl.indexOf("V");
            volume = Float.valueOf(mid(ctrl, loc + 2, 3)) / 100;
            if (volume > 1) {
                volume = (float) 1.0;
            }
            fadeOut(volume, 1000, audio.get(cBGplayer), null);
        }
    }

    public void fadeOut(final float targetVolume, final float duration, final MediaPlayerWrapper audio, final Runnable task) {
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            private float time = duration;
            private float newVolume = audio.getVolume();
            private float delta = (volume - targetVolume) / (time / 100);

            @Override
            public void run() {
                if (audio.isPlaying()) {
                    time -= 100;
                    newVolume -= delta;
                    audio.setVolume(newVolume);
                    if (time > 0) h.postDelayed(this, 100);
                    else {
                        if (targetVolume == 0 && audio.isPlaying()) {
                            audio.stop();
                            audio.reset();
                        }
                        if (task != null) task.run();
                    }
                } else {
                    if (task != null) task.run();
                }
            }
        }, 100);
    }

    private void playMedia(final float volume, MediaPlayerWrapper mediaPlayer, String media, final Runnable callback) throws IOException {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        AssetFileDescriptor raw = getApplicationContext().getAssets().openFd(media);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setVolume(volume, volume);
                callback.run();
            }
        });
        mediaPlayer.setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
        mediaPlayer.prepare();
    }

    private void createSystemInitialState() {
        SCENE = 0;
        newPOS = 0;
        cPOS = 0;
        pPOS = 0;
        newTRACK = "0000";
        volume = (float) 1.0;
        newBGTRACK = "00";
        cBGTRACK = "00";
        newBGplayer = 26;
        cBGplayer = 27;
        ctrl = "Y";
        ctrlrsv = 0;

        beaconID = new HashMap<String, Integer>();
        beaconID.put("1340030201", 1);
        beaconID.put("1340030202", 2);
        beaconID.put("1340030203", 3);
        beaconID.put("1340030204", 4);
        beaconID.put("1340030205", 5);
        beaconID.put("1340030206", 6);
        beaconID.put("1340030207", 7);
        beaconID.put("1340030208", 8);
        beaconID.put("1340030209", 9);
        beaconID.put("1340030210", 10);
        beaconID.put("1340030211", 11);
        beaconID.put("1340030212", 12);
        beaconID.put("1340030213", 13);
        beaconID.put("1340030214", 14);
        beaconID.put("1340030215", 15);
        beaconID.put("1340030216", 16);
        beaconID.put("1340030217", 17);
        beaconID.put("1340030219", 19);
        beaconID.put("1340030220", 20);
        beaconID.put("1340030221", 21);
        beaconID.put("1340030222", 22);
        beaconID.put("1340030223", 23);
        beaconID.put("1340030224", 24);

        ctrlData = new HashMap<String, String>();
        ctrlData.put("0015", "P01010");
        ctrlData.put("0016", "P02030");
        ctrlData.put("0017", "P03100");

        initializeResume();

        BGresume = new HashMap<String, Integer>();
        BGresume.put("00", 0);

        total = new SparseIntArray();
        total.put(0, 0);
        total.put(1, 0);
        total.put(2, 0);
        total.put(3, 0);
        total.put(4, 0);
        total.put(5, 0);
        total.put(6, 0);
        total.put(7, 0);
        total.put(8, 0);
        total.put(9, 0);
        total.put(10, 0);
        total.put(11, 0);
        total.put(12, 0);
        total.put(13, 0);
        total.put(14, 0);
        total.put(15, 0);
        total.put(16, 0);
        total.put(17, 0);
        total.put(18, 0);
        total.put(20, 0);
        total.put(21, 0);
        total.put(22, 0);
        total.put(23, 0);
        total.put(24, 0);
        total.put(25, 0);
        total.put(27, 0);

        audio = new ArrayList<MediaPlayerWrapper>();

        for (int i = 0; i < 28; i++) {
            MediaPlayerWrapper mediaPlayer = new MediaPlayerWrapper(new MediaPlayer());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    return false;
                }
            });
            audio.add(mediaPlayer);
        }
    }

    private void initializeResume() {
        PTresume = new SparseIntArray();
        PTresume.put(0, 0);
        PTresume.put(1, 0);
        PTresume.put(2, 0);
        PTresume.put(3, 0);
        PTresume.put(4, 0);
        PTresume.put(5, 0);
        PTresume.put(6, 0);
        PTresume.put(7, 0);
        PTresume.put(8, 0);
        PTresume.put(9, 0);
        PTresume.put(10, 0);
        PTresume.put(11, 0);
        PTresume.put(12, 0);
        PTresume.put(13, 0);
        PTresume.put(14, 0);
        PTresume.put(15, 0);
        PTresume.put(16, 0);
        PTresume.put(17, 0);
        PTresume.put(18, 0);
        PTresume.put(19, 0);
        PTresume.put(20, 0);
        PTresume.put(21, 0);
        PTresume.put(22, 0);
        PTresume.put(23, 0);
        PTresume.put(24, 0);
        PTresume.put(25, 0);
        PTresume.put(26, 0);
        PTresume.put(27, 0);
    }

    private String mid(String str, int start, int length) {
        int len = str.length();
        String buf = "";
        int i = 0;
        int j = 0;
        if (start > len) {
            buf = "";
        } else {
            if (length <= 0) {
                buf = "";
            } else {
                for (Character character : str.toCharArray()) {
                    i++;
                    if (i >= start) {
                        if (j < length) {
                            buf = buf + character;
                            j++;
                        }
                    }
                }
            }
        }
        return buf;
    }

    private void PTfadein(Integer pos) {
        if (PTresume.get(pos) > 0) {
            MediaPlayerWrapper mediaPlayer = audio.get(pos);
            if (mediaPlayer.isPlaying()) mediaPlayer.seekTo(PTresume.get(pos));
        }
        fadeOut(1, 800, audio.get((pos)), null);
    }

    private void BGfadein() {
        MediaPlayerWrapper mediaPlayer = audio.get(newBGplayer);
        if (BGresume.get(newBGTRACK) > 0) {
            if (mediaPlayer.isPlaying()) audio.get(newBGplayer).seekTo(BGresume.get(newBGTRACK));
        }
        fadeOut(volume, 800, mediaPlayer, null);
    }
}
