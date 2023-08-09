package com.alexei.communicationoftwo.socket.packet;


import androidx.annotation.Nullable;

import java.io.Serializable;

public class Packet implements Serializable {
    private int idPacket;
    @Nullable
    private Object object;
    private long create;
    private String keyObj;
    private String recipientIP;

    public Packet() {
    }

    public Packet(int id_packet, @Nullable Object object, long create, String keyObj, String recipientIP) {
        this.idPacket = id_packet;
        this.object = object;
        this.create = create;
        this.keyObj = keyObj;
        this.recipientIP = recipientIP;
    }

    public Packet(int id_packet, @Nullable Object object) {
        this.idPacket = id_packet;
        this.object = object;

    }

    public Packet(int id_packet) {
        this.idPacket = id_packet;
    }

    public Packet(Packet packet) {
        this.idPacket = packet.idPacket;
        this.object = packet.object;
        this.create = packet.create;
        this.keyObj = packet.keyObj;
        this.recipientIP = packet.recipientIP;
    }

    public String getRecipientIP() {
        return recipientIP;
    }

    public void setRecipientIP(String recipientIP) {
        this.recipientIP = recipientIP;
    }

    public String getKeyObj() {
        return keyObj;
    }

    public void setKeyObj(String keyObj) {
        this.keyObj = keyObj;
    }

    public long getCreate() {
        return create;
    }

    public void setCreate(long create) {
        this.create = create;
    }

    public int getIdPacket() {
        return idPacket;
    }

    public void setIdPacket(int idPacket) {
        this.idPacket = idPacket;
    }

    @Nullable
    public Object getObject() {
        return object;
    }

    public void setObject(@Nullable Object object) {
        this.object = object;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
