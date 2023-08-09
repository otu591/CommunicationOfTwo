package com.alexei.communicationoftwo.socket.client;


import androidx.annotation.Nullable;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.socket.HostConnections;

import java.util.ArrayList;
import java.util.List;

public class CommNodes {

    private static CommNodes instance;
    private final List<CommunicationNode> commNodeList = new ArrayList<>();

    private final HostConnections hostConnections = HostConnections.getInstance();


    //----------------------------Listener
    private OnListener onListener;


    public interface OnListener {

        void onDisconnect(CommunicationNode node, boolean clearChat);

        void onConnected(CommunicationNode commNode);

        void onChangeStatusContact(DataContact contact);

        void onReStartCommunicationNode(long contactId);


    }

    public void setOnListener(OnListener listener) {
        this.onListener = listener;
    }

    //    -------------------------

    public static synchronized CommNodes getInstance() {
        if (instance == null) {
            instance = new CommNodes();
        }
        return instance;
    }

    public CommNodes() {

    }

    @Nullable
    public synchronized CommunicationNode getCommunicationNode(DataContact contact) {
        System.out.println("getCommunicationNode");
        for (CommunicationNode cn : commNodeList) {
            if (cn.contact.getId() == contact.getId()) {
                System.out.println("getCommunicationNode cn - " + cn.contact.getId());
                return cn;
            }
        }
        return null;
    }


    public void closeCommunicationNode(DataContact contact, boolean clearChat) {

        //получаем поток по контакту из списка запущенных
        CommunicationNode cn = getCommunicationNode(contact);
        if (cn != null) {
            System.out.println("получаем поток из списка запущенных -- есть");
            cn.closeCommNode(clearChat);

        } else {
            System.out.println("получаем поток из списка запущенных -- нет");
        }
    }


    public synchronized void runCommunicationNode(DataContact contact) {
        //провепяем есть ли объект по контакту

        if (getCommunicationNode(contact) == null) {

            notifyStatusContact(Const.CONNECTING_CONTACT_STATUS, contact);

            createCommunicationNode(contact);
        } else {
            System.out.println("getCommunicationNode есть " + contact.getName());
        }
    }

    public synchronized void notifyStatusContact(int status, DataContact contact) {

        if (changeStatusContact(status, contact)) {

            onListener.onChangeStatusContact(contact);
        }
    }


    private synchronized void createCommunicationNode(DataContact contact) {

        CommunicationNode node = new CommunicationNode(contact);

        node.setOnConnectListeners(new CommunicationNode.OnConnectListeners() {

            @Override
            public void onDisconnect(boolean clearChat) {
                //----промежуточный статус
                notifyStatusContact(Const.NO_CONNECT_CONTACT_STATUS, node.contact);

                onListener.onDisconnect(node, clearChat);

                commNodeList.remove(node);

                restartCommunicationNode(node.contact.getId());

            }

            @Override
            public void onConnected() {//установлено соединение

                notifyStatusContact(Const.CONNECT_CONTACT_STATUS, node.contact);

                commNodeList.add(node);
                onListener.onConnected(node);
            }

            @Override
            public void onRestartedByTimeout() {
                notifyStatusContact(Const.NO_CONNECT_CONTACT_STATUS, contact);
            }

            @Override
            public void onStopRestart() {
                notifyStatusContact(Const.NO_CONNECT_CONTACT_STATUS, contact);
            }
        });
//        node.start();
    }


    private boolean changeStatusContact(int status, DataContact contact) {
        if (contact.getStatus() != Const.BLOCKED_IP_CONTACT_STATUS && contact.getStatus() != Const.DROP_IP_CONTACT_STATUS) {
            contact.setStatus(status);
            return true;
        }

        return false;
    }

    private synchronized void restartCommunicationNode(long contactId) {

        DataContact contact = hostConnections.getContact(contactId);

        if (contact != null) {
            if (contact.isRestart()) {

                onListener.onReStartCommunicationNode(contact.getId());

                runCommunicationNode(contact);

            }
        }
    }

}