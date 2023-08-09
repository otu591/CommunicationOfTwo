package com.alexei.communicationoftwo.socket.client;


import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.Util;
import com.alexei.communicationoftwo.activity.MainActivity;
import com.alexei.communicationoftwo.database.AppDB;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.exClass.RecordVoice;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.model.InfoNewMail;
import com.alexei.communicationoftwo.socket.packet.AttachedFilePacket;
import com.alexei.communicationoftwo.socket.packet.AuthenticationPacket;
import com.alexei.communicationoftwo.socket.packet.InfoMessagePacket;
import com.alexei.communicationoftwo.socket.packet.MessagePacket;
import com.alexei.communicationoftwo.socket.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CommunicationNode {

    private ScheduledFuture<?> futureDropDialTimer;
    private ScheduledFuture<?> futureSendPingTimer;
    private ScheduledFuture<?> futureResendMsgTimer;
    private ScheduledFuture<?> futureResendPacketTimer;
    private final ScheduledExecutorService serviceTimer;

    private final AppDB db;
    private final Socket socket;
    private final ExecutorService service;

    private ObjectInputStream in; // поток чтения из сокета
    private ObjectOutputStream out; // поток чтения в сокет

    private int pingStep;

    public DataContact contact;

    private RecordVoice recordVoice;
    public List<Long> missedCalls = new ArrayList<>();
    private Map<Integer, Packet> mapResendPackets = new HashMap<>();
    private boolean isClose = false;

    @Nullable
    public List<HistoryMessagePacket> messages;
    public long timeCreate;

    private boolean bExecute;
    public InfoNewMail newMail;
    private int voiceCommReqState = Const.MISSING_PHONE;


    ///////////// OnConnectListeners -------------------------------

    public interface OnConnectListeners {

        void onDisconnect(boolean clearChat);

        void onConnected();

        void onRestartedByTimeout();

        void onStopRestart();
    }

    private OnConnectListeners onConnectListeners;
    //------------------------------


    public int getVoiceCommReqState() {
        return voiceCommReqState;
    }

    public void setVoiceCommReqState(byte statePhoneCall) {
        voiceCommReqState = statePhoneCall;
        if (onListeners != null)
            onListeners.onChangeStateVoiceCommReq(voiceCommReqState);//обновляем адаптер
    }


    public synchronized void createVoiceThread() {
        if (recordVoice == null) {

            System.out.println("CommNode createVoiceThread() 1");
//            recordVoice.mute();
//            recordVoice = null;
//
//        } else {
//
//            System.out.println("CommNode createVoiceThread() 2");
            recordVoice = new RecordVoice(socket.getInetAddress().getHostAddress());

            recordVoice.setOnListener(new RecordVoice.OnListeners() {

                @Override
                public void onStartRec() {
                    System.out.println("CommunicationNode onStartRec()");
                    setVoiceCommReqState(Const.VOICE_PHONE);
//                    voiceCommReqState = Const.VOICE_PHONE;//трубка разговор
//                    onListeners.onChangeStateVoiceCommReq(voiceCommReqState);
                }

                @Override
                public void onBreakVoice() {
                    System.out.println("CommunicationNode onBreakVoice()");
                    setVoiceCommReqState(Const.MISSING_PHONE);
//                    voiceCommReqState = Const.MISSING_PHONE;//трубка лежит
//                    onListeners.onChangeStateVoiceCommReq(voiceCommReqState);

                }

                @Override
                public void onChangeStateSpeakerphone(boolean speakerphoneOn) {
                    onListeners.onChangeStateSpeakerphone(speakerphoneOn);
                }

            });
        }
    }


    public synchronized void endConversation() {
        System.out.println("CommunicationNode endConversation()1");
        if (recordVoice != null) {

            recordVoice.mute();
            recordVoice = null;
        } else {

            setVoiceCommReqState(Const.MISSING_PHONE);
//            voiceCommReqState = Const.MISSING_PHONE;//трубка лежит
//            if (onListeners != null) {
//
//                onListeners.onChangeStateVoiceCommReq(voiceCommReqState);
//            }

        }

    }

    public synchronized void zeroingMessages() {
        if (messages != null) {
            messages.clear();
            messages = null;
        }
    }


    public void setOnConnectListeners(OnConnectListeners listener) {
        this.onConnectListeners = listener;
    }

    private OnListeners onListeners;

    public interface OnListeners {

        void onPingResponse();

        void onNewMessage();

        void onNotifySendMsgPacket();

        void onMessageAccept(int index);

        void onChangeStateVoiceCommReq(int state);

        void onRemoveMessage(int index);

        void onChangeStateSpeakerphone(boolean speakerphoneOn);
    }

    public void setOnListener(OnListeners listener) {
        this.onListeners = listener;
    }
    //-----------------------------

    public CommunicationNode(DataContact contact) {

        this.bExecute = true;
        this.serviceTimer = Executors.newScheduledThreadPool(1);

        this.db = Room.databaseBuilder(App.context,
                AppDB.class,
                Const.DATABASE_NAME).fallbackToDestructiveMigration().build();

        this.contact = contact;
        this.service = Executors.newFixedThreadPool(5);//1-in, 2-ping, 3-all...
        this.timeCreate = System.currentTimeMillis();
        this.newMail = new InfoNewMail();

        this.socket = new Socket();
        service.execute(this::started);

    }


    public synchronized List<HistoryMessagePacket> initMessages() {
        try {
            if (messages == null)//если чата небыло
                messages = db.getMessageDAO().getHMPackets(contact.getId());

        } catch (Exception e) {
            System.out.println("CommunicationNode initMessages() ERROR: " + e.getMessage());
            messages = new ArrayList<>();
        }

        return messages;
    }

//    @Override
//    public void run() {
//
//        started();
//    }

    private void started() {
        try {
            System.out.println("CommunicationNode started() - " + Thread.currentThread().getName() + "/ " + contact.getName());

            socket.connect(new InetSocketAddress(contact.getIp(), Const.PORT_CONNECT), Const.TIMEOUT_SOCKET_CONNECT);//подключение....

            out = new ObjectOutputStream(socket.getOutputStream());//отправка

            //через 10 сек
            putToMapResendPackets(new Packet(Const.ID_AUTHENTICATION,
                    new AuthenticationPacket(MainActivity.account.getName(),//по умолчанию
                            "",
                            getBytesFromFile(MainActivity.account.getPathImg()),//по умолчанию
                            "",
                            -1,
                            contact.getKey()),//передаем ключ который привязывается-сохраняется в поле-keyReverse контакта, где в первое подключение определется им сервером
                    0, "", ""));

            //отправка через 15 сек
            sendPing();

            initInputSteam();//-получение------> здесь остановка потока

        } catch (SocketException net) {
            System.out.println("networkIsUnreachable - " + net.getMessage());
            pauseThread(2000);
            closeCommNode(false);

        } catch (SocketTimeoutException timeoutException) {

            System.out.println("timeoutException - " + timeoutException.getMessage());
            if (contact.isRestart()) {
                pauseThread(Const.PAUSE_RESTART_CONNECT);
                onConnectListeners.onRestartedByTimeout();
                started();
            } else {
                closeByNoRestart();
            }

        } catch (IOException | ClassNotFoundException e) {

            System.out.println("CommunicationNode ERROR started() - " + e.getMessage());

            closeCommNode(false);
        }
    }

    private void pauseThread(int msek) {
        try {
            Thread.sleep(msek);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeByNoRestart() {
        System.out.println("closeByNoRestart() - " + Thread.currentThread().getName() + "/ " + contact.getName());

        closeStreamObjects();
        closeTimers();

        onConnectListeners.onStopRestart();
        service.shutdown();
    }

    private byte[] getBytesFromFile(String pathF) {
        byte[] bytesAccountAvatar = new byte[0];
        File f = new File(pathF);
        if (f.exists()) {
            try {
                bytesAccountAvatar = Util.getByteOfFile(Uri.fromFile(f), f.length());
            } catch (Exception e) {
                System.out.println("CommunicationNode ERROR getBytesFromFile - " + e.getMessage());
            }
        }
        return bytesAccountAvatar;
    }


    public synchronized void closeCommNode(boolean clearChat) {

        if (!isClose) {
            bExecute = false;//завершаться все циклы

            System.out.println("CommunicationNode closeCommNode()");

            closeStreamObjects();
            closeTimers();
            closeServices(clearChat);

        }
    }

    private void closeStreamObjects() {

        endConversation();

        try {
            if (out != null) out.close();

            if (in != null) in.close();

            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("CommunicationNode ERROR closeCommNode() - " + e.getMessage());
        }
    }

    private void closeServices(boolean clearChat) {
        if (onConnectListeners != null)
            onConnectListeners.onDisconnect(clearChat);
        isClose = true;
        service.shutdown();
    }

    private void closeTimers() {
        cancelDropDialTimer();

        cancelResendMessageTimer();

        cancelResendPacketTimer();

        if (futureSendPingTimer != null)
            futureSendPingTimer.cancel(true);

        if (serviceTimer != null && !serviceTimer.isShutdown())
            serviceTimer.shutdown();
    }

    private void cancelResendPacketTimer() {
        if (futureResendPacketTimer != null)
            futureResendPacketTimer.cancel(true);
    }

    private void cancelResendMessageTimer() {
        if (futureResendMsgTimer != null)
            futureResendMsgTimer.cancel(true);
    }


    private void initInputSteam() throws IOException, ClassNotFoundException {

        Packet packet = null;
        System.out.println("initInputSteam() " + Thread.currentThread().getName() + "/ " + contact.getName());

        onConnectListeners.onConnected();//добавляем в список

        in = new ObjectInputStream(socket.getInputStream());//если пусто должен блок

        while (bExecute) {
            try {

                Object obj = in.readObject();// ждем/получаем сообщения с сервера

                packet = (Packet) obj;// ждем/получаем сообщения с сервера

            } catch (OutOfMemoryError e) {
                System.out.println("CommunicationNode->initInputSteam() ERROR - OutOfMemoryError ");
                packet = onOutOfMemoryError(packet, e);
            }

            realizeReceivedPacket(packet);//--обработка пакета

        }
    }


    @Nullable
    private Packet onOutOfMemoryError(Packet packet, OutOfMemoryError e) {
        if (packet != null) {

            if (packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE || packet.getIdPacket() == Const.ID_MESSAGE) {
                // клиенту отправляем уведомление - сообщение большое
                sendRequest(createPacketResultAcceptMessage(Const.ID_RESPONSE_MESSAGE_OUT_MEMORY, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));
            }

            //переделываем и передаем сообщение для себя(в активити) о попытке получить большое сообщение
            String msg = App.context.getString(R.string.message_no_accept) + "\n" + e.getMessage();
            packet = new Packet(Const.ID_MESSAGE_SYS,
                    new MessagePacket(Const.NAME_SYSTEM, msg, System.currentTimeMillis(), Util.generateKey(), packet.getRecipientIP()),
                    0,
                    "",
                    "");
        }
        return packet;
    }

    public synchronized void sendRequest(Packet packet) {
        System.out.println("sendRequest  - " +packet.getIdPacket());

        if (!service.isShutdown()) {

            try {
                if (out != null) {

                    if (packet.getIdPacket() == Const.ID_MESSAGE || packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE) {

                        modificationSendPacketMessage(packet);

                        out.writeObject(packet);

                        out.flush(); // чистим

                        onListeners.onNotifySendMsgPacket();//информируем об отправке сообщения

                        //запускаем таймер для повторной отправки сообщения
                        runReSendMessage();

                    } else {
                        out.writeObject(packet);
                        out.flush(); // чистим
                    }

                }
            } catch (Exception e) {
                System.out.println("CommunicationNode ERROR sendRequest(Packet packet) - " + e.getMessage());
            }

        }
    }

    private void modificationSendPacketMessage(Packet packet) {
        System.out.println("CommunicationNode modificationSendPacketMessage1");
        MessagePacket mp = (MessagePacket) packet.getObject();
        AttachedFilePacket filePacket = null;

        if (mp != null) {
            if (mp.getFilePacket() != null) {
                //копия без bytes
                filePacket = new AttachedFilePacket(mp.getFilePacket().getMimeType(),
                        mp.getFilePacket().getTypeFile(),
                        mp.getFilePacket().getPathFile(),
                        mp.getFilePacket().getNameFile());
            }

            //1 сохраняем сообщение(копию) в историю
            saveMessageToDB(new MessagePacket(mp.getNameSender(),
                    mp.getMessage(),
                    mp.getCreated(),
                    filePacket,
                    mp.getStatusAccept(),
                    mp.getKeyUnique(),
                    mp.getRecipientIp()), Const.MESSAGE_TYPE_DIRECTION_OUT);

            //2 отправляем пакет клиенту
            if (mp.getFilePacket() != null) {
                //2_1 этот параметр скопирован при отправки не нужен(получатель создает файл и заполняет его путь)
                mp.getFilePacket().setPathFile("");
            }
        }
    }


    public synchronized void deleteMessage(HistoryMessagePacket hmp, List<HistoryMessagePacket> list) {

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
                onListeners.onRemoveMessage(index);
            }
        }

    }

    private boolean isExistsMessageInDb(long create, String keyObj) {
        return (db.getMessageDAO().getId(create, keyObj) > 0);
    }


    private void realizeReceivedPacket(Packet packet) {
        pingStep = 0; //сброс счетчика


        try {

            switch (packet.getIdPacket()) {
                case Const.ID_ACCEPT_RESEND_PACKET:
                    removePacketFromMap((Integer) packet.getObject());
                    break;
                case Const.ID_DROP_CONNECTION:
                    contact.setRestart(false);
                    contact.setStatus(Const.DROP_IP_CONTACT_STATUS);

                    closeCommNode(false);
                    break;
                case Const.ID_MESSAGE_SYS:
                    MessagePacket ms = (MessagePacket) packet.getObject();
                    saveMessageToDB(ms, Const.MESSAGE_TYPE_DIRECTION_IN);

                    onListeners.onNewMessage();
                    break;
                case Const.ID_MESSAGE_RESENDING:
                case Const.ID_MESSAGE_AND_ATTACHED_FILE://сообщение с файлом
                case Const.ID_MESSAGE://сообщение от сервера

                    caseMessagePacket(packet);
                    break;
//                case Const.ID_REGISTRATION_KEY:
//
//                    startWork();
//                    break;
                case Const.ID_SUCCESS_AUTHENTICATION:

                    removePacketFromMap((int) Const.ID_AUTHENTICATION);//удаление из повторной отправки

                    startWork();
                    break;
                case Const.ID_RESPONSE_MESSAGE_ACCEPT://--- сообщение получено
                    setMessageStatus(packet, Const.ACCEPT_MESSAGE_STATUS);

                    break;
                case Const.ID_RESPONSE_MESSAGE_OUT_MEMORY://--- сообщение не получено - большое
                    setMessageStatus(packet, Const.OUT_MEMORY_MESSAGE_STATUS);

                    break;
                case Const.ID_BLOCKED_IP://--- ответ - мой IP им заблокирован
                    contact.setRestart(false);
                    contact.setStatus(Const.BLOCKED_IP_CONTACT_STATUS);

                    closeCommNode(false);
                    break;
                case Const.ID_REQUEST_PING://запрашивает проверку звязи

                    sendRequest(new Packet(Const.ID_RESPONSE_PING));//ответ...
                    break;
                case Const.ID_RESPONSE_PING://--- ответ на мой запрос

//                    System.out.println("CommunicationNode ID_RESPONSE_PING");
                    break;

//***************************************************************  phone ОТВЕТ

                case Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST://1---он запрашивает со мной общение голосом

                    if (voiceCommReqState == Const.MISSING_PHONE) {
                        cancelDropDialTimer();
                        setVoiceCommReqState(Const.INCOMING_PHONE_CALL);
//                        voiceCommReqState = Const.INCOMING_PHONE_CALL;//трубка входящий звонок
//                        onListeners.onChangeStateVoiceCommReq(voiceCommReqState);//обновляем адаптер

                        sendRequest(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял
                    }

                    break;
                case Const.ID_VOICE_COMMUNICATION_REQUEST://разговор( он взял трубку)
                    cancelDropDialTimer();
                    if (voiceCommReqState == Const.OUTGOING_PHONE_CALL) {
                        createVoiceThread();//создаем канал связи
                        sendRequest(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял
                    }
                    break;
                case Const.ID_MISSING_VOICE_COMMUNICATION_REQUEST://он завершает разговор

                    if (voiceCommReqState == Const.VOICE_PHONE) {
                        endConversation();//закрываем канал у себя
                        sendRequest(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял
                    }
                    break;
                case Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST://он отменил запрос на разговор

                    if (voiceCommReqState == Const.INCOMING_PHONE_CALL) {
                        missedCalls.add(System.currentTimeMillis());
                        endConversation();//закрываем канал у себя
                        sendRequest(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял
                    }
                    break;
                case Const.ID_DROP_VOICE_COMMUNICATION_REQUEST://он сбросил

                    endConversation();//закрываем канал у себя

                    sendRequest(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял

                    break;

            }
        } catch (OutOfMemoryError e) {
            System.out.println("CommunicationNode OutOfMemoryError");
            try {
                //пробуем убрать байты предположительно большого файла
                if (packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE) {
                    MessagePacket mp = (MessagePacket) packet.getObject();
                    if (mp.getFilePacket().getBytesAttachedFile() != null) {
                        mp.getFilePacket().setBytesAttachedFile(null);
                        final String concat = mp.getMessage().concat(App.context.getString(R.string.out_of_memory));
                        mp.setMessage(concat);

                        service.submit(() -> {
                            realizeReceivedPacket(packet);
                        });
                    }
                }
            } catch (Exception ex) {
                System.out.println("CommunicationNode OutOfMemoryError->RESET  " + ex.getMessage());
            }

        } catch (Exception e) {
            System.out.println("CommunicationNode ERROR realizeReceivedPacket(Packet packet) - " + e.getMessage());
        }
    }

    private void removePacketFromMap(Integer ID) {
        if (ID != null)
            actionByMapResendPackets(Const.REMOVE, ID);
    }

    private void caseMessagePacket(Packet packet) {
        if (packet.getObject() != null) {

            if (!isExistsMessageInDb(packet.getCreate(), packet.getKeyObj())) { //сообщения в базе нет
                newMail.setArrived(true);

                MessagePacket mp = (MessagePacket) packet.getObject();

                if (mp.getFilePacket() != null && mp.getFilePacket().getBytesAttachedFile() != null) {
                    String dir = App.context.getFilesDir().getPath() + "/" + Const.PATH_STORAGE;// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

                    //путь созданного файла
                    String fPath = Util.saveFileToStore(mp.getFilePacket().getTypeFile(), mp.getFilePacket().getBytesAttachedFile(), dir);

                    mp.getFilePacket().setPathFile(fPath);
                    mp.getFilePacket().setBytesAttachedFile(null);//очистка
                }

//                      отправляем уведомление - сообщение принято
                sendRequest(createPacketResultAcceptMessage(Const.ID_RESPONSE_MESSAGE_ACCEPT, mp.getCreated(), mp.getKeyUnique(), mp.getRecipientIp()));

                saveMessageToDB(mp, Const.MESSAGE_TYPE_DIRECTION_IN);

                onListeners.onNewMessage();
            } else {//сообщения в базе есть отправляем уведомление повторно

                sendRequest(createPacketResultAcceptMessage(Const.ID_RESPONSE_MESSAGE_ACCEPT, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));//идентификация сообщения по данным в InfoMessagePacket
            }
        }
    }

    private void startWork() {
        //получили положительный ответ - останавливаем authentication

        cancelResendPacketTimer();

        runReSendMessage();

    }

    @NonNull
    private Packet createPacketResultAcceptMessage(byte idPacket, long created, String keyUnique, String recipientIp) {
        return new Packet(idPacket,
                new InfoMessagePacket(created, keyUnique, recipientIp),
                0,
                "",
                "");
    }

    private void saveMessageToDB(MessagePacket mp, int directionTypeMessage) {
        HistoryMessagePacket hMP = new HistoryMessagePacket(directionTypeMessage,
                mp,
                contact.getName(),
                contact.getId());

        //сохраняем в историю сообщение и путь созданного файла
        long id = db.getMessageDAO().add(hMP);
        hMP.setId(id);
        //сейчас чат
        if (messages != null) {
            messages.add(hMP);
        }
    }


    private void setMessageStatus(Packet packet, byte outMemoryMessageStatus) {
        if (packet.getObject() != null) {
            InfoMessagePacket imp = (InfoMessagePacket) packet.getObject();
            //изменяем в db
            setStMessageInDb(imp, outMemoryMessageStatus);
            //изменяем в чате
            setStMessageInList(imp, outMemoryMessageStatus);
        }
    }

    private void setStMessageInList(InfoMessagePacket imp, byte status) {
        if (messages != null) {
            for (HistoryMessagePacket p : messages) {
                if (p.toString().equals(imp.getKeyUnique()) && p.getMessagePacket().getCreated() == imp.getCreate()) {
                    p.getMessagePacket().setStatusAccept(status);

                    onListeners.onMessageAccept(messages.indexOf(p));
                    break;
                }
            }
        }

    }

    //статус о доставке
    private void setStMessageInDb(InfoMessagePacket imp, int status) {
        System.out.println("ConnectStream setStatusMessage 1");
        //изменяем в базе
        db.getMessageDAO().updateAcceptStatus(imp.getCreate(),
                imp.getKeyUnique(),
                contact.getId(),
                status);
    }

    private void runReSendMessage() {
        cancelResendMessageTimer();

        Runnable task = () -> {
            System.out.println("runReSendMessage() ");
            if (contact != null) {

                //получаем из базы неполученные contact.getId()-ом сообщения
                List<HistoryMessagePacket> list = db.getMessageDAO().getHMPacketNoAccepted(contact.getId(), Const.NO_ACCEPTED_MESSAGE_STATUS, Const.MESSAGE_TYPE_DIRECTION_OUT);

                if (list.size() > 0) {

                    for (HistoryMessagePacket hmp : list) {

                        //обработка(получение байты файла) прикрепленного файла если был...
                        AttachedFilePacket filePacket = hmp.getMessagePacket().getFilePacket();
                        if (filePacket != null) {

                            File f = new File(filePacket.getPathFile());

                            if (f.exists()) {

                                try {
                                    filePacket.setBytesAttachedFile(Util.getByteOfFile(Uri.fromFile(f), f.length()));

                                } catch (Exception | OutOfMemoryError e) {
                                    System.out.println("ConnectStream runReSendMessage() ERROR - " + e.getMessage());
                                    filePacket.setBytesAttachedFile(null);
                                }
                            }
                        }
                        //повторная попытка отправить...

                        sendRequest(new Packet(Const.ID_MESSAGE_RESENDING,
                                hmp.getMessagePacket(),
                                hmp.getMessagePacket().getCreated(),
                                hmp.getMessagePacket().getKeyUnique(),
                                hmp.getMessagePacket().getRecipientIp()));

                    }
                } else {

                    cancelResendMessageTimer();
                }
            } else {

                cancelResendMessageTimer();
            }
        };

        futureResendMsgTimer = serviceTimer.scheduleAtFixedRate(task, 0, 15, TimeUnit.SECONDS);
    }

    private synchronized void putToMapResendPackets(Packet packet) {
        System.out.println("Node runResendPacket()");
        mapResendPackets.put(packet.getIdPacket(), packet);
        sendRequest(packet);//отправляем сразу

        Runnable task = () -> {
            System.out.println("Node Runnable task = () -> runResendPacket()");

            actionByMapResendPackets(Const.SEND_ACTION_TO_MAPS, Const.NOT_ID_PACKET);//отправка через интервал
        };

        if (futureResendPacketTimer == null || futureResendPacketTimer.isCancelled()) {
            futureResendPacketTimer = serviceTimer.scheduleAtFixedRate(task, 10, 10, TimeUnit.SECONDS);
        }
    }

    private void requestProcessingBeforeToSend(Packet packet) {
        switch (packet.getIdPacket()) {
            case Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST:
                setVoiceCommReqState(Const.OUTGOING_PHONE_CALL);
//                voiceCommReqState = (Const.OUTGOING_PHONE_CALL);//трубка исходящий звонок
//                onListener.onChangeStateVoiceCommReq(voiceCommReqState);//обновляем адапте

                dropDialTimer();
                break;
            case Const.ID_VOICE_COMMUNICATION_REQUEST://я взял трубку
                createVoiceThread();
                break;
            case Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST://отменить вызов
            case Const.ID_MISSING_VOICE_COMMUNICATION_REQUEST://завершить разговор
            case Const.ID_DROP_VOICE_COMMUNICATION_REQUEST://сброс
                cleanMapResendPacketFromVoicePackets();
                endConversation();//закрываем канал у себя(state определяется после событий от RecordVoice)
                break;

        }
    }

    private void cleanMapResendPacketFromVoicePackets() {

        removePacketFromMap((int) Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST);
        removePacketFromMap((int) Const.ID_VOICE_COMMUNICATION_REQUEST);
//        removePacketFromMap((int) Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST);
//        removePacketFromMap((int) Const.ID_MISSING_VOICE_COMMUNICATION_REQUEST);
//        removePacketFromMap((int) Const.ID_DROP_VOICE_COMMUNICATION_REQUEST);

    }


    private synchronized void actionByMapResendPackets(int action, int idPacket) {
        switch (action) {
            case Const.SEND_ACTION_TO_MAPS:
                for (Packet p : mapResendPackets.values()) {
                    sendRequest(p);
                }
                if (mapResendPackets.size() == 0)
                    cancelResendPacketTimer();
                break;
            case Const.REMOVE:
                System.out.println("actionByMapResendPackets REMOVE");
                mapResendPackets.remove(idPacket);
                break;
        }

    }


    private void sendPing() {

        Runnable task = () -> {

            sendRequest(new Packet(Const.ID_REQUEST_PING));
            if (pingStep > 5) {//25 sec

                futureSendPingTimer.cancel(true);
                closeCommNode(false);

            } else {
                pingStep++;
            }

        };

        futureSendPingTimer = serviceTimer.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
    }

    private void dropDialTimer() {
        cancelDropDialTimer();

        Runnable task = () -> {
            removePacketFromMap((int) Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST);
            sendRequest(new Packet(Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST));//отменить вызов
            endConversation();
        };
        futureDropDialTimer = serviceTimer.schedule(task, 30, TimeUnit.SECONDS);
    }

    private void cancelDropDialTimer() {
        if (futureDropDialTimer != null)
            futureDropDialTimer.cancel(true);
    }

    public void prepareBeforeSendPacket(Packet packet, boolean bResend) {
        requestProcessingBeforeToSend(packet);//выполнием некоторые действия по пакету

        if (bResend) {
            putToMapResendPackets(packet);
        } else {
            sendRequest(packet);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("CommunicationNode finalize()");
    }
}
