package hive.android.hebron;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;


public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);
        //Run with 2 seconds delay
        runBeaconPlayer(2);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Run with 2 seconds delay
        runBeaconPlayer(2);
    }

    /**
     * run BeaconPlayer after "delay" seconds
     * @param delay in seconds
     */
    private void runBeaconPlayer(int delay) {
        final Intent intent = new Intent(this, BeaconPlayerActivity.class);
        final Context context = this;
        Timer timer = new Timer("PlayerActivityTimer", false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                context.startActivity(intent);
            }
        }, delay * 1000);
    }
}
