package com.alexei.communicationoftwo.socket.packet;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

import java.io.Serializable;

public class AttachedFilePacket implements Serializable {
    private String mimeType;
    @Nullable
    @Ignore
    private byte[] bytesAttachedFile;
    private String typeFile;
    private String pathFile;
    private String nameFile;

    public AttachedFilePacket() {
    }

    @Ignore
    public AttachedFilePacket(String mimeType, @Nullable byte[] bytesAttachedFile, String typeFile, String pathFile, String nameFile) {
        this.mimeType = mimeType;
        this.bytesAttachedFile = bytesAttachedFile;
        this.typeFile = typeFile;
        this.pathFile = pathFile;
        this.nameFile = nameFile;
    }

    @Ignore
    public AttachedFilePacket(String mimeType, String typeFile, String pathFile, String nameFile) {
        this.mimeType = mimeType;
        this.typeFile = typeFile;
        this.pathFile = pathFile;
        this.nameFile = nameFile;
    }

    public String getNameFile() {
        return nameFile;
    }

    public void setNameFile(String nameFile) {
        this.nameFile = nameFile;
    }

    public String getPathFile() {
        return pathFile;
    }

    public void setPathFile(String pathFile) {
        this.pathFile = pathFile;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getBytesAttachedFile() {
        return bytesAttachedFile;
    }

    public void setBytesAttachedFile(@Nullable byte[] bytesAttachedFile) {
        this.bytesAttachedFile = bytesAttachedFile;
    }

    public String getTypeFile() {
        return typeFile;
    }

    public void setTypeFile(String typeFile) {
        this.typeFile = typeFile;
    }

}
