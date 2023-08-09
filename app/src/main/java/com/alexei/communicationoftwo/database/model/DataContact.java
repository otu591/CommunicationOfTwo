package com.alexei.communicationoftwo.database.model;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts", indices = {@Index(value = {"key","ip"}, unique = true)})
public class DataContact {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String ip;
    private String name;
    private int status;
    private String pathAvatar = "";
    private String key;
    private String KeyReverse;

    private boolean restart;
    private boolean notificationSoundOfConnection;

    @Nullable
    @Ignore
    private byte[] bytesPhoto;

    public DataContact() {
    }

    @Ignore
    public DataContact(long id, String ip, String name, int status, String pathAvatar, String key, @Nullable byte[] bytesPhoto,String KeyReverse) {
        this.id = id;
        this.ip = ip;
        this.name = name;
        this.status = status;
        this.pathAvatar = pathAvatar;
        this.key = key;
        this.bytesPhoto = bytesPhoto;
        this.KeyReverse = KeyReverse;

    }

    @Ignore
    public void setDataContact(DataContact contact) {
        if(contact!=null){
            this.id = contact.id;
            this.ip = contact.ip;
            this.name = contact.name;
//            this.status = contact.status;
//            this.restart = contact.restart;
//            this.notificationSoundOfConnection = contact.notificationSoundOfConnection;
            this.pathAvatar = contact.pathAvatar;
            this.key = contact.key;
            this.bytesPhoto = contact.bytesPhoto;
            this.KeyReverse = contact.KeyReverse;
        }
    }

    public String getKeyReverse() {
        return KeyReverse;
    }

    public void setKeyReverse(String keyReverse) {
        KeyReverse = keyReverse;
    }

    public byte[] getBytesPhoto() {
        return bytesPhoto;
    }

    public void setBytesPhoto(@Nullable byte[] bytesPhoto) {
        this.bytesPhoto = bytesPhoto;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPathAvatar() {
        return pathAvatar;
    }

    public void setPathAvatar(String pathAvatar) {
        this.pathAvatar = pathAvatar;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isRestart() {
        return restart;
    }

    public void setRestart(boolean restart) {
        this.restart = restart;
    }

    public boolean isNotificationSoundOfConnection() {
        return notificationSoundOfConnection;
    }

    public void setNotificationSoundOfConnection(boolean notificationSoundOfConnection) {
        this.notificationSoundOfConnection = notificationSoundOfConnection;
    }

}
