package hive.android.hebron.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import hive.android.hebron.R;
import hive.android.hebron.utils.DataManager;
import hive.android.hebron.utils.Mp3File;


interface DownloadThreadCallback{
    void onCallbackDownloded(String url, String data);
}


public class WebViewActivity extends Activity implements BeaconConsumer, RangeNotifier {

    private static final String TAG = "WebViewActivity";
    private static final String HOME_URL = "http://passagetellsproject.net/app/";
    private static final String PROJECT_HTML = "projects.html";
    WebView webView;

    private ArrayList<String> mp3FileArray;

    private String project_name;
    private String project_id;
    private String project_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        DataManager.getInstance(this);//initialize

        webView = (WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClientLocal());
        webView.setWebChromeClient(new WebChromeClient(){//for java script popup
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(WebViewActivity.this, message, Toast.LENGTH_LONG).show();
                result.confirm();
                return true;
            }
        });
        webView.loadUrl(HOME_URL);

        checkAuthorizationStatus();

        Timer timer = new Timer(false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "timer task");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Animation anim = AnimationUtils.loadAnimation(WebViewActivity.this, R.anim.fade_out);
                        findViewById(R.id.imageView).startAnimation(anim);
                        anim.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                findViewById(R.id.imageView).setVisibility(View.GONE);
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                    }
                });
            }
        }, 1000);


    }

    @Override
    public void onBackPressed() {
        if(webView != null && webView.getUrl().compareTo(HOME_URL) != 0){
            if(webView.canGoBack()){
                webView.goBack();
            }else{
                webView.loadUrl(HOME_URL);
            }
            return;//browser back
        }
        super.onBackPressed();//Pause App
    }



    private static final String SCHEME_PASSAGETELLS = "passagetells";

    private boolean errorFlag = false;
    class WebViewClientLocal extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, url);

            if(!isNetworkAvailable()){
                webView.stopLoading();
                showErrorDialog(HOME_URL);//force back start page
                return false;
            }

            //NO.1 Step1 Select project
            if(url.indexOf("/d/intro.html") > 0){
                //redirect projects.html
                webView.stopLoading();
                webView.loadUrl(HOME_URL + PROJECT_HTML);
                return false;
            }

            try{
                DataManager dataManager = DataManager.getInstance(WebViewActivity.this);

                //URL parse
                Uri uri = Uri.parse(url);
                String actionsAndParams = uri.getLastPathSegment();
                String query = uri.getQuery();
                String scheme = uri.getScheme();

                Map<String, String> map = new HashMap<String, String>();
                if(query != null) {
                    String[] urlComponents = query.split("&");

                    for (String keyValuePair : urlComponents) {
                        String[] pairComponents = keyValuePair.split("=");
                        if (pairComponents.length == 2) {
                            map.put(URLDecoder.decode(pairComponents[0], "UTF-8"),
                                    URLDecoder.decode(pairComponents[1], "UTF-8"));
                        }
                    }
                }

                if(scheme.equals(SCHEME_PASSAGETELLS)) {

                    if (actionsAndParams.contains("selectProject")) {
                        Log.d(TAG, "passagetells///selectProject action");

                        project_id = map.get("id");
                        project_name = map.get("name");

                        dataManager.setProject_name(project_name);
                        Log.d(TAG, "project_id / project_name: " + project_id + " / " + project_name);

                        project_url = HOME_URL;
                        project_url += project_name;
                        project_url += "/";

                        dataManager.setProject_url(project_url);
                        Log.d(TAG, "project_url: " + dataManager.getProject_url());

                        String fileURL = project_url;
                        fileURL += "intro.html";

//                        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.loadUrl(fileURL);

                        return false;

                    } else if (actionsAndParams.contains("download")) {
                        Log.d(TAG, "passagetells///download action");

                        webView.stopLoading();
                        doDownload();

                        return false;

                    }else if(actionsAndParams.contains("understood")){

                        dataManager.setReadytoPlay(1);
                        Log.d(TAG, "understood: " + dataManager.getReadytoPlay());

                        String fileURL = dataManager.getProject_url();
                        fileURL += "start.html";
                        Log.d(TAG, "fileURL:" + fileURL);

//                        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.loadUrl(fileURL);

                    }else if(actionsAndParams.contains("play")){

                        //move playing screen
                        showPlayerActivity();
                        return true;

                    }else if(actionsAndParams.contains("home")){

                        dataManager.setOnsite(true);
                        dataManager.setDownloadcompleted(false);
                        dataManager.setReadytoPlay(0);

                        Log.d(TAG, "home: ");
                        String fileURL = HOME_URL;
                        fileURL += PROJECT_HTML;
                        Log.d(TAG, "fileURL:" + fileURL);

//                        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.loadUrl(fileURL);

                    }

                    return false;

                }else if(scheme.equals("mozilla")){

                    String linkURL = url.replace("mozilla://", "http://");
                    doExecuteApplication(linkURL);
                    return true;

                }else if(scheme.equals("mailto") || scheme.equals("tel")){

                    doExecuteApplication(url);
                    return true;

                }else if(scheme.equals("comgooglemaps")){

                    String linkURL = url.replace("comgooglemaps://", "https://maps.google.com/maps");
                    Log.d("comgooglemaps", linkURL);
                    doExecuteApplication(linkURL);
                    return true;

                }

            }catch(Exception e){
                e.printStackTrace();
            }

            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.setVisibility(View.INVISIBLE);
            showErrorDialog(failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.setVisibility(errorFlag ? View.INVISIBLE : View.VISIBLE);
            super.onPageFinished(view, url);
        }

    }
    void showErrorDialog(String url){
        errorFlag = true;
        webView.setVisibility(View.INVISIBLE);
        final String errorUrl = url;
        AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
        alertDlg.setTitle(R.string.webview_error_dialog_title);
        alertDlg.setMessage(R.string.webview_error_dialog_message);
        alertDlg.setCancelable(false);
        alertDlg.setPositiveButton(
                "Reload",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //rebinding for network error
                        unbindBeaconManager();
                        checkBluetoothAdapter();
                        errorFlag = false;
                        webView.loadUrl(errorUrl);
                    }
                });
        alertDlg.create().show();
    }
    private boolean isNetworkAvailable(){
        ConnectivityManager cm =  (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return ( info != null && info.isConnected());
    }

    void doExecuteApplication(String url){
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }


    class DownloadThread implements Runnable {

        private String url;
        private DownloadThreadCallback callback;
        private String data;

        DownloadThread(String url, DownloadThreadCallback callback){
            this.url = url;
            this.callback = callback;
        }
        @Override
        public void run() {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                //
                HttpGet httpGet = new HttpGet(url);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                String str = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                Log.d("HTTP", "downloaded:" + url);
                Log.d("HTTP", str);

                data = str;
                callback.onCallbackDownloded(url, data);

                Log.d("HTTP", "thread finished");
            } catch(Exception e) {
                e.printStackTrace();
                showWaitDialog(false);
            }
        }
    }
    void startDownloadThread(String url, DownloadThreadCallback callback){
        Thread thread = new Thread(new DownloadThread(url, callback));
        thread.start();
    }

    private ProgressDialog waitDialog;
    void doDownload(){
        if(!isNetworkAvailable()){
            showErrorDialog(HOME_URL + PROJECT_HTML);
            return;
        }

        DataManager.getInstance(this).getCtrlDatas().clear();

        showWaitDialog(true);

        String url;
        String project_url = DataManager.getInstance(this).getProject_url();

        url = project_url + "ctrldata.json";
        startDownloadThread(url, new DownloadThreadCallback() {
            @Override
            public void onCallbackDownloded(String url, String data) {
                Log.d(TAG, "ctrldata: ");
                try {
                    Map<String, String> map = DataManager.getInstance(WebViewActivity.this).getCtrlDatas();
                    map.clear();

                    JSONObject json = new JSONObject(data);
                    Iterator it = json.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        map.put(key, (String)json.get(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showWaitDialog(false);
                }
            }
        });

        url = HOME_URL + "projects.json";
        startDownloadThread(url, new DownloadThreadCallback() {
            @Override
            public void onCallbackDownloded(String url, String data) {
                Log.d(TAG, "projects.json: ");
                try {
                    ArrayList<String> list = DataManager.getInstance(WebViewActivity.this).getProjects();
                    list.clear();

                    JSONObject json = new JSONObject(data);
                    Iterator it = json.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        list.add((String) json.get(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showWaitDialog(false);
                }
            }
        });

        url = project_url + "firstbeacon.txt";
        startDownloadThread(url, new DownloadThreadCallback() {
            @Override
            public void onCallbackDownloded(String url, String data) {
                Log.d(TAG, "firstbeacon.txt: " + data);
                DataManager.getInstance(WebViewActivity.this).setProject_firstbeacon(data);
            }
        });

        ///////////// downloading beacons (needed?)
        //beacons.json
        url = project_url + "beaconid.json";
        startDownloadThread(url, new DownloadThreadCallback() {
            @Override
            public void onCallbackDownloded(String url, String data) {
                Log.d(TAG, "beaconid.json: " + data);
                try {
                    Map<String,Integer> map = DataManager.getInstance(WebViewActivity.this).getBeaconID();
                    map.clear();
                    JSONObject json = new JSONObject(data);
                    Iterator it = json.keys();
                    while (it.hasNext()) {
                        String key = (String)it.next();
                        if(!key.equals("version")){
                            if(DataManager.isDebug() && (Integer)json.get(key) == 9){
                                map.put(DataManager.DEBUG_FIRST_BEACON, 9);
                            }else {
                                map.put(key, (Integer) json.get(key));
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showWaitDialog(false);
                } catch (Exception e){
                    e.printStackTrace();
                    showWaitDialog(false);
                }
            }
        });

        //initialize beacon
        startWaitInitialBeaconCheck();

        // mp3s
        url = String.format("%s%s/%s", HOME_URL, project_name, "mp3s.json");
        startDownloadThread(url, new DownloadThreadCallback() {
            @Override
            public void onCallbackDownloded(String url, String data) {
                Log.d(TAG, "mp3s: ");
                try {
                    ArrayList<String> list = DataManager.getInstance(WebViewActivity.this).getMp3FileNames();
                    list.clear();
                    JSONArray array = new JSONArray(data);
                    for(int i = 0; i < array.length(); i++){
                        list.add((String)array.get(i));
                    }
                    downloadMp3s(list);
                } catch (JSONException e) {
                    e.printStackTrace();
                    showWaitDialog(false);
                }

            }
        });

    }

    void downloadMp3s(ArrayList<String> mp3fileNames){

        DataManager dataManager = DataManager.getInstance(this);
        ArrayList<Mp3File> saveFiles = dataManager.getMp3Files();

        if(saveFiles != null && saveFiles.size() != 0){
            Mp3File savedFileFirst = saveFiles.get(0);
            File file = new File(savedFileFirst.getFilePath());
            if(file.exists()){
                Log.d(TAG, "The files do exist");
            }else{
                Log.d(TAG, "The files do not exist");
                saveFiles.clear();
            }
        }

        mp3FileArray = new ArrayList<String>();
        if(saveFiles != null && saveFiles.size() == 36){
            dataManager.setDownloadcompleted(true);
            gotoNextVC();
            return;
        }else if(saveFiles != null && saveFiles.size() == 0){
            mp3FileArray = mp3fileNames;
        }else{

            for(String downFile : mp3fileNames){
                Log.d(TAG, downFile);

                boolean flag = false;
                if(saveFiles != null) {
                    for (Mp3File savedFile : saveFiles) {
                        if (downFile.compareTo(savedFile.getFileName()) == 0) {
                            flag = true;
                            break;
                        }
                    }
                }

                if(!flag){
                    mp3FileArray.add(downFile);
                }
            }
        }

        showWaitDialog(true);

        if(mp3FileArray.size() != 0){
            downloadMp3File(mp3FileArray.get(0));
        }else{
            showWaitDialog(false);

            //? wait 3.0 sec
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            dataManager.setDownloadcompleted(true);
            gotoNextVC();
        }

    }

    private Handler handler = new Handler();

    void downloadMp3File(String filename){

        String urlString = String.format("%s%s", project_url, filename);
        DataManager dataManager = DataManager.getInstance();


        int responseCode;
        int BUFFER_SIZE = 2048;

        try{
            URI url = new URI(urlString);

            HttpClient hClient = new DefaultHttpClient();
            HttpGet hGet = new HttpGet();
            HttpResponse hResp;

            hClient.getParams().setParameter("http.connection.timeout", 15000);

            hGet.setURI(url);

            hResp = hClient.execute(hGet);

            responseCode = hResp.getStatusLine().getStatusCode();

            if (responseCode == HttpStatus.SC_OK) {
                String path = this.getFilesDir().getAbsolutePath() + "/" + filename;
                File file = new File(path);
                InputStream is = hResp.getEntity().getContent();
                BufferedInputStream in = new BufferedInputStream(is, BUFFER_SIZE);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, false), BUFFER_SIZE);

                byte buf[] = new byte[BUFFER_SIZE];
                int size;
                while((size = in.read(buf)) != -1) {
                    out.write(buf, 0, size);
                }
                out.flush();

                out.close();
                in.close();

                Log.d(TAG, "Successfully downloaded file to " + path);

                if(file.exists()){
                    Log.d(TAG, path + " has been downloaded");
                } else {
                    Log.d(TAG, path + " hasn't been downloaded");
                }

                dataManager.getMp3Files().add(new Mp3File(filename, path));

                mp3FileArray.remove(0);

                if(mp3FileArray.size() == 0){
                    // Finish Downloading and Goto Main VC

                    showWaitDialog(false);
                    Log.d(TAG, urlString);
                    //[_sysmsg setText:@"Finish DL"];
                    dataManager.setDownloadcompleted(true);
                    gotoNextVC();

                }else{

                    downloadMp3File(mp3FileArray.get(0));
                }

            }else if(responseCode == HttpStatus.SC_NOT_FOUND){

                throw new Exception("file was not found.");

            }else if(responseCode == HttpStatus.SC_REQUEST_TIMEOUT){

                throw new Exception("request timeout");
            }

        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG, "Error: " + e.getLocalizedMessage());

            if(mp3FileArray.size() == 0){
                // Finish Downloading and Goto Main VC
                showWaitDialog(false);
                //[_sysmsg setText:@"Finish DL (with errors)"];
                dataManager.setDownloadcompleted(true);
                gotoNextVC();
            }else{
                mp3FileArray.remove(0);
                if(mp3FileArray.size() == 0){
                    showWaitDialog(false);
                    Log.d(TAG, "Finish DL (with errors after removeObjectIndex)");
                    dataManager.setDownloadcompleted(true);
                    gotoNextVC();
                }else{
                    downloadMp3File(mp3FileArray.get(0));
                }
            }

        }
    }

    void showWaitDialog(boolean flagShow){
        final boolean flag_show = flagShow;
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (WebViewActivity.this){
                    if(flag_show) {
                        if(waitDialog == null) {
                            waitDialog = new ProgressDialog(WebViewActivity.this);
                            waitDialog.setMessage("Loading...");
                            waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            waitDialog.setCancelable(false);
                        }
                        waitDialog.show();
                    }else{
                        if(waitDialog != null){
                            waitDialog.dismiss();
                            waitDialog = null;
                        }
                    }
                }
            }
        });

    }

    void gotoNextVC(){

        handler.post(new Runnable() {
            @Override
            public void run() {
                DataManager dataManager = DataManager.getInstance();
                dataManager.saveManager();

                String fileURL = dataManager.getProject_url();
                fileURL += "slider.html";
                if (dataManager.isOnsite()) {
                    fileURL += "?onsite=1";
                }
                Log.d(TAG, fileURL);

                webView.loadUrl(fileURL);

                Log.d(TAG, "and it is done");

            }
        });
    }


    void checkAuthorizationStatus(){
    /*
        switch ([CLLocationManager authorizationStatus]) {
            case kCLAuthorizationStatusDenied:
            case kCLAuthorizationStatusRestricted:
            {
                //Device does not allowed
                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Location Service Setting" message:@"The access to location services on this app is restricted/denied. Go to Settings > Privacy > Location Services to change the setting on your phone." delegate:self cancelButtonTitle:@"OK" otherButtonTitles: @"Cancel",  null];
                [alert setTag:1];
                [alert show];
                break;
            }
            case kCLAuthorizationStatusNotDetermined:
            {
                //Asking permission
                if ([self.locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
                [self.locationManager requestWhenInUseAuthorization];
                Log.d(TAG, "requestWhenInUseAuthorization!!!");
            }
                else {
                [self.locationManager startRangingBeaconsInRegion:self.beaconRegion];
                Log.d(TAG, "startRangingBeaconsInRegion!!!");
            }

                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Location Service" message:@"Checking the availability of Location Service on the app." delegate:self cancelButtonTitle:@"OK" otherButtonTitles:  null];
                [alert setTag:2];
                [alert show];
                break;
            }
            case kCLAuthorizationStatusAuthorizedAlways:
            case kCLAuthorizationStatusAuthorizedWhenInUse:
            {
                //Start monitoring
                Log.d(TAG, "Location Service has been Authorized");
                Log.d(TAG, "Monitoring");
                break;

            }
            default:
            {
                //unknown error
                Log.d(TAG, "unknown value in authorizationStatus");

                break;
            }
        }
*/
    }

    //for Beacon

    private BeaconManager beaconManager;

    private boolean flagConnectedBeaconService = false;

    @Override
    public void onBeaconServiceConnect() {
        flagConnectedBeaconService = true;
        Log.d(TAG, "onBeaconServiceConnect");
    }

    void checkBluetoothAdapter(){
        if(beaconManager == null){
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth Adapter hasn't found", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please, enable Bluetooth Adapter", Toast.LENGTH_SHORT).show();
            }
            if(!beaconManager.isBound(this)){
                beaconManager.bind(this);
            }
        }
    }

