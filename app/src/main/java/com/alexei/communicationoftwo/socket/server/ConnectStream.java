package com.alexei.communicationoftwo.socket.server;


import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.Util;
import com.alexei.communicationoftwo.database.AppDB;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.exClass.RecordVoice;
import com.alexei.communicationoftwo.model.InfoNewMail;
import com.alexei.communicationoftwo.socket.HostConnections;
import com.alexei.communicationoftwo.socket.packet.AttachedFilePacket;
import com.alexei.communicationoftwo.socket.packet.AuthenticationPacket;
import com.alexei.communicationoftwo.socket.packet.InfoMessagePacket;
import com.alexei.communicationoftwo.socket.packet.MessagePacket;
import com.alexei.communicationoftwo.socket.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConnectStream {

    private ScheduledFuture<?> futureDialTimer;
    private ScheduledFuture<?> futureResendPacketTimer;
    private ScheduledFuture<?> futureSendPingTimer;
    private ScheduledFuture<?> futureReSendMessageTimer;
    private final ScheduledExecutorService serviceTimer;
    private final HostConnections hostConnections;
    private final Socket socket;
    private final AppDB db;
    @Nullable//ориентир чат идет или нет
    public List<HistoryMessagePacket> messages;
    public List<Long> missedCalls = new ArrayList<>();
    public long timeCreate;

    private ObjectInputStream in; // поток чтения из сокета
    private ObjectOutputStream out; // поток записи в сокет
    private final ExecutorService service;

    private Map<Integer, Packet> mapResendPackets = new HashMap<>();

    public boolean modeSpeakerphone=false;

    //  заполняются данные при идентификации
    @NonNull
    private DataContact contact;

    private RecordVoice recordVoice;

    private boolean bExecute;

    public InfoNewMail newMail;

    private int voiceCommReqState = Const.MISSING_PHONE;

    public String addressHost;

    private int pingStep;

    private boolean isClose = false;



    //-----------------------------
    public ConnectStream(Socket socket) {
        this.hostConnections = HostConnections.getInstance();
        this.bExecute = true;
        this.db = Room.databaseBuilder(App.context,
                AppDB.class,
                Const.DATABASE_NAME).fallbackToDestructiveMigration().build();
        this.socket = socket;
        this.addressHost = socket.getInetAddress().getHostAddress();
        this.timeCreate = System.currentTimeMillis();
        this.service = Executors.newFixedThreadPool(10);//1-in, 2-ping, 3-all...
        this.newMail = new InfoNewMail();
        this.serviceTimer = Executors.newScheduledThreadPool(1);

        this.contact = new DataContact(0,
                addressHost,
                "",
                Const.NO_CONNECT_CONTACT_STATUS,
                "",
                "",
                null,
                "");

        this.service.execute(this::startStream);
    }

    public List<HistoryMessagePacket> initMessages() {
        try {
            if (messages == null)//если небыло чата
                messages = db.getMessageDAO().getHMPackets(contact.getId());


        } catch (Exception e) {
            System.out.println("ConnectStream initMessages() ERROR: " + e.getMessage());
            messages = new ArrayList<>();

        }
        return messages;
    }

    public int getVoiceCommReqState() {
        return voiceCommReqState;
    }


    //------------------------------

    public void createVoiceThread() {
        if (recordVoice == null) {
            recordVoice = new RecordVoice(socket.getInetAddress().getHostAddress());
            handlerRecordVoiceListeners();
        }
    }


    private void handlerRecordVoiceListeners() {
        recordVoice.setOnListener(new RecordVoice.OnListeners() {
            @Override
            public void onStartRec() {
                setVoiceCommReqState(Const.VOICE_PHONE);
//                voiceCommReqState = Const.VOICE_PHONE;//трубка разговор
//                onListener.onChangeStateVoiceCommReq(voiceCommReqState);
            }

            @Override
            public void onBreakVoice() {
                System.out.println("ConnectStream onBreakVoice() 1");
                setVoiceCommReqState(Const.MISSING_PHONE);
//                voiceCommReqState = Const.MISSING_PHONE;//трубка лежит
//                onListener.onChangeStateVoiceCommReq(voiceCommReqState);
            }

            @Override
            public void onChangeStateSpeakerphone(boolean speakerphoneOn) {
                modeSpeakerphone = speakerphoneOn;
                onListener.onChangeStateSpeakerphone(speakerphoneOn);
            }

        });
    }

    public void endConversation() {

        if (recordVoice != null) {
            System.out.println("ConnectStream  endConversation()2");
            recordVoice.mute();
            recordVoice = null;
        } else {
            System.out.println("ConnectStream  endConversation()3");
            setVoiceCommReqState(Const.MISSING_PHONE);
//            voiceCommReqState = Const.MISSING_PHONE;//трубка лежит
//            onListener.onChangeStateVoiceCommReq(voiceCommReqState);

        }

    }

    private OnListeners onListener;

    public void zeroingMessages() {
        if (messages != null) {
            messages.clear();
            messages = null;
        }

    }



    public void setVoiceCommReqState(byte statePhoneCall) {
        voiceCommReqState = statePhoneCall;
        if (onListener != null)
            onListener.onChangeStateVoiceCommReq(voiceCommReqState);//обновляем адаптер
    }


    public interface OnListeners {
        void onNewMessage();

        void onDisconnectConnectStream(boolean clearChat);

        void onAuthenticationStream();

        void onNotifySendMsgPacket();

        void onMessageAccept(int index);

        void onChangeStateVoiceCommReq(int state);

        void onRemoveMessage(int index);

        void onChangeStateSpeakerphone(boolean speakerphoneOn);
    }

    public void setOnListener(OnListeners listener) {
        this.onListener = listener;
    }


    private void startStream() {
        try {
            System.out.println("ConnectStream  startStream()");

            out = new ObjectOutputStream(socket.getOutputStream());//---------для отправки...
            in = new ObjectInputStream(socket.getInputStream());//блок. пока нет данных


//            sendPing();

            waitInputPacketStream();//поток остается здесь - ожидание поступление данных, обрабатывает и отвечает


        } catch (IOException e) {
            System.out.println("ConnectStream ERROR(IOException) startStream() - " + e.getMessage());
            cleanResStream(false);
        }
    }

    public void dropStream() {
        sendPacket(new Packet(Const.ID_DROP_CONNECTION));
        cleanResStream(false);
    }

    public synchronized void cleanResStream(boolean clearChat) {

        System.out.println(" cleanResStream - " + Thread.currentThread().getName() + "/ " + contact.getName());
        if (!isClose) {

            bExecute = false;
            missedCalls.clear();

            try {

                endConversation();

                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (socket != null && !socket.isClosed()) socket.close();

            } catch (Exception e) {

                System.out.println("ConnectStream ERROR cleanResStream() - " + e.getMessage());

            } finally {
                try {
                    cancelDropDialTimer();

                    cancelResendMessageTimer();

                    if (futureSendPingTimer != null)
                        futureSendPingTimer.cancel(true);

                    cancelResendPacketTimer();

                    if (serviceTimer != null && !serviceTimer.isShutdown())
                        serviceTimer.shutdown();

                } catch (Exception ignored) {
                }

                //1 завершаем все задачи
                try {

                    service.shutdown();
                    isClose = true;
                    onListener.onDisconnectConnectStream(clearChat);

                } catch (Exception e) {
                    System.out.println("ConnectStream ERROR service.shutdown() - " + e.getMessage());
                }
            }
        }
    }

    private void cancelResendPacketTimer() {
        if (futureResendPacketTimer != null)
            futureResendPacketTimer.cancel(true);
    }

    private void cancelDropDialTimer() {
        if (futureDialTimer != null)
            futureDialTimer.cancel(true);
    }


    private void waitInputPacketStream() {
        Packet packet = null;

        while (bExecute) {
            try {

                Object obj = in.readObject();// ждем сообщения от клиента

                packet = (Packet) obj;// ждем сообщения от клиента

            } catch (OutOfMemoryError e) {
                System.out.println("ConnectStream->waitInputPacketStream() ERROR - OutOfMemoryError ");
                packet = handlerOutMemoryError(packet, e);
            } catch (ClassNotFoundException e) {
                System.out.println("ConnectStream->waitInputPacketStream() ERROR(ClassNotFoundException) waitInputPacketStream() - " + e.getMessage());
                break;
            } catch (IOException e) {
                System.out.println("ConnectStream->waitInputPacketStream() ERROR(Exception) waitInputPacketStream() - " + e.getMessage());
                break;
            }

            realizeReceivedPacket(packet); //реализуем запрос-пакет
        }

        cleanResStream(false);
    }

    @Nullable
    private Packet handlerOutMemoryError(Packet packet, OutOfMemoryError e) {
        if (packet != null) {

            if (packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE || packet.getIdPacket() == Const.ID_MESSAGE) {
                //  отправляем уведомление - сообщение большое,  где ааресать анализирует отправляемое им сообщение
                sendPacket(createInfoMessagePacket(Const.ID_RESPONSE_MESSAGE_OUT_MEMORY, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));
            }

            //информируем пользователя о попытке получить большое сообщение
            //передаем сообщение для себя(в активити) о попытке получить большое сообщение

            String msg = App.context.getString(R.string.message_no_accept) + "\n" + e.getMessage();
            packet = new Packet(Const.ID_MESSAGE_SYS,
                    new MessagePacket(Const.NAME_SYSTEM, msg, System.currentTimeMillis(), Util.generateKey(), packet.getRecipientIP()),
                    0,
                    "",
                    "");
        }
        return packet;
    }

    private synchronized void actionByMapResendPackets(int action, int idPacket) {
        switch (action) {
            case Const.SEND_ACTION_TO_MAPS:
                for (Packet p : mapResendPackets.values()) {
                    sendPacket(p);
                }
                if (mapResendPackets.size() == 0)
                    cancelResendPacketTimer();
                break;
            case Const.REMOVE:
                System.out.println("1ConnectStream remove(idPacket) - " + mapResendPackets.size());
                mapResendPackets.remove(idPacket);
                System.out.println("2ConnectStream remove(idPacket) - " + mapResendPackets.size());
                break;
        }

    }

    private void removePacketFromMap(Integer ID) {

        if (ID != null)
            actionByMapResendPackets(Const.REMOVE, ID);
    }

    public synchronized void realizeReceivedPacket(Packet packet) {
        pingStep = 0;//сброс счетчика
        try {

            switch (packet.getIdPacket()) {

                case Const.ID_ACCEPT_RESEND_PACKET:
                    removePacketFromMap((Integer) packet.getObject());
                    break;
                case Const.ID_REQUEST_PING://запрашивает проверку звязи
                    sendPacket(new Packet(Const.ID_RESPONSE_PING));//проверка...
                    break;
                case Const.ID_RESPONSE_PING://--- ответ

                    break;
                case Const.ID_AUTHENTICATION://нужна идентификация нового подключения от (CommNode())
                    caseAuthentication(packet);
                    break;
                case Const.ID_MESSAGE_SYS:
                    newMail.setArrived(true);
                    MessagePacket ms = (MessagePacket) packet.getObject();
                    HistoryMessagePacket hMP = initMessages(ms, Const.MESSAGE_TYPE_DIRECTION_IN);
                    saveMessage(hMP);

                    onListener.onNewMessage();
                    break;
                case Const.ID_MESSAGE_RESENDING:
                case Const.ID_MESSAGE_AND_ATTACHED_FILE://сообщение с файлом
                case Const.ID_MESSAGE://----пришло сообщение повтор не принимать!

                    //сохраняем если контакт идентифицирован
                    caseMessage(packet);
                    break;
                case Const.ID_RESPONSE_MESSAGE_ACCEPT://--- сообщение получено
                    setMessageStatus(packet, Const.ACCEPT_MESSAGE_STATUS);
                    break;
                case Const.ID_RESPONSE_MESSAGE_OUT_MEMORY://--- сообщение не получено - большое
                    setMessageStatus(packet, Const.OUT_MEMORY_MESSAGE_STATUS);
                    break;

// *********************************************************************  phone

                case Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST://---он запрашивает со мной общение голосом

                    if (voiceCommReqState == Const.MISSING_PHONE) {

                        cancelDropDialTimer();
                        setVoiceCommReqState(Const.INCOMING_PHONE_CALL);
//                        voiceCommReqState = Const.INCOMING_PHONE_CALL;//трубка входящий звонок
//                        onListener.onChangeStateVoiceCommReq(voiceCommReqState);//обновляем адапте

                        sendPacket(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял пакет
                    }

                    break;
                case Const.ID_VOICE_COMMUNICATION_REQUEST://разговор( он взял трубку)
                    cancelDropDialTimer();
                    if (voiceCommReqState == Const.OUTGOING_PHONE_CALL) {//если я еще дозваниваюсь
                        createVoiceThread();//создаем канал связи
                        sendPacket(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял пакет
                    }
                    break;
                case Const.ID_MISSING_VOICE_COMMUNICATION_REQUEST://он завершил разговор

                    if (voiceCommReqState == Const.VOICE_PHONE) {
                        endConversation();//закрываем канал у себя
                        sendPacket(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял пакет
                    }
                    break;
                case Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST://он отменил вызов на разговор

                    if (voiceCommReqState == Const.INCOMING_PHONE_CALL) {
                        missedCalls.add(System.currentTimeMillis());
                        endConversation();//закрываем канал у себя
                        sendPacket(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял пакет
                    }
                    break;
                case Const.ID_DROP_VOICE_COMMUNICATION_REQUEST://он сбросил

                    endConversation();//закрываем канал у себя

                    sendPacket(new Packet(Const.ID_ACCEPT_RESEND_PACKET, packet.getIdPacket()));//ответ что принял

                    break;

            }
        } catch (OutOfMemoryError e) {
            System.out.println("ConnectStream ERROR OutOfMemoryError - " + e.getMessage());
            handlerOutOfMemoryError(packet);

        } catch (Exception e) {
            System.out.println("ConnectStream ERROR realizeReceivedPacket(Packet packet - " + packet.getIdPacket() + ") - " + e.getMessage());
        }

    }

    private void handlerOutOfMemoryError(Packet packet) {
        try {
            //пробуем убрать байты предположительно большого файла
            if (packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE) {
                MessagePacket mp = (MessagePacket) packet.getObject();
                if (mp.getFilePacket().getBytesAttachedFile() != null) {
                    mp.getFilePacket().setBytesAttachedFile(null);
                    mp.setMessage(mp.getMessage().concat(App.context.getString(R.string.out_of_memory)));

                    service.submit(() -> {
                        realizeReceivedPacket(packet);
                    });

                }
            }
        } catch (Exception ex) {
            System.out.println("ConnectStream OutOfMemoryError->RESET  " + ex.getMessage());
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

                    onListener.onMessageAccept(messages.indexOf(p));
                    break;
                }
            }
        }

    }

    private void caseMessage(Packet packet) {
        if (packet.getObject() != null) {

            MessagePacket mp = (MessagePacket) packet.getObject();

            if (contact.getId() > 0) {

                if (!isExistsMessageInDb(packet.getCreate(), packet.getKeyObj())) { //исключаем дубликат в базе


                    newMail.setArrived(true);

                    //сохраняем переданный файл
                    saveFileInMessagePacket(mp);

                    //сохраняем в историю сообщение и путь созданного файла

                    HistoryMessagePacket hMP = initMessages(mp, Const.MESSAGE_TYPE_DIRECTION_IN);
                    saveMessage(hMP);

                    onListener.onNewMessage();

                    sendPacket(createInfoMessagePacket(Const.ID_RESPONSE_MESSAGE_ACCEPT, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));//идентификация сообщения по данным в InfoMessagePacket

                } else {
                    sendPacket(createInfoMessagePacket(Const.ID_RESPONSE_MESSAGE_ACCEPT, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));//идентификация сообщения по данным в InfoMessagePacket

                }
            } else {

                //если не дубликат
                if (!isExistsMessageInList(messages, mp.getKeyUnique())) {

                    //для уведомления
                    newMail.setArrived(true);

                    // если идет чат(messages!=null)
                    if (messages != null) {
                        //сохраняем переданный файл
                        saveFileInMessagePacket(mp);

                        HistoryMessagePacket hMP = initMessages(mp, Const.MESSAGE_TYPE_DIRECTION_IN);
                        //сохраняем в массив
                        messages.add(hMP);

                        sendPacket(createInfoMessagePacket(Const.ID_RESPONSE_MESSAGE_ACCEPT, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));//идентификация сообщения по данным в InfoMessagePacket

                    }

                    //сообщим чтобы показать уведомление
                    onListener.onNewMessage();
                }
//
//                System.out.println("ConnectStream sendPacket");
//                sendPacket(createInfoMessagePacket(Const.ID_RESPONSE_MESSAGE_ACCEPT, packet.getCreate(), packet.getKeyObj(), packet.getRecipientIP()));//идентификация сообщения по данным в InfoMessagePacket

            }

        }
    }

    private void saveFileInMessagePacket(MessagePacket mp) {

        if (mp.getFilePacket() != null && mp.getFilePacket().getBytesAttachedFile() != null) {


            String dir = App.context.getFilesDir().getPath() + "/" + Const.PATH_STORAGE;

            //путь созданного файла
            String fPath = Util.saveFileToStore(mp.getFilePacket().getTypeFile(), mp.getFilePacket().getBytesAttachedFile(), dir);

            mp.getFilePacket().setPathFile(fPath);

            mp.getFilePacket().setBytesAttachedFile(null);//очистка
        }

    }

    private boolean isExistsMessageInList(List<HistoryMessagePacket> list, String keyUnique) {
        if (list != null) {
            for (HistoryMessagePacket hmp : list) {
                if (hmp.toString().equals(keyUnique)) {
                    return true;
                }
            }
        }

        return false;
    }


    //**********************************************************************************
    private void caseAuthentication(Packet packet) {
        AuthenticationPacket authPacket = (AuthenticationPacket) packet.getObject();
        if (authPacket != null) {
            // initialisation(tmp contact) if null

            //определяем контакт к которому привязанно(в поле KeyReverse ) соеденение
            DataContact attachContact = hostConnections.getKeyReverseContact(authPacket.getKeyContact());

            //если не получен ключ(ключ контакта по которому клиент(commNode) подключается) переданный клиентом
            if (contact.getKeyReverse().equals("")) {

                if (attachContact == null) {//соеденение не прикрепленно к кантакту

                    //получение данных аутентификации
                    contact.setName(authPacket.getName());
                    contact.setBytesPhoto(authPacket.getBytesPhoto());
                    contact.setKeyReverse(authPacket.getKeyContact());


                } else {

                    contact = attachContact;
                    sendPacket(new Packet(Const.ID_SUCCESS_AUTHENTICATION));//ответ ему (CommNode())
                    //запускаем таймер для повторной отправки сообщения
                    runReSendMessage();
                }

                //Обновим данные авторизации
                onListener.onAuthenticationStream();
            }
        }
    }

    @NonNull
    private Packet createInfoMessagePacket(byte idPacket, long created, String keyUnique, String recipientIp) {
        return new Packet(idPacket,
                new InfoMessagePacket(created,
                        keyUnique,
                        recipientIp),
                0,
                "",
                "");
    }

    private boolean isExistsMessageInDb(long create, String keyObj) {
        return (db.getMessageDAO().getId(create, keyObj) > 0);
    }


    private void saveMessage(HistoryMessagePacket hMP) {

        try {
            System.out.println("ConnectStream saveMessageInDb1");
            if (hMP.getIdContact() > 0) {
                //сохраняем в историю сообщение и путь созданного файла
                System.out.println("ConnectStream saveMessageInDb2");
                long id = db.getMessageDAO().add(hMP);
                hMP.setId(id);
            }

            if (messages != null) {
                messages.add(hMP);
                System.out.println("ConnectStream messages.add(hMP) - " + messages.size());
            }

        } catch (Exception e) {
            System.out.println("ConnectStream ERROR saveMessageToDB(MessagePacket mp) - " + e.getMessage());
        }

    }

    @NonNull
    private HistoryMessagePacket initMessages(MessagePacket mp, int directionTypeMessage) {
        return new HistoryMessagePacket(directionTypeMessage,
                mp,
                contact.getName(),
                contact.getId());
    }


    //статус о доставке
    private void setStMessageInDb(InfoMessagePacket imp, int status) {
        System.out.println("ConnectStream setStatusMessage 1");
        //изменяем в базе
        int countUpdate = db.getMessageDAO().updateAcceptStatus(imp.getCreate(),
                imp.getKeyUnique(),
                contact.getId(),
                status);
        System.out.println("ConnectStream setStatusMessage 2");

    }


    public synchronized void sendPacket(Packet packet) {

        if (!service.isShutdown()) {

            try {
                if (out != null) {

                    if (packet.getIdPacket() == Const.ID_MESSAGE || packet.getIdPacket() == Const.ID_MESSAGE_AND_ATTACHED_FILE) {

                        modificationAndSavePacketMessage(packet);

                        writePacket(packet);

                        onListener.onNotifySendMsgPacket();//информируем об отправке сообщения

                        //запускаем таймер для повторной отправки сообщения
                        runReSendMessage();

                    } else {

                        writePacket(packet);
                    }
                }

            } catch (IOException e) {
                System.out.println("ConnectStream ERROR sendPacket - " + e.getMessage());
            }

        }
    }

    private void writePacket(Packet packet) throws IOException {
        out.writeObject(packet);
        out.flush(); // чистим
    }

    private void modificationAndSavePacketMessage(Packet packet) {
        System.out.println("ConnectStream modificationPacketMessage(Packet packet)1");
        MessagePacket mp = (MessagePacket) packet.getObject();
        AttachedFilePacket filePacket = null;

        if (mp != null) {
            if (mp.getFilePacket() != null) {
                //копия пакета файла без bytes , но с путем файла в базу данных
                filePacket = new AttachedFilePacket(mp.getFilePacket().getMimeType(),
                        mp.getFilePacket().getTypeFile(),
                        mp.getFilePacket().getPathFile(),
                        mp.getFilePacket().getNameFile());
            }

            if (mp.getFilePacket() != null) {
                //2_1 этот параметр скопирован при отправки не нужен
                mp.getFilePacket().setPathFile("");
            }

            HistoryMessagePacket hMP = initMessages(new MessagePacket(mp.getNameSender(),
                    mp.getMessage(),
                    mp.getCreated(),
                    filePacket,
                    mp.getStatusAccept(),
                    mp.getKeyUnique(),
                    mp.getRecipientIp()), Const.MESSAGE_TYPE_DIRECTION_OUT);

            saveMessage(hMP);

        }

    }

    public void deleteMessage(HistoryMessagePacket hmp, List<HistoryMessagePacket> list) {
        service.submit(() -> {
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
                    onListener.onRemoveMessage(index);
                }
            }
        });
    }

    private void runReSendMessage() {
        System.out.println("ConnectStream reSendMessage() 1");
        cancelResendMessageTimer();

        Runnable task = () -> {

            if (contact.getId() > 0) {

                //получаем из базы неполученные сообщения для адресата(id)
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
                                    System.out.println("ConnectStream reSendMessage() ERROR - " + e.getMessage());
                                    filePacket.setBytesAttachedFile(null);
                                }
                            }
                        }

                        //повторная попытка отправить...
                        try {
                            if (out != null) {

                                sendPacket(new Packet(Const.ID_MESSAGE_RESENDING,
                                        hmp.getMessagePacket(),
                                        hmp.getMessagePacket().getCreated(),
                                        hmp.getMessagePacket().getKeyUnique(),
                                        hmp.getMessagePacket().getRecipientIp()));


                            }
                        } catch (Exception e) {
                            System.out.println("ConnectStream ERROR sendRequest(Packet packet) - " + e.getMessage());
                        }

                    }
                } else {
                    cancelResendMessageTimer();
                }
            } else {
                cancelResendMessageTimer();
            }

        };

        futureReSendMessageTimer = serviceTimer.scheduleAtFixedRate(task, 0, 15, TimeUnit.SECONDS);
    }

    private void cancelResendMessageTimer() {
        if (futureReSendMessageTimer != null)
            futureReSendMessageTimer.cancel(true);
    }


    @NonNull
    public DataContact getContact() {
        return contact;
    }

    public synchronized void gettingAttachContact() {
        //получаем контакт к которому привязали соеденение(в KeyReverse )
        DataContact attachContact = hostConnections.getKeyReverseContact(contact.getKeyReverse());

        //покажем какое то подключение(если контакт не идентифицирован то чата небудет)
        if (attachContact != null) {//  контакт назначен

            contact = attachContact;//  назначаем ссылку здесь

            //ответ клиенту CommNode() - контакт на его подключение определен
            sendPacket(new Packet(Const.ID_SUCCESS_AUTHENTICATION));

            //запускаем таймер для повторной отправки сообщений
            runReSendMessage();

            onListener.onAuthenticationStream();//обновим подключение(все ок)
        }
    }

    private void sendPing() {
        System.out.println("ConnectStream sendPing1");
        Runnable task = () -> {

            System.out.println("ConnectStream sendPing2");
            sendPacket(new Packet(Const.ID_REQUEST_PING));

            if (pingStep > 5) {//25 sec
                pingStep=0;
                cleanResStream(false);

            } else {
                pingStep++;
            }

        };

        futureSendPingTimer = serviceTimer.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
    }

    private synchronized void putToMapResendPackets(Packet packet) {

        mapResendPackets.put(packet.getIdPacket(), packet);
        sendPacket(packet);//отправляем сразу

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

    private void dropDialTimer() {
        cancelDropDialTimer();

        Runnable task = () -> {
            removePacketFromMap((int) Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST);//снимаем с дозвона
            sendPacket(new Packet(Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST));//отменить вызов
            endConversation();
        };
        futureDialTimer = serviceTimer.schedule(task, 30, TimeUnit.SECONDS);
    }

    public void prepareBeforeSendPacket(Packet packet, boolean bResend) {
        requestProcessingBeforeToSend(packet);

        if (bResend){
            putToMapResendPackets(packet);
        }else {
            sendPacket(packet);
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("ConnectStream finalize()");
    }

}
