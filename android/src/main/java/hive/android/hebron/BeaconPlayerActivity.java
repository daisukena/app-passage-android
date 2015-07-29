package hive.android.hebron;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
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

public class BeaconPlayerActivity extends Activity implements BeaconConsumer {
    public final UUID REGION_UUID = UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D");
    public final Identifier IDENTIFIER_UUID = Identifier.fromUuid(REGION_UUID);
    protected final String TAG = "BeaconPlayerActivity";
    Context context;
    private BeaconManager beaconManager;
    private boolean hasBounded = false;
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

    private float volume;

    private String newBGTRACK;
    private String cBGTRACK;

    private Integer newBGplayer;
    private Integer cBGplayer;

    private String ctrl;
    private Integer ctrlrsv;

    private List<MediaPlayer> audio;

    {
        createSystemInitialState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        context = this;
        beaconManager = BeaconManager.getInstanceForApplication(this);

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
        View resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (MediaPlayer player : audio) {
                    if (player.isPlaying()) player.stop();
                    player.release();
                }
                createSystemInitialState();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hasBounded) {
            beaconManager.unbind(this);
            hasBounded = false;
        }
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
                for (Beacon current : beacons) {
                    if (current.getDistance() != -1.0D && beaconID.get(current.getId2().toString() + current.getId3()) != null) {
                        beacon = current;
                        break;
                    }
                }
                if (beacon == null) {
                    return;
                }
                total.put(cPOS, total.get(cPOS) + 1);

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
                    PTctrlP();
                    PTctrlS();
                    PTctrlV();

                    if (ctrl.contains("E")) {
                        pPOS = cPOS;
                        cPOS = newPOS;
                        return;
                    }

                    PTctrlN();
                    PTplay();
                    cPOS = newPOS;
                    pPOS = cPOS;
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

    private void PTctrlN() {
        if (ctrl.contains("N")) {
            ++SCENE;
            initializeResume();
            newTRACK = String.format("%04d", SCENE * 100 + newPOS);
        }
    }

    private void PTplay() {
        if (audio.get(pPOS).isPlaying()) PTresume.put(pPOS, audio.get(pPOS).getCurrentPosition() - 2000);
        else PTresume.put(pPOS, 0);

        fadeOut((float) 0.2, 800, audio.get(pPOS), new Runnable() {
            @Override
            public void run() {
                PTplayNew(newPOS, newTRACK);
            }
        });

    }

