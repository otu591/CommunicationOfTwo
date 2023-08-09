package com.alexei.communicationoftwo.socket;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.model.InfoNewMail;
import com.alexei.communicationoftwo.socket.client.CommunicationNode;
import com.alexei.communicationoftwo.socket.packet.Packet;
import com.alexei.communicationoftwo.socket.server.ConnectStream;

import java.util.ArrayList;
import java.util.List;

public class ConnectionObject {


    private String currentIP;
    private int type;
    private Object communication;
    private boolean selected;
    private final long create;

    public ConnectionObject(String currentIP, int type, Object communication) {
        this.currentIP = currentIP;
        this.type = type;
        this.communication = communication;
        this.create = System.currentTimeMillis();
    }


    public long getCreate() {
        return create;
    }

    public String getCurrentIP() {
        return currentIP;
    }

    public void setCurrentIP(String currentIP) {
        this.currentIP = currentIP;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getCommunication() {
        return communication;
    }

    public void setCommunication(Object communication) {
        this.communication = communication;
    }


    public boolean isAuth() {
        if (type == Const.TYPE_CONNECT_STREAM) {
            DataContact c = ((ConnectStream) communication).getContact();
            return (c.getId() > 0);
        }
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            return (((CommunicationNode) communication).contact.getId() > 0);
        }
        return false;
    }

    public DataContact getContact() {
        if (type == Const.TYPE_CONNECT_STREAM) {
            return ((ConnectStream) communication).getContact();
        }
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            return ((CommunicationNode) communication).contact;
        }
        return null;
    }

    public void setArrivedMail(boolean b) {
        if (type == Const.TYPE_CONNECT_STREAM) {
            ((ConnectStream) communication).newMail.setArrived(b);
        }
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            ((CommunicationNode) communication).newMail.setArrived(b);
        }
    }

    public List<HistoryMessagePacket> initHistoryMessagePackets() {
        if (type == Const.TYPE_CONNECT_STREAM) {
            return ((ConnectStream) communication).initMessages();
        }
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            return ((CommunicationNode) communication).initMessages();
        }
        return new ArrayList<>();
    }

    public String getKeyAttachContact() {
        if (type == Const.TYPE_CONNECT_STREAM) {
            return ((ConnectStream) communication).getContact().getKeyReverse();
        }
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            return ((CommunicationNode) communication).contact.getKeyReverse();
        }
        return "";
    }

    public void sendPacket(Packet packet) {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).sendPacket(packet);

        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).sendRequest(packet);

    }

//    public void sendToResendPacket(Packet packet) {
//        if (type == Const.TYPE_CONNECT_STREAM)
//            ((ConnectStream) communication).runResendPacket(packet);
//
//        if (type == Const.TYPE_COMMUNICATION_NODE)
//            ((CommunicationNode) communication).runResendPacket(packet);
//
//    }

    public String getAddressHost() {
        if (type == Const.TYPE_CONNECT_STREAM)
            return ((ConnectStream) communication).addressHost;

        if (type == Const.TYPE_COMMUNICATION_NODE)
            return ((CommunicationNode) communication).contact.getIp();

        return "";
    }

    public String getContactName() {
        return (getContact() == null ? "" : getContact().getName());
    }

    public void zeroingMessages() {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).zeroingMessages();


        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).zeroingMessages();
    }

    public void initAttachContact() {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).gettingAttachContact();
    }

    public void endConversation() {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).endConversation();

        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).endConversation();
    }

    public void createVoiceThread() {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).createVoiceThread();

        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).createVoiceThread();
    }

    public void setVoiceCommReqState(byte statePhoneCall) {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).setVoiceCommReqState(statePhoneCall);

        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).setVoiceCommReqState(statePhoneCall);
    }

    public int getVoiceCommReqState() {
        if (type == Const.TYPE_CONNECT_STREAM)
            return ((ConnectStream) communication).getVoiceCommReqState();

        if (type == Const.TYPE_COMMUNICATION_NODE)
            return ((CommunicationNode) communication).getVoiceCommReqState();

        return -1;
    }

    public void deleteMessage(HistoryMessagePacket hmp) {
        if (type == Const.TYPE_CONNECT_STREAM)
            ((ConnectStream) communication).deleteMessage(hmp, ((ConnectStream) communication).messages);

        if (type == Const.TYPE_COMMUNICATION_NODE)
            ((CommunicationNode) communication).deleteMessage(hmp, ((CommunicationNode) communication).messages);
    }

    public InfoNewMail getMail() {
        if (type == Const.TYPE_CONNECT_STREAM)
            return ((ConnectStream) communication).newMail;

        if (type == Const.TYPE_COMMUNICATION_NODE)
            return ((CommunicationNode) communication).newMail;

        return null;
    }

    public List<HistoryMessagePacket> getMessages() {
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            return ((CommunicationNode) communication).messages;
        } else if (type == Const.TYPE_CONNECT_STREAM) {
            return ((ConnectStream) communication).messages;
        }
        return new ArrayList<>();
    }



    public void prepareBeforeSendPacket(Packet packet, boolean b) {
        if (type == Const.TYPE_COMMUNICATION_NODE) {
            ((CommunicationNode) communication).prepareBeforeSendPacket(packet,b);
        } else if (type == Const.TYPE_CONNECT_STREAM) {
            ((ConnectStream) communication).prepareBeforeSendPacket(packet,b);
        }
    }
}
