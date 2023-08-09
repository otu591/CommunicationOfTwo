package com.alexei.communicationoftwo.model;

import androidx.room.Ignore;

public class UserChat {
    private long idContact;
    private String recipientIp;
    private String nameContact;

    public UserChat() {
    }

    @Ignore
    public UserChat(String IP, String nameContact, int idContact) {
        this.recipientIp = IP;
        this.nameContact = nameContact;
        this.idContact = idContact;
    }

    public long getIdContact() {
        return idContact;
    }

    public void setIdContact(long idContact) {
        this.idContact = idContact;
    }

    public String getRecipientIp() {
        return recipientIp;
    }

    public void setRecipientIp(String recipientIp) {
        this.recipientIp = recipientIp;
    }

    public String getNameContact() {
        return nameContact;
    }

    public void setNameContact(String nameContact) {
        this.nameContact = nameContact;
    }
}
