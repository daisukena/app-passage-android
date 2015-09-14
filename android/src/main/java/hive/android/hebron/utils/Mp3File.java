package hive.android.hebron.utils;


public class Mp3File {
    private static final String CHAR_SEPARATOR = ",";
    private String fileName;
    private String filePath;

    public Mp3File(String fileName, String filePath){
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public Mp3File(String dictValue){
        String[] array = dictValue.split(CHAR_SEPARATOR);
        if(array.length == 2){
            fileName = array[0];
            filePath = array[1];
        }
    }

    public String dictValue(){
        return fileName + CHAR_SEPARATOR + filePath;
    }

    public static boolean isValidDictValue(String dictValue){
        return dictValue.contains(CHAR_SEPARATOR);
    }
}
