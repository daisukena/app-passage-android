package hive.android.hebron.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.altbeacon.beacon.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataManager {
    private static boolean DEBUG_FLAG = false;//TODO for test
    public static final Identifier DEBUG_IDENTIFIER_UUID = null;//test beacon uuid
    public static final String DEBUG_FIRST_BEACON = "345822770";//test beacon major + minor
    public static boolean isDebug(){
        return DEBUG_FLAG;
    }

    private Context context;
    private static DataManager instance = null;
    public static DataManager getInstance(Context context){
        if(instance == null){
            instance = new DataManager();
            instance.initManager(context);
        }
        return instance;
    }
    public static DataManager getInstance(){
        return instance;
    }

    private ArrayList<String> beaconIDs;
    private ArrayList<String> mp3FileNames;
    private Map<String, Integer> beaconID;
    private String project_name;
    private String project_url;
    private String project_firstbeacon;
    private Map<String, String> ctrlDatas;
    private ArrayList<String> projects;
    private boolean onsite;
    private boolean downloadcompleted;
    private int readytoPlay;

    private ArrayList<Mp3File> mp3Files;

    public ArrayList<String> getBeaconIDs() {
        return beaconIDs;
    }

    public ArrayList<String> getMp3FileNames() {
        return mp3FileNames;
    }

    public Map<String, Integer> getBeaconID() {
        return beaconID;
    }

    public String getProject_name() {
        return project_name;
    }

    public String getProject_url() {
        return project_url;
    }

    public String getProject_firstbeacon() {
        return project_firstbeacon;
    }

    public Map<String, String> getCtrlDatas() {
        return ctrlDatas;
    }

    public ArrayList<String> getProjects() {
        return projects;
    }

    public boolean isOnsite() {
        return onsite;
    }

    public boolean isDownloadcompleted() {
        return downloadcompleted;
    }

    public int getReadytoPlay() {
        return readytoPlay;
    }

    public ArrayList<Mp3File> getMp3Files() {
        return mp3Files;
    }

    public void setBeaconIDs(ArrayList<String> beaconIDs) {
        this.beaconIDs = beaconIDs;
    }

    public void setMp3FileNames(ArrayList<String> mp3FileNames) {
        this.mp3FileNames = mp3FileNames;
    }

    public void setBeaconID(Map<String, Integer> beaconID) {
        this.beaconID = beaconID;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public void setProject_url(String project_url) {
        this.project_url = project_url;
    }

    public void setProject_firstbeacon(String project_firstbeacon) {
        this.project_firstbeacon = project_firstbeacon;
    }

    public void setCtrlDatas(Map<String, String> ctrlDatas) {
        this.ctrlDatas = ctrlDatas;
    }

    public void setProjects(ArrayList<String> projects) {
        this.projects = projects;
    }

    public void setOnsite(boolean onsite) {
        this.onsite = onsite;
    }

    public void setDownloadcompleted(boolean downloadcompleted) {
        this.downloadcompleted = downloadcompleted;
    }

    public void setReadytoPlay(int readytoPlay) {
        this.readytoPlay = readytoPlay;
    }

    public void setMp3Files(ArrayList<Mp3File> mp3Files) {
        this.mp3Files = mp3Files;
    }

    private void initManager(Context context){
        this.context = context;

        this.beaconIDs = new ArrayList<String>();
        this.mp3FileNames = new ArrayList<String>();
        this.beaconID = new HashMap<String, Integer>();
        this.project_name = "";
        this.project_url = "";
        this.project_firstbeacon = "";
        this.ctrlDatas = new HashMap<String, String>();
        this.projects = new ArrayList<String>();
        this.onsite = false;
        this.downloadcompleted = false;
        this.readytoPlay = 0;
        this.mp3Files = new ArrayList<Mp3File>();

        loadManager();
    }

    static public String getCtrlData(Context context, String key){

        String returned = getInstance(context).ctrlDatas.get(key);

        if(returned == null){
            return "NULL";
        }
        return returned;

    }

    static public Mp3File getMp3File(String filename){

        ArrayList<Mp3File> mp3Files = DataManager.getInstance().getMp3Files();
        for(Mp3File mp3File : mp3Files){
            if(mp3File.getFileName().compareTo(filename) == 0){
                return mp3File;
            }
        }
        return null;
    }

    private static final String KEY_MP3FILE_COUNT = "Mp3File.Count";
    private static final String KEY_MP3FILE_DATA_FORMAT = "Mp3File%03d";
    public void saveManager(){
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();

        int count = mp3Files.size();
        edit.putInt(KEY_MP3FILE_COUNT, count);
        for(int i = 0; i < count; i++){
            edit.putString(String.format(KEY_MP3FILE_DATA_FORMAT, i), mp3Files.get(i).dictValue());
        }
        edit.commit();
    }

    public void loadManager(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int count = sp.getInt(KEY_MP3FILE_COUNT, 0);
        String dictValue;
        for(int i = 0; i < count; i++){
            dictValue = sp.getString(String.format(KEY_MP3FILE_DATA_FORMAT, i), "");
            if(Mp3File.isValidDictValue(dictValue)){
                this.mp3Files.add(new Mp3File(dictValue));
            }
        }
    }
}
