package hive.android.hebron.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import hive.android.hebron.R;
import hive.android.hebron.utils.MediaPlayerWrapper;

public class BeaconPlayerActivity extends Activity implements BeaconConsumer {
    public final UUID REGION_UUID = UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D");
    public final Identifier IDENTIFIER_UUID = Identifier.fromUuid(REGION_UUID);
    protected final String TAG = "BeaconPlayerActivity";
    private BeaconManager beaconManager;
    private boolean hasBounded = false;
    private Map<String, Integer> beaconID;
    private Map<String, String> ctrlData;

    private Map<String, Integer> PTresume;
    private Map<String, Integer> BGresume;

    private Integer SCENE;

    private Integer newPOS;
    private Integer cPOS;

    private String newTRACK;

    private volatile float volume;

    private String newBGTRACK;
    private String cBGTRACK;

    private volatile Integer newBGplayer;
    private volatile Integer cBGplayer;
    private volatile Integer cPlayer;
    private volatile Integer newPlayer;

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
        ButterKnife.bind(this);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth Adapter hasn't found", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please, enable Bluetooth Adapter", Toast.LENGTH_SHORT).show();
            }
            beaconManager.bind(this);
            hasBounded = true;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hasBounded) {
            beaconManager.unbind(this);
            hasBounded = false;
        }
    }

    @OnClick(R.id.resetButton)
    public void onResetClick(View view) {
        for (MediaPlayerWrapper player : audio) {
            if (player.isPlaying()) player.stop();
            player.reset();
        }
        createSystemInitialState();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                List<Beacon> beacons = new ArrayList<Beacon>(collection);
                if (beacons.size() == 0) {
                    return;
                }

                Beacon beacon = null;
                double distance = 0;

                for (Beacon current : beacons) {
                    if (current.getDistance() != -1.0D && beaconID.get(current.getId2().toString() + current.getId3()) != null) {
                        if (distance == 0 || current.getDistance() < distance) {
                            beacon = current;
                            distance = current.getDistance();
                        }
                    }
                }
                if (beacon == null) {
                    createSystemInitialState();
                    return;
                }

                newPOS = beaconID.get(beacon.getId2().toString() + beacon.getId3());

                if (!newPOS.equals(cPOS) && !newPOS.equals(0)) {
                    newTRACK = String.format("%04d", SCENE * 100 + newPOS);
                    PTctrlGet();
                    if (ctrlrsv.equals(0) && (ctrl.contains("N") || ctrl.contains("P") || ctrl.contains("S") || ctrl.contains("V"))) {
                        ctrlrsv = 1;
                        return;
                    } else {
                        ctrlrsv = 0;
                    }

                    caseCtrlContainsP();
                    caseCtrlContainsS();
                    caseCtrlContainsV();

                    if (ctrl.contains("E")) {
                        cPOS = newPOS;
                        return;
                    }

                    caseCtrlContainsN();
                    play(newTRACK);
                    cPOS = newPOS;
                }
            }
        });

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
            }
        });

        try {
            AssetFileDescriptor raw = getAssets().openFd("raw/initial.mp3");
            audio.get(0).setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    mediaPlayer.setVolume(1, 1);
                }
            });
            audio.get(0).setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
            audio.get(0).prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("EstimoteRegion", IDENTIFIER_UUID, null, null));
        } catch (RemoteException e) {
            Log.d(TAG, "Start monitoring beacons");
        }
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

    private void play(final String nextTrack) {
        if (audio.get(cPlayer).isPlaying()) PTresume.put(audio.get(cPlayer).getCurrentTrack(), audio.get(cPlayer).getCurrentPosition() - 2000);
        else PTresume.put(audio.get(cPlayer).getCurrentTrack(), 0);

        Runnable createCallback = new Runnable() {
            @Override
            public void run() {
                fadeOut((float) 0, 1000, audio.get(cPlayer), null);
                try {
                    final MediaPlayerWrapper mediaPlayer = audio.get(newPlayer);
                    playMedia(1, mediaPlayer, "raw/" + nextTrack + ".mp3", new Runnable() {
                        @Override
                        public void run() {
                            PTfadein(mediaPlayer.getCurrentTrack(), newPlayer);
                        }
                    });

                    if (cPlayer == 1) {
                        cPlayer = 2;
                        newPlayer = 1;
                    } else {
                        cPlayer = 1;
                        newPlayer = 2;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        fadeOut((float) 0.2, 800, audio.get(cPlayer), createCallback);
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
                            if (cBGplayer == 4) {
                                cBGplayer = 3;
                                newBGplayer = 4;
                            } else {
                                cBGplayer = 4;
                                newBGplayer = 3;
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
        mediaPlayer.setCurrentTrack(media);
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
        newTRACK = "0000";
        volume = (float) 1.0;
        newBGTRACK = "00";
        cBGTRACK = "00";
        newBGplayer = 3;
        cBGplayer = 4;
        cPlayer = 1;
        newPlayer = 2;
        ctrl = "Y";
        ctrlrsv = 7;

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

        audio = new ArrayList<MediaPlayerWrapper>();

        for (int i = 0; i < 6; i++) {
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
        PTresume = new HashMap<String, Integer>();
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

    private void PTfadein(String track, Integer newPlayer) {
        if (PTresume.get(track) != null && PTresume.get(track) > 0) {
            MediaPlayerWrapper mediaPlayer = audio.get(newPlayer);
            if (mediaPlayer.isPlaying()) mediaPlayer.seekTo(PTresume.get(track));
        }
        fadeOut(1, 800, audio.get((newPlayer)), null);
    }

    private void BGfadein() {
        MediaPlayerWrapper mediaPlayer = audio.get(newBGplayer);
        if (BGresume.get(newBGTRACK) > 0) {
            if (mediaPlayer.isPlaying()) audio.get(newBGplayer).seekTo(BGresume.get(newBGTRACK));
        }
        fadeOut(volume, 800, mediaPlayer, null);
    }
}
