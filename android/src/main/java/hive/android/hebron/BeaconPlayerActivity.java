package hive.android.hebron;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
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
    protected static final String TAG = "BeaconPlayerActivity";
    private BeaconManager beaconManager;
    Context context;
    private boolean hasBounded = false;

    public static final UUID REGION_UUID = UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D");
    public static final Identifier IDENTIFIER_UUID = Identifier.fromUuid(REGION_UUID);


    private static Map<String, Integer> beaconID;
    private static Map<String, String> ctrlData;

    private static Map<Integer, Integer> PTresume;
    private static Map<String, Integer> BGresume;
    private static Map<Integer, Integer> total;

    private static Integer SCENE = 0;

    private static Integer newPOS = 0;
    private static Integer cPOS = 0;
    private static Integer pPOS = 0;

    private static String newTRACK = "0000";
    private static String cTRACK = "0000";

    private static Double volume = 1.0;

    private static String newBGTRACK = "00";
    private static String cBGTRACK = "00";

    private static Integer newBGplayer = 10;
    private static Integer cBGplayer = 11;

    private static String ctrl = "Y";
    private static Integer ctrlrsv = 0;
    private static Integer ctrlNrsv = 0;
    private static Integer ctrlPrsv = 0;
    private static Integer ctrlSrsv = 0;

    private static Integer PTfadeoutTask = 0;
    private static List<Integer> PTfadeoutCtrler = new ArrayList<Integer>();
    private static Integer PTstopTask = 0;
    private static List<Integer> PTstopCtrler = new ArrayList<Integer>();
    private static Integer PTplayTask = 0;
    private static List<Integer> PTplayCtrler = new ArrayList<Integer>();
    private static List<String> PTplayTrack = new ArrayList<String>();
    private static Integer PTfadeinTask = 0;
    private static List<Integer> PTfadeinCtrler = new ArrayList<Integer>();
    private static List<String> PTfadeinTrack = new ArrayList<String>();
    private static Integer PTfinalTask = 0;
    private static List<String> PTfinalTrack = new ArrayList<String>();

    private static List<MediaPlayer> audio = new ArrayList<MediaPlayer>();

    static {
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
        ctrlData.put("a0001", "S");
        ctrlData.put("a0002", "P02020");
        ctrlData.put("a0003", "EV100");
        ctrlData.put("a0004", "P01100");
        ctrlData.put("a0005", "E");
        ctrlData.put("a0007", "E");
        ctrlData.put("a0009", "NS");
        ctrlData.put("a0101", "N");
        ctrlData.put("a0209", "N");
        ctrlData.put("a0301", "NP05005");
        ctrlData.put("a0401", "P05005");
        ctrlData.put("a0402", "V010");
        ctrlData.put("a0403", "V015");
        ctrlData.put("a0404", "V020");
        ctrlData.put("a0405", "V025");
        ctrlData.put("a0406", "V030");
        ctrlData.put("a0407", "V35");
        ctrlData.put("a0408", "NP06100");
        ctrlData.put("a0409", "NP06100");
        ctrlData.put("a0501", "E");
        ctrlData.put("a0502", "E");
        ctrlData.put("a0503", "E");
        ctrlData.put("a0504", "E");
        ctrlData.put("a0505", "E");
        ctrlData.put("a0506", "E");
        ctrlData.put("a0507", "E");
        ctrlData.put("a0508", "E");
        ctrlData.put("a0509", "E");

        PTresume = new HashMap<Integer, Integer>();
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

        BGresume = new HashMap<String, Integer>();
        BGresume.put("00", 0);

        total = new HashMap<Integer, Integer>();
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
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            audio.add(mediaPlayer);
        }
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
            //bound this activity for beacon monitoring
            beaconManager.bind(this);
            hasBounded = true;
        }
        View resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
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

                int ii = -1;
                int iii = 0;

                for (int i = 0; i < beacons.size(); i++) {
                    Beacon beacon = beacons.get(i);
                    if (beacon.getDistance() != -1.0D && beaconID.get(beacon.getId2().toString() + beacon.getId3()) != null && iii == 0) {
                        ii = i;  // save the first one's number
                        iii = 1; // the flag that the first one is got
                    }
                }
                if (ii == -1) {
                    return;
                }
                Beacon beacon = beacons.get(ii);

                total.put(cPOS, total.get(cPOS) + 1);

                if (beaconID.get(beacon.getId2().toString() + beacon.getId3()) == null) {
                    newPOS = 0;
                } else {
                    newPOS = beaconID.get(beacon.getId2().toString() + beacon.getId3());
                }


                if (!newPOS.equals(cPOS) && !newPOS.equals(0)) {
                    newTRACK = "a" + String.format("%04d", SCENE * 100 + newPOS);
                    PTctrlGet();
                    //wait once before scene change
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
                        cTRACK = newTRACK + "E";
                        return;
                    } else {
                        //pPOS = cPOS
                    }
                    PTctrlN();
                    PTplay();
                    cPOS = newPOS;
                    pPOS = cPOS;
                    cTRACK = newTRACK;
                } else {
                    ctrlNrsv = 0;
                }
            }
        });
        //this will be used in the future
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
            audio.get(0).prepareAsync();
