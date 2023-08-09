package com.alexei.communicationoftwo.database.model;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.alexei.communicationoftwo.socket.packet.MessagePacket;

@Entity(tableName = "history_messages")
public class HistoryMessagePacket {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int directionTypeMessage;
    //    @Relation
    @Embedded
    private MessagePacket messagePacket;

    private String nameContact;
    private long idContact;

    public HistoryMessagePacket(int directionTypeMessage, MessagePacket messagePacket, String nameContact, long idContact) {
        this.directionTypeMessage = directionTypeMessage;
        this.messagePacket = messagePacket;

        this.nameContact = nameContact;
        this.idContact = idContact;

    }

    public long getIdContact() {
        return idContact;
    }

    public void setIdContact(long idContact) {
        this.idContact = idContact;
    }

    public String getNameContact() {
        return nameContact;
    }

    public void setNameContact(String nameContact) {
        this.nameContact = nameContact;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return directionTypeMessage;
    }

    public void setDirectionTypeMessage(int directionTypeMessage) {
        this.directionTypeMessage = directionTypeMessage;
    }

    public int getDirectionTypeMessage() {
        return directionTypeMessage;
    }

    public MessagePacket getMessagePacket() {
        return messagePacket;
    }

    public void setMessagePacket(MessagePacket messagePacket) {
        this.messagePacket = messagePacket;
    }

    @NonNull
    @Override
    public String toString() {
        return getMessagePacket().getKeyUnique();
    }
}