//    private Timer timer;
    void startWaitInitialBeaconCheck(){
        startBeaconCheck();

//        timer = new Timer(false);
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                Log.d(TAG, "timer task");
//                DataManager dataManager = DataManager.getInstance();
//                if(dataManager.getBeaconID().size() > 0 && dataManager.getCtrlDatas().size() > 0){
//                    startBeaconCheck();
//                    timer.cancel();
//                    Log.d(TAG, "timer task end");
//                }
//            }
//        }, 500, 1000);
    }

    void startBeaconCheck(){
        if(!flagConnectedBeaconService){
            Log.d(TAG, "startBeaconCheck: no connection");
            return;
        }
        beaconManager.setRangeNotifier(this);

        regionRanging = BeaconPlayerActivity.startRangingBeaconsInRegion(beaconManager, regionRanging);

    }
    private Region regionRanging;

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        Log.d(TAG, "mark 3 location manager called.");
        if (beacons == null) {
            Log.d(TAG, "beacons are  null but didRange called");
        } else {
            Log.d(TAG, "beacons are NOT  null and didRange called");

            DataManager dataManager = DataManager.getInstance();
            Map<String, Integer> beaconID = dataManager.getBeaconID();

            if (beacons.size() == 0) {
                return;
            }
            Log.d(TAG, "beacons count is not 0");
            Log.d(TAG, "count:" + beacons.size());
            if (beaconID == null) {
                Log.d(TAG, "and beacon id is null");
            }
            //Log.d(TAG, "%l",[[dataManager.beaconID] count]);

            Beacon beacon = BeaconPlayerActivity.getBeacon(beacons, beaconID);
            if (beacon != null) {

                if (!dataManager.isOnsite()) {
                    dataManager.setOnsite(true);
                    //[_sysmsg setText:@"onsite status has been changed: false->true while downloading process"];
                    if (dataManager.isDownloadcompleted()) {
                        gotoNextVC();
                        //[_sysmsg setText:@"onsite status has been changed: false->true while viewing slider.html"];
                    }
                } else {
                    if (dataManager.isDownloadcompleted() && dataManager.getReadytoPlay() > 0) {
                        //[_sysmsg setText:@"ready to play"];
                        String beaconmajorminor = String.format("%s%s", beacon.getId2().toString(), beacon.getId3());
                        if(DataManager.isDebug()){
                            DataManager.getInstance().setProject_firstbeacon(DataManager.DEBUG_FIRST_BEACON);
                        }
                        Log.d(TAG, dataManager.getProject_firstbeacon());
                        Log.d(TAG, beaconmajorminor);
                        if (dataManager.getProject_firstbeacon().equals(beaconmajorminor)) {
                            if (dataManager.isDownloadcompleted() && dataManager.getReadytoPlay() > 4) {
                                //[_sysmsg setText:@"Play!!!"];

                                try {
                                    beaconManager.stopRangingBeaconsInRegion(regionRanging);
                                    regionRanging = null;
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                                //move player activity
                                showPlayerActivity();

                            } else {
                                dataManager.setReadytoPlay(dataManager.getReadytoPlay() + 1);
                                Log.d(TAG, "readytoplay value:" + dataManager.getReadytoPlay());
                            }
                        } else {
                            Log.d(TAG, "(need more value) readytoplay value:" + dataManager.getReadytoPlay());
                        }
                    } else {
                        Log.d(TAG, "not ready to play");
                    }
                }
                //Log.d(TAG, "mark 5 beacon in the ids found. set on site. ");
                String beaconmajorminor = String.format("%s%s", beacon.getId2(), beacon.getId3());
                Log.d(TAG, "beacon's major+minor: ");
                Log.d(TAG, beaconmajorminor);

            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetoothAdapter();
    }


    @Override
    protected void onPause() {
        super.onPause();
        unbindBeaconManager();
    }
    private void unbindBeaconManager(){
        if(beaconManager.isBound(this)){
            beaconManager.unbind(this);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        backProjectHtml();
        super.onActivityResult(requestCode, resultCode, data);
    }

    void showPlayerActivity(){
//        backProjectHtml();
        Intent intent = new Intent(WebViewActivity.this, BeaconPlayerActivity.class);
        WebViewActivity.this.startActivityForResult(intent, 0);
    }

    void backProjectHtml(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(HOME_URL + PROJECT_HTML);
                webView.clearHistory();
                //rebinding for network error
                unbindBeaconManager();
                checkBluetoothAdapter();
            }
        });
    }
}