//            audio.get(0).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("hebron", IDENTIFIER_UUID, null, null));
        } catch (RemoteException e) {
            Log.d(TAG, "Start monitoring beacons");
        }
    }

    private void PTctrlGet() {
        if (ctrlData.get(newTRACK) == null) {
            ctrl = "Y";
        } else {
            ctrl = ctrlData.get(newTRACK);
        }
    }

    private void PTctrlN() {
        if (ctrl.contains("N")) {
            ++SCENE;
            PTresume = new HashMap<Integer, Integer>();
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
            newTRACK = "a" + String.format("%04d", SCENE * 100 + newPOS);
        }
    }

    private void PTplay() {
        PTresume.put(pPOS, audio.get(pPOS).getCurrentPosition() / 1000 - 2);
        PTfadeoutCtrler.add(pPOS);
        PTfadeoutTask += 1;

        FadeOut(0.2, 800, audio.get(pPOS), new Runnable() {
            @Override
            public void run() {
                PTplayNew();
            }
        });

        PTplayCtrler.add(newPOS);
        PTplayTrack.add(newTRACK);
        PTplayTask += 1;
    }

    private void PTplayNew() {
        FadeOut(0, 1000, audio.get(PTfadeoutCtrler.get(0)), new Runnable() {
            @Override
            public void run() {
                PTstop();
            }
        });

        PTstopCtrler.add(PTfadeoutCtrler.get(0));
        PTstopTask += 1;
        PTfadeoutCtrler.remove(0);
        PTfadeoutTask -= 1;

        if (audio.get(PTplayCtrler.get(0)).isPlaying()) {
            audio.get(PTplayCtrler.get(0)).pause();
        }

        try {
            AssetFileDescriptor raw = getApplicationContext().getAssets().openFd("raw/" + PTplayTrack.get(0) + ".mp3");
            audio.get(PTplayCtrler.get(0)).setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
            audio.get(PTplayCtrler.get(0)).prepareAsync();
//            audio.get(PTplayCtrler.get(0)).start();
            audio.get(PTplayCtrler.get(0)).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    PTfadein();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        PTfadeinCtrler.add(PTplayCtrler.get(0));
        PTfadeinTrack.add(PTplayTrack.get(0));
        PTfadeinTask += 1;
        PTplayCtrler.remove(0);
        PTplayTrack.remove(0);
        PTplayTask -= 1;
    }

    private void PTstop() {
        if (PTstopCtrler.size() > 0) PTstopCtrler.remove(0);
        PTstopTask -= 1;
    }

    private void PTfadein() {
        if (PTresume.get(PTfadeinCtrler.get(0)) > 2) {
            if (audio.get(PTfadeinCtrler.get(0)).isPlaying()) {
                audio.get(PTfadeinCtrler.get(0)).seekTo(PTresume.get(PTfadeinCtrler.get(0)) * 1000);
            }
        }

        FadeOut(1, 800, audio.get((PTfadeinCtrler.get(0))), new Runnable() {
            @Override
            public void run() {
                PTDidPlay();
            }
        });

        PTfinalTrack.add(PTfadeinTrack.get(0));
        PTfinalTask += 1;
        PTfadeinCtrler.remove(0);
        PTfadeinTrack.remove(0);
        PTfadeinTask -= 1;
    }

    private void PTDidPlay() {
        PTfinalTrack.remove(0);
        PTfinalTask -= 1;
    }

    private void PTctrlP() {
        if (ctrl.contains("P")) {
            String str = ctrl;
            int loc = str.indexOf("P");
            newBGTRACK = mid(str, loc + 2, 2);
            volume = ((double) Integer.valueOf(mid(str, loc + 4, 3))) / 100;
            if (audio.get(cBGplayer).isPlaying()) {
                BGresume.put(cBGTRACK, audio.get(cBGplayer).getCurrentPosition() / 1000 - 2);
            } else {
                BGresume.put(cBGTRACK, 0);
            }
            if (!newBGTRACK.equals(cBGTRACK)) {
                FadeOut(0.2, 800, audio.get(cBGplayer), new Runnable() {
                    @Override
                    public void run() {
                        BGplayNew();
                    }
                });
            } else {
                FadeOut(volume, 800, audio.get(cBGplayer), new Runnable() {
                    @Override
                    public void run() {
                        BGDidFade();
                    }
                });
            }
        }
    }

    private void BGplayNew() {
        FadeOut(0, 1000, audio.get(cBGplayer), new Runnable() {
            @Override
            public void run() {
                BGstop();
            }
        });
        if (audio.get(newBGplayer).isPlaying()) {
            audio.get(newBGplayer).pause();
        }

        try {
            AssetFileDescriptor raw = getApplicationContext().getAssets().openFd("raw/abg" + newBGTRACK + ".mp3");
            audio.get(newBGplayer).setDataSource(raw.getFileDescriptor(), raw.getStartOffset(), raw.getLength());
            audio.get(newBGplayer).prepareAsync();
//            audio.get(newBGplayer).start();
            audio.get(newBGplayer).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    BGfadein();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void BGfadein() {
        if (BGresume.get(newBGTRACK) > 2) {
            if (audio.get(newBGplayer).isPlaying()) {
                audio.get(newBGplayer).seekTo(BGresume.get(newBGTRACK) * 1000);
            }
        }
        FadeOut(volume, 800, audio.get(newBGplayer), new Runnable() {
            @Override
            public void run() {
                BGDidPlay();
            }
        });
    }

    private void BGDidPlay() {
    }

    private void PTctrlS() {
        if (ctrl.contains("S")) {
            if (audio.get(cBGplayer).isPlaying()) {
                BGresume.put(cBGTRACK, audio.get(cBGplayer).getCurrentPosition() / 1000 - 2);
            } else {
                BGresume.put(cBGTRACK, 0);
            }

            FadeOut(0, 5000, audio.get(cBGplayer), new Runnable() {
                @Override
                public void run() {
                    BGDidStop();
                }
            });
        }
    }

    private void BGDidStop() {
    }

    private void PTctrlV() {
        if (ctrl.contains("V")) {
            String str = ctrl;
            int loc = str.indexOf("V");
            volume = Double.valueOf(mid(str, loc + 2, 3)) / 100;
            if (volume > 1) {
                volume = 1.0;
            }
            FadeOut(volume, 1000, audio.get(cBGplayer), new Runnable() {
                @Override
                public void run() {
                    BGDidFade();
                }
            });

        }
    }

    private void BGDidFade() {
        if (ctrl.contains("V")) {
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

    public static void FadeOut(final double volume, final double duration, final MediaPlayer audio, final Runnable task) {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private double time = duration;
            private float newVolume = 0.0f;

            @Override
            public void run() {
//                if (!audio.isPlaying())
//                    audio.start();
                // can call h again after work!
                time -= 100;
                newVolume = (float) ((volume * time) / duration);
                audio.setVolume(newVolume, newVolume);
                if (time > 0)
                    h.postDelayed(this, 100);
                else {
                    if (audio.isPlaying()) {
                        audio.pause();
                    }
                    task.run();
                }
            }
        }, 100);

    }
}
