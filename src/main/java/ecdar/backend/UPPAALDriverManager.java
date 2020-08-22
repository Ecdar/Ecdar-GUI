package ecdar.backend;


import ecdar.Ecdar;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;

public final class UPPAALDriverManager {

    private static IUPPAALDriver instance = null;
    private static final SimpleStringProperty serverFilePath = new SimpleStringProperty(Ecdar.preferences.get("serverLocation", ""));
    private static final SimpleStringProperty verifytgaFilePath = new SimpleStringProperty(Ecdar.preferences.get("verifytgaLocation", ""));

    private UPPAALDriverManager(){}

    public static synchronized IUPPAALDriver getInstance(){

        //If the instance is null this instantiates the correct IUPPAALDriver class
        if(instance == null){
            File serverFile = new File(serverFilePath.getValue());
            File verifytgaFile = new File(verifytgaFilePath.getValue());
            if(serverFile.exists()){
                instance = new UPPAALDriver(serverFile, verifytgaFile);
            } else {
                serverFilePath.set("dummy");
                instance = new DummyUPPAALDriver();
            }
        }

        return instance;
    }

    public static SimpleStringProperty getServerFilePathProperty(){
        return serverFilePath;
    }

    public static String getServerFilePath() {
        return serverFilePath.getValue();
    }

    public static void setServerFilePath(String filePath) {
        //Set the instance to null to allow the correct UPPAALDriver to be instantiated
        //Todo: Insert check to see if the new value points to a UPPAAL server file
        instance = null;

        //Update uppaalFilePath and save the new value to preferences
        serverFilePath.set(filePath);
        Ecdar.preferences.put("serverLocation", filePath);
    }

    public static SimpleStringProperty getVerifytgaFilePathProperty(){
        return verifytgaFilePath;
    }

    public static String getVerifytgaFilePath() {
        return verifytgaFilePath.getValue();
    }

    public static void setVerifytgaFilePath(String filePath) {
        //Set the instance to null to allow the correct UPPAALDriver to be instantiated
        //Todo: Insert check to see if the new value points to a UPPAAL server file
        instance = null;

        //Update uppaalFilePath and save the new value to preferences
        verifytgaFilePath.set(filePath);
        Ecdar.preferences.put("verifytgaLocation", filePath);
    }
}