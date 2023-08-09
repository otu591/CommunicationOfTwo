package com.alexei.communicationoftwo.socket.packet;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Ignore;

import java.io.Serializable;

public class MessagePacket implements Serializable {
    private String nameSender;
    private String message;
    private long created;
    @Nullable
    @Embedded
    private AttachedFilePacket filePacket;
    private int statusAccept;

    private String keyUnique;
    private String recipientIp;


    public MessagePacket() {
    }

    @Ignore
    public MessagePacket(String nameSender, String message, long created, String keyUnique, String recipientIp) {
        this.nameSender = nameSender;
        this.message = message;
        this.created = created;
        this.keyUnique = keyUnique;
        this.recipientIp = recipientIp;

    }

    @Ignore
    public MessagePacket(String nameSender, String message, long created, @Nullable AttachedFilePacket filePacket, int statusAccept, String keyUnique, String recipientIp) {
        this.nameSender = nameSender;
        this.message = message;
        this.created = created;
        this.filePacket = filePacket;
        this.statusAccept = statusAccept;
        this.keyUnique = keyUnique;
        this.recipientIp = recipientIp;
    }

    @Ignore
    public MessagePacket(MessagePacket object) {
        this.nameSender = object.nameSender;
        this.message = object.message;
        this.created = object.created;

        AttachedFilePacket filePacket = object.filePacket;
        if (filePacket != null) {
            this.filePacket = new AttachedFilePacket(filePacket.getMimeType(),
                    filePacket.getBytesAttachedFile(),
                    filePacket.getTypeFile(),
                    filePacket.getPathFile(),
                    filePacket.getNameFile());
        }else {
            this.filePacket=null;
        }
        this.statusAccept = object.statusAccept;
        this.keyUnique = object.keyUnique;
        this.recipientIp = object.recipientIp;
    }


    public String getRecipientIp() {
        return recipientIp;
    }

    public void setRecipientIp(String recipientIp) {
        this.recipientIp = recipientIp;
    }

    public String getKeyUnique() {
        return keyUnique;
    }

    public void setKeyUnique(String keyUnique) {
        this.keyUnique = keyUnique;
    }

    public int getStatusAccept() {
        return statusAccept;
    }

    public void setStatusAccept(int statusAccept) {
        this.statusAccept = statusAccept;
    }

    @Nullable
    public AttachedFilePacket getFilePacket() {
        return filePacket;
    }

    public void setFilePacket(@Nullable AttachedFilePacket filePacket) {
        this.filePacket = filePacket;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getNameSender() {
        return nameSender;
    }

    public void setNameSender(String nameSender) {
        this.nameSender = nameSender;
    }
}
