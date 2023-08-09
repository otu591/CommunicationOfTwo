package com.alexei.communicationoftwo.socket.packet;

import java.io.Serializable;

public class InfoMessagePacket implements Serializable {
    private  long create;
    private String keyUnique;
    private String recipientIp;

    public InfoMessagePacket(long create, String keyUnique, String recipientIp) {
        this.create = create;
        this.keyUnique = keyUnique;
        this.recipientIp = recipientIp;
    }

    public long getCreate() {
        return create;
    }

    public void setCreate(long create) {
        this.create = create;
    }

    public String getKeyUnique() {
        return keyUnique;
    }

    public void setKeyUnique(String keyUnique) {
        this.keyUnique = keyUnique;
    }

    public String getRecipientIp() {
        return recipientIp;
    }

    public void setRecipientIp(String recipientIp) {
        this.recipientIp = recipientIp;
    }
}