    private void PTplayNew(final Integer pos, String track) {
        try {
            MediaPlayer mediaPlayer = audio.get(pos);
            playMedia(mediaPlayer, "raw/" + track + ".mp3", new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
//                    PTfadein(pos);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void PTfadein(Integer pos) {
//        if (PTresume.get(pos) > 2) {
//            MediaPlayer mediaPlayer = audio.get(pos);
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.seekTo(PTresume.get(pos));
//            }
//        }
//        fadeOut(1, 800, audio.get((pos)), null);
//    }


    private void PTctrlP() {
        if (ctrl.contains("P")) {
            int loc = ctrl.indexOf("P");
            newBGTRACK = mid(ctrl, loc + 2, 2);
            volume = ((float) Integer.valueOf(mid(ctrl, loc + 4, 3))) / 100;

            MediaPlayer mediaPlayer = audio.get(cBGplayer);

            if (mediaPlayer.isPlaying())
                BGresume.put(cBGTRACK, mediaPlayer.getCurrentPosition() - 2000);
            else BGresume.put(cBGTRACK, 0);

            if (!newBGTRACK.equals(cBGTRACK)) {
                fadeOut((float) 0.2, 800, mediaPlayer, new Runnable() {
                    @Override
                    public void run() {
                        BGplayNew();
                    }
                });
            } else {
                fadeOut(volume, 800, mediaPlayer, null);
            }
        }
    }

    private void BGplayNew() {
        fadeOut(0, 1000, audio.get(cBGplayer), new Runnable() {
            @Override
            public void run() {
                BGstop();

                try {
                    MediaPlayer mediaPlayer = audio.get(newBGplayer);
                    playMedia(mediaPlayer, "raw/bg" + newBGTRACK + ".mp3", new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
//                            BGfadein();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void playMedia(MediaPlayer mediaPlayer, String media, MediaPlayer.OnCompletionListener completionListener) throws IOException {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        AssetFileDescriptor raw = getApplicationContext().getAssets().openFd(media);
        mediaPlayer.setOnCompletionListener(completionListener);
        mediaPlayer.setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
        mediaPlayer.prepareAsync();
    }

    private void BGstop() {
        if (audio.get(cBGplayer).isPlaying()) {
            audio.get(cBGplayer).pause();
        }
        if (cBGplayer == 11) {
            cBGplayer = 10;
            newBGplayer = 11;
        } else {
            cBGplayer = 11;
            newBGplayer = 10;
        }
        cBGTRACK = newBGTRACK;
    }

//    private void BGfadein() {
//        MediaPlayer mediaPlayer = audio.get(newBGplayer);
//        if (BGresume.get(newBGTRACK) > 2) {
//            if (mediaPlayer.isPlaying()) {
//                audio.get(newBGplayer).seekTo(BGresume.get(newBGTRACK));
//            }
//        }
//        fadeOut(volume, 800, mediaPlayer, null);
//    }


    private void PTctrlS() {
        if (ctrl.contains("S")) {
            MediaPlayer mediaPlayer = audio.get(cBGplayer);
            if (mediaPlayer.isPlaying()) {
                BGresume.put(cBGTRACK, mediaPlayer.getCurrentPosition() - 2000);
            } else {
                BGresume.put(cBGTRACK, 0);
            }

            fadeOut(0, 5000, mediaPlayer, null);
        }
    }


    private void PTctrlV() {
        if (ctrl.contains("V")) {
            int loc = ctrl.indexOf("V");
            volume = Float.valueOf(mid(ctrl, loc + 2, 3)) / 100;
            if (volume > 1) {
                volume = (float) 1.0;
            }
            fadeOut(volume, 1000, audio.get(cBGplayer), null);
        }
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

    public void fadeOut(final float givenVolume, final float duration, final MediaPlayer audio, final Runnable task) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private double time = duration;
            private float newVolume = 0.0f;

            @Override
            public void run() {
                if (audio.isPlaying()) {
                    time -= 100;
                    newVolume = (float) ((givenVolume * time) / duration);
                    audio.setVolume(newVolume, newVolume);
                    if (time > 0) h.postDelayed(this, 100);
                    else {
//                        if (givenVolume == 0 && audio.isPlaying()) audio.pause();
                        if (task != null) task.run();
                    }
                } else {
                    if (task != null) task.run();
                }
            }
        }, 100);
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
        newBGplayer = 10;
        cBGplayer = 11;
        ctrl = "Y";
        ctrlrsv = 0;

        audio = new ArrayList<MediaPlayer>();

        beaconID = new HashMap<String, Integer>();
        beaconID.put("1340038689", 1);
        beaconID.put("1340030102", 2);
        beaconID.put("1340030103", 3);
        beaconID.put("1340030104", 4);
        beaconID.put("1340030105", 5);
        beaconID.put("1340030106", 6);
        beaconID.put("1340030107", 7);
        beaconID.put("1340030108", 8);
        beaconID.put("1340010769", 9);

        ctrlData = new HashMap<String, String>();
        ctrlData.put("0001", "S");
        ctrlData.put("0002", "P02020");
        ctrlData.put("0003", "EV100");
        ctrlData.put("0004", "P01100");
        ctrlData.put("0005", "E");
        ctrlData.put("0007", "E");
        ctrlData.put("0009", "NS");
        ctrlData.put("0101", "N");
        ctrlData.put("0209", "N");
        ctrlData.put("0301", "NP05005");
        ctrlData.put("0401", "P05005");
        ctrlData.put("0402", "V010");
        ctrlData.put("0403", "V015");
        ctrlData.put("0404", "V020");
        ctrlData.put("0405", "V025");
        ctrlData.put("0406", "V030");
        ctrlData.put("0407", "V35");
        ctrlData.put("0408", "NP06100");
        ctrlData.put("0409", "NP06100");
        ctrlData.put("0501", "E");
        ctrlData.put("0502", "E");
        ctrlData.put("0503", "E");
        ctrlData.put("0504", "E");
        ctrlData.put("0505", "E");
        ctrlData.put("0506", "E");
        ctrlData.put("0507", "E");
        ctrlData.put("0508", "E");
        ctrlData.put("0509", "E");

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

        for (int i = 0; i < 12; i++) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
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
    }
}
