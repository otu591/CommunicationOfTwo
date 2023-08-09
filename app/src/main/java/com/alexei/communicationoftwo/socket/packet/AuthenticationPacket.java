package com.alexei.communicationoftwo.socket.packet;

import java.io.Serializable;

public class AuthenticationPacket implements Serializable {

    private String name;
    private String IP;
    private byte[] bytesPhoto;
    private String fileAvatar;
    private long idContact;
    private String keyContact;


    public void setPacket(AuthenticationPacket packet) {
        this.name = packet.name;
        this.IP = packet.IP;
        this.bytesPhoto = packet.bytesPhoto;
        this.fileAvatar = packet.fileAvatar;
        this.idContact = packet.idContact;
        this.keyContact = packet.keyContact;
    }

    public AuthenticationPacket(String name, String IP, byte[] bytesPhoto, String fileAvatar, long idContact,String keyContact) {
        this.name = name;
        this.IP = IP;
        this.bytesPhoto = bytesPhoto;
        this.fileAvatar = fileAvatar;
        this.idContact = idContact;
        this.keyContact = keyContact;

    }

    public String getKeyContact() {
        return keyContact;
    }

    public void setKeyContact(String keyContact) {
        this.keyContact = keyContact;
    }

    public long getIdContact() {
        return idContact;
    }

    public void setIdContact(long idContact) {
        this.idContact = idContact;
    }

    public String getFileAvatar() {
        return fileAvatar;
    }

    public void setFileAvatar(String fileAvatar) {
        this.fileAvatar = fileAvatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public byte[] getBytesPhoto() {
        return bytesPhoto;
    }

    public void setBytesPhoto(byte[] bytesPhoto) {
        this.bytesPhoto = bytesPhoto;
    }
}
