package com.alexei.communicationoftwo.model;

public class InfoConnectedObject {
    private String keyReverseContact;
    private int typeCommunication;
    private long idContact;

    public InfoConnectedObject(String keyReverseContact, int typeCommunication, long idContact) {
        this.keyReverseContact = keyReverseContact;
        this.typeCommunication = typeCommunication;
        this.idContact = idContact;
    }



    public String getKeyReverseContact() {
        return keyReverseContact;
    }

    public void setKeyReverseContact(String keyReverseContact) {
        this.keyReverseContact = keyReverseContact;
    }

    public int getTypeCommunication() {
        return typeCommunication;
    }

    public void setTypeCommunication(int typeCommunication) {
        this.typeCommunication = typeCommunication;
    }

    public long getIdContact() {
        return idContact;
    }

    public void setIdContact(long idContact) {
        this.idContact = idContact;
    }
}
