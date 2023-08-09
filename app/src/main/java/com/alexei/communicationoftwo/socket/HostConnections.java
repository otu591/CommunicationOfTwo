package com.alexei.communicationoftwo.socket;

import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.Util;
import com.alexei.communicationoftwo.database.AppDB;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.model.InfoConnectedObject;
import com.alexei.communicationoftwo.model.UserChat;
import com.alexei.communicationoftwo.socket.client.CommNodes;
import com.alexei.communicationoftwo.socket.client.CommunicationNode;
import com.alexei.communicationoftwo.socket.server.ConnectStream;
import com.alexei.communicationoftwo.socket.server.Server;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HostConnections {
    private ExecutorService service;
    private static HostConnections instance;
    private final AppDB db;
    private final Type MAP_TYPE = new TypeToken<List<DataContact>>() {
    }.getType();

    private CommNodes commNodes;
    private final Server serverSocket;

    @Nullable
    private ConnectionObject selectedConnectionObject = null;
    @Nullable
    private InfoConnectedObject infoSelectedConnObj = null;

    public List<DataContact> contactList = new ArrayList<>();

    public List<ConnectionObject> connectionObjectList = new ArrayList<>();


    //---------------------------------

    public List<ConnectionObject> getArrivedList() {
        List<ConnectionObject> arrivedList = new ArrayList<>();
        for (ConnectionObject o : connectionObjectList) {
            if (o.getMail().isArrived()) {
                arrivedList.add(o);
            }
        }

        return arrivedList;
    }

    public void closeConnectByContact(DataContact contact, boolean clearChat) {
        commNodes.closeCommunicationNode(contact, clearChat);

    }

    public synchronized void startConnectionByContact(DataContact contact) {
        System.out.println("2 - " + Thread.currentThread().getName() + "/ " + contact.getName());
        commNodes.runCommunicationNode(contact);

    }

    @Nullable
    public ConnectionObject getSelectedConnectionObject() {
        return selectedConnectionObject;
    }


    public void setActionSelectedConnectionObject(@Nullable ConnectionObject newConnectionObj) {


        if (newConnectionObj != null) {
            dropAllSelectedConnectionObjects();

            newConnectionObj.setSelected(true);
            newConnectionObj.setArrivedMail(false);

            infoSelectedConnObj = new InfoConnectedObject(newConnectionObj.getContact().getKeyReverse(),
                    newConnectionObj.getType(),
                    newConnectionObj.getContact().getId());

            if (selectedConnectionObject != null && selectedConnectionObject != newConnectionObj)
                selectedConnectionObject.zeroingMessages();//очистка массива у прошлого соединения

            selectedConnectionObject = newConnectionObj;

            onListener.onSelectionConnectionObject(newConnectionObj);
        }

    }

    public synchronized boolean addOrChangeContactToDB(DataContact contact) {
        long res = 0;
        try {
            System.out.println("addOrChangeContactToDB 1");
            DataContact contactDb = db.getContactsDAO().getContact(contact.getId());

            if (contactDb != null) {//--есть

                contactDb.setDataContact(contact);//передаем данные полей
                //создаем файл аватарки из байтов пришедших по сети

                createAvatarContact(contactDb);//передаем объект в котором изменяются поля

                res = db.getContactsDAO().update(contactDb);

                //update с сохранением ссылки
                contact.setDataContact(contactDb);
                contactList.set(contactList.indexOf(contact), contact);
            } else { // -----------------нет

                contact.setKey(Util.generateKey());
                res = db.getContactsDAO().add(contact);
                contact.setId(res);

                contactList.add(contact);

                addOrChangeContactToDB(contact);
            }


            if (res > 0) {
                onListener.onAddOrChangeContact(contact);
            }

        } catch (Exception e) {
            System.out.println("HostConnections -> addOrChangeContact ERROR : - " + e.getMessage());
        }

        return (res > 0);
    }

    public void createAvatarContact(DataContact contact) {
        if (contact.getBytesPhoto() != null && contact.getBytesPhoto().length > 1) {
            String fPath = Util.saveFileAvatar(contact.getBytesPhoto(),
                    App.context.getFilesDir().getPath() + "/" + Const.PATH_AVATAR,
                    Const.PART_NAME_AVATAR + contact.getId());//получаем имя(id) контакта

            //подготовка окончательных данных
            contact.setBytesPhoto(null);
            contact.setPathAvatar(fPath);
        }
    }


    private List<DataContact> loadMapContacts() {

        if (contactList.size() == 0) {
            contactList.addAll(db.getContactsDAO().getContacts());
//            SharedPreferences prefs = App.context.getSharedPreferences(Const.PREFS_CONTACTS, Context.MODE_PRIVATE);
//            contactList = new Gson().fromJson(prefs.getString(Const.MAP_CONTACTS, String.valueOf(new ArrayList<DataContact>())), MAP_TYPE);
            initSomeDataContact(contactList);
        }

        return contactList;
    }

    private void initSomeDataContact(List<DataContact> contactList) {
        for (DataContact contact : contactList) {
            contact.setStatus(Const.NO_CONNECT_CONTACT_STATUS);
            contact.setRestart(false);

            File f = new File(contact.getPathAvatar());
            if (!f.exists()) {
                contact.setPathAvatar("android.resource://" + App.context.getPackageName() + "/drawable/ic_baseline_account_circle_24");
            }

            for (ConnectionObject object : connectionObjectList) {
                if (contact.getIp().equals(object.getCurrentIP())) {
                    contact.setStatus(Const.CONNECT_CONTACT_STATUS);
                    contact.setRestart(true);
                }
            }
        }
    }

    public void getMyIP() {
        serverSocket.getDeviceIP();
    }

    public void blockConnectStream(ConnectStream cs) {
        serverSocket.blockConnectStream(cs);
    }


    public OnListeners onListener;

    public void getHistoryChat(UserChat userChat) {
        List<HistoryMessagePacket> mps = db.getMessageDAO().getHMPackets(userChat.getIdContact());
        onListener.onShowHistoryChat(mps, userChat);
    }

    public void deleteMessage(HistoryMessagePacket hmp, List<HistoryMessagePacket> list) {
        //1 удаление из бд
        int countDeleted = db.getMessageDAO().delete(hmp.getMessagePacket().getCreated(), hmp.getMessagePacket().getKeyUnique(), hmp.getIdContact());
        if (countDeleted > 0 && list != null) {

            //2 удаление файла если из базы удален
            if (hmp.getMessagePacket().getFilePacket() != null) {
                File f = new File(hmp.getMessagePacket().getFilePacket().getPathFile());
                f.deleteOnExit();
            }

            int index = list.indexOf(hmp);
            if (index > -1) {
                //3 удаление из списка если из базы удален
                list.remove(index);
                onListener.onRemoveMessageHistory(index);
            }
        }
    }

    public int delChat(long idContact) {
        return db.getMessageDAO().deleteAll(idContact);
    }

    public boolean deleteContact(int position) {

//        try {
        DataContact contact = contactList.get(position);
        if (contact != null) {
            closeConnectByContact(contact, true);//1 разъеденение CommunicationNode

            dropConnectStreamByContact(contact); //2 разъеденение от ConnectStream(удаляем поток)

            db.getContactsDAO().delete(contact);   //3 удаление из базы

            delChat(contact.getId());              //4 удаление переписки из базы

            contactList.remove(position);          //5 удаление из списка

            onListener.onDeletedContact(position);
            return true;
        }
        return false;

//        } catch (Exception e) {
//            System.out.println("HostConnections -> deleteContact(DataContact contact) ERROR : - " + e.getMessage());
//        return false;
//        }

    }

    private void dropConnectStreamByContact(DataContact contact) {
        serverSocket.dropConnectionStream(contact);
    }

    @Nullable
    public DataContact getContact(long id) {
        for (DataContact c : contactList) {
            if (c.getId() == id)
                return c;
        }
        return null;
    }


    @Nullable
    public DataContact getKeyReverseContact(String keyContact) {
        for (DataContact c : contactList) {
            if (c.getKeyReverse().equals(keyContact))
                return c;
        }
        return null;
    }

    public void dropAllSelectedConnectionObjects() {
        for (ConnectionObject obj : connectionObjectList) {
            obj.setSelected(false);
        }
    }


    public synchronized ConnectionObject getConnectionObjectByAttachCommObj(Object attachCommunication) {
        System.out.println("getConnectionObjectByAttachCommObj - " + Thread.currentThread().getName());
        for (ConnectionObject c : connectionObjectList) {
            if (c.getCommunication() == attachCommunication) {
                System.out.println("getConnectionObjectByAttachCommObj - " + Thread.currentThread().getName() + "/ " + c.getContact().getName());
                return c;
            }
        }
        return null;
    }

    public void offSelectConnectionObject(@Nullable ConnectionObject selectedConnectionObject) {
        if (selectedConnectionObject != null) {
            System.out.println("offSelectConnectionObject - " + Thread.currentThread().getName() + "/ " + selectedConnectionObject.getContact().getName());

            selectedConnectionObject.setSelected(false);
            selectedConnectionObject.zeroingMessages();

        }

        infoSelectedConnObj = null;

        onListener.onOffSelectConnection();

    }

    public boolean isVoiceStateIncomingToConnObjList() {
        System.out.println("isVoiceStateIncomingToConnObjList");
        for (ConnectionObject object : connectionObjectList) {
            if (object.getVoiceCommReqState() == Const.INCOMING_PHONE_CALL) {
                return true;
            }
        }
        return false;
    }

    public List<ConnectionObject> getConnectionObjectsList() {
        return connectionObjectList;
    }

    public List<DataContact> getContactList() {
        return contactList;
    }

    public boolean isSelectConnectionFromAttachObj(Object obj) {
        if (selectedConnectionObject != null) {
            return selectedConnectionObject.getCommunication() == obj;
        }
        return false;
    }


    public interface OnListeners {

        void onConnectStream(ConnectStream stream);

        void onDisconnectStream();

        void onNewMessage(ConnectStream connectStream);

        void getIP(String sIP);

        void onDisconnectCommNode();

        void onConnectCommNode(CommunicationNode commNode);

        void onNewMessage(CommunicationNode commNode);

        void onAddOrChangeContact(DataContact contact);

        void onModifyStatusContact(DataContact contact);

        void onBlockedContact(String ip);

        void onSendPacket(ConnectStream connectStream);

        void onCommNodeSendPacket(CommunicationNode commNode);

        void onMessageAcceptCommNode(CommunicationNode commNode, int i);

        void onMessageAcceptConnectStream(ConnectStream connectStream, int i);

        void onChangeStateVoiceCommNode(CommunicationNode commNode, int state);/////

        void onChangeStateVoiceConnStream(ConnectStream connectStream, int state);//++++

        void onRemoveMessageCommNode(CommunicationNode commNode, int index);

        void onRemoveMessageConnStream(ConnectStream connectStream, int index);

        void onShowHistoryChat(List<HistoryMessagePacket> mp, UserChat userChat);

        void onRemoveMessageHistory(int index);

        void onDeletedContact(int i);

        void onRestartConnectionByContact(long contactId);

        void onAuthenticationConnectionStream(ConnectStream stream);

        void onSelectionConnectionObject(ConnectionObject object);

        void onDisconnectSelectedConnectionObject(ConnectionObject selectedConnectionObject, String name, boolean clearChat);

        void onModifyConnections();

        void onOffSelectConnection();

        void onChangeStateSpeakerphone(ConnectionObject object, boolean speakerphoneOn);
    }

    public void setOnListeners(OnListeners listener) {
        this.onListener = listener;
    }
    //-------------------------------


    public static synchronized HostConnections getInstance() {
        if (instance == null) {
            instance = new HostConnections();
        }
        return instance;
    }

    public HostConnections() {
        this.service = Executors.newFixedThreadPool(10);
        this.db = Room.databaseBuilder(App.context,
                AppDB.class,
                Const.DATABASE_NAME).fallbackToDestructiveMigration().build();
        this.serverSocket = new Server(Const.PORT_CONNECT);

        service.submit(() -> {
            loadMapContacts();
            handlerServerListener(serverSocket);
            runCommNodes();
        });

    }


    private void handlerServerListener(Server serverSocket) {

        serverSocket.setOnListener(new Server.OnListeners() {
            @Override
            public void onDropSocketListen() {//сброшен прослушиватель ServerSocket

            }

            @Override
            public void onRunServerSocket() {//запущен прослушиватель ServerSocket

            }

            @Override
            public synchronized void onModifyConnection(ConnectStream stream) {
                System.out.println("onModifyConnection - contactId = " + stream.getContact().getId() + " /  " + Thread.currentThread().getName());

                onListener.onAuthenticationConnectionStream(stream);

                ConnectionObject obj = getConnectionObjectByAttachCommObj(stream);

                if (obj != null) {
                    defLastIfSelectedConnectionObjectIsStream(stream.getContact().getKeyReverse(), obj);
                } else {
                    System.out.println("onModifyConnection - ненайдено подключение по прикркпленному communication");
                }
            }

            @Override
            public synchronized void onDisconnectionStream(ConnectStream stream, boolean clearChat) {//--Отключение клиента

                ConnectionObject connectionObject = getConnectionObjectByAttachCommObj(stream);

                if (connectionObject != null) {

                    disconnectIsSelectedConnectionObject(connectionObject, stream.getContact().getName(), clearChat);

                    modifyConnectList(Const.REMOVE, connectionObject);//сообщим в адаптер

                    onListener.onDisconnectStream();

                }

            }

            @Override
            public void onNewMessage(ConnectStream connectStream) {
                onListener.onNewMessage(connectStream);
            }

            @Override
            public void onGetIP(String sIP) {
                onListener.getIP(sIP);
            }


            @Override
            public void onDialBlockedContact(String IP) {
                onListener.onBlockedContact(IP);
            }

            @Override
            public void onSendPacket(ConnectStream connectStream) {
                onListener.onSendPacket(connectStream);
            }

            @Override
            public void onMessageAcceptConnStream(ConnectStream connectStream, int i) {
                onListener.onMessageAcceptConnectStream(connectStream, i);
            }

            @Override
            public void onChangeStateVoice(ConnectStream connectStream, int state) {//++++

                onListener.onChangeStateVoiceConnStream(connectStream, state);
            }

            @Override
            public void onRemoveMessageConnStream(ConnectStream connectStream, int index) {
                onListener.onRemoveMessageConnStream(connectStream, index);
            }

            @Override
            public synchronized void onConnection(ConnectStream stream) {
                System.out.println("onConnection - contactId = " + stream.getContact().getId() + " /  " + Thread.currentThread().getName());

                ConnectionObject newConnectionObject = new ConnectionObject(stream.getContact().getIp(), Const.TYPE_CONNECT_STREAM, stream);
//
                defLastIfSelectedConnectionObjectIsStream(stream.getContact().getKeyReverse(), newConnectionObject);//сброс чата

                modifyConnectList(Const.ADD, newConnectionObject);

                onListener.onConnectStream(stream);
            }

            @Override
            public void onChangeModeSpeakerphone(ConnectStream finalStream, boolean speakerphoneOn) {
                changeStateSpeakerphone(speakerphoneOn, finalStream);
            }


        });
        serverSocket.startLoadServer();
    }

    private void disconnectIsSelectedConnectionObject(ConnectionObject connectionObject, String nameByContact, boolean clearChat) {
        if (selectedConnectionObject == connectionObject) {
            onListener.onDisconnectSelectedConnectionObject(selectedConnectionObject, nameByContact, clearChat);
        }
    }


    private void runCommNodes() {
        commNodes = CommNodes.getInstance();
        commNodes.setOnListener(new CommNodes.OnListener() {

            @Override
            public synchronized void onDisconnect(CommunicationNode node, boolean clearChat) {
                System.out.println("7 - " + Thread.currentThread().getName() + "/ " + node.contact.getName());

                ConnectionObject connectionObject = getConnectionObjectByAttachCommObj(node);

                if (connectionObject != null) {

                    System.out.println("8 - " + Thread.currentThread().getName() + "/ " + node.contact.getName());

                    disconnectIsSelectedConnectionObject(connectionObject, node.contact.getName(), clearChat);

                    modifyConnectList(Const.REMOVE, connectionObject);

                    onListener.onDisconnectCommNode();

                }

            }

            @Override
            public void onConnected(CommunicationNode commNode) {

                ConnectionObject obj = new ConnectionObject(commNode.contact.getIp(), Const.TYPE_COMMUNICATION_NODE, commNode);

                handlerCommunicationNodeListener(commNode);

                modifyConnectList(Const.ADD, obj);

                //2 обработка по назначенному
                onListener.onConnectCommNode(commNode);

                //1 назначение
                defLastIfSelectedConnectionObjectIsNode(commNode.contact.getId(), obj);

            }


            @Override
            public void onChangeStatusContact(DataContact contact) {

                onListener.onModifyStatusContact(contact);
            }

            @Override
            public void onReStartCommunicationNode(long contactId) {

                onListener.onRestartConnectionByContact(contactId);
            }




        });
    }

    private synchronized void modifyConnectList(int action, ConnectionObject connectionObject) {
        if (action == Const.REMOVE) {
            connectionObjectList.remove(connectionObject);
        } else if (action == Const.ADD) {
            connectionObjectList.add(connectionObject);
        }
        onListener.onModifyConnections();
    }

    private synchronized void defLastIfSelectedConnectionObjectIsStream(String keyReverseContact, ConnectionObject newObj) {
        if (infoSelectedConnObj != null &&
                infoSelectedConnObj.getTypeCommunication() == Const.TYPE_CONNECT_STREAM &&
                infoSelectedConnObj.getKeyReverseContact().equals(keyReverseContact)) {

            setActionSelectedConnectionObject(newObj);
        }
    }

    private synchronized void defLastIfSelectedConnectionObjectIsNode(long contactId, ConnectionObject newObj) {
        if (infoSelectedConnObj != null &&
                infoSelectedConnObj.getTypeCommunication() == Const.TYPE_COMMUNICATION_NODE &&
                infoSelectedConnObj.getIdContact() == contactId) {

            setActionSelectedConnectionObject(newObj);
        }
    }


    private void handlerCommunicationNodeListener(CommunicationNode commNode) {
        //события от контакта(сервер) с которым соединение
        commNode.setOnListener(new CommunicationNode.OnListeners() {

            @Override
            public void onPingResponse() {

            }

            @Override
            public void onNewMessage() {
                onListener.onNewMessage(commNode);

            }

            @Override
            public void onNotifySendMsgPacket() {
                onListener.onCommNodeSendPacket(commNode);
            }

            @Override
            public void onMessageAccept(int i) {
                onListener.onMessageAcceptCommNode(commNode, i);
            }

            @Override
            public void onChangeStateVoiceCommReq(int state) {
                onListener.onChangeStateVoiceCommNode(commNode, state);
            }

            @Override
            public void onRemoveMessage(int index) {
                onListener.onRemoveMessageCommNode(commNode, index);
            }

            @Override
            public void onChangeStateSpeakerphone(boolean speakerphoneOn) {
                changeStateSpeakerphone(speakerphoneOn, commNode);
            }


        });
    }

    private void changeStateSpeakerphone(boolean speakerphoneOn, Object attachObj) {
        System.out.println("changeModeSpeakerphone 1 ");
        ConnectionObject object = getConnectionObjectByAttachCommObj(attachObj);
        if (object != null) {
            System.out.println("changeModeSpeakerphone 2 ");
            onListener.onChangeStateSpeakerphone(object, speakerphoneOn);
        }
    }

    public List<UserChat> getAllChats() {
        return db.getMessageDAO().getAllChatUsers();
    }
}
