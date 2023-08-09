package com.alexei.communicationoftwo.socket.server;


import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.model.StreamBlockData;
import com.alexei.communicationoftwo.socket.packet.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService service;
    private final List<ConnectStream> streamList = new ArrayList<>();// список всех поключений
    public static List<StreamBlockData> blockList = new ArrayList<>();// список блокированных
    private static ServerSocket server;


    public boolean bWhile;
    private final int port;


    //-----------------------------------listener
    private OnListeners onListener;


    public String getLocalIPAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                    return inetAddress.getHostAddress().toString();
                }
            }
        }
        return "null";
    }



    public void getDeviceIP() {
//        onListener.onGetIP(getLocalIPAddress());
        try {

//            Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
//            while (b.hasMoreElements()) {
//                for (InterfaceAddress f : b.nextElement().getInterfaceAddresses()) {
//
//                    if (f.getAddress().isSiteLocalAddress()) {
//                        onListener.onGetIP(f.getAddress().getHostAddress());
                        onListener.onGetIP(getLocalIPAddress());
//                        return;
//                    }
//                }
//
//            }
        } catch (SocketException e) {
            System.out.println("Server ERROR getRecipientIp() - " + e.getMessage());
        }

    }


    public void blockConnectStream(ConnectStream stream) {
        String ip = stream.addressHost;

        StreamBlockData streamBlockData = isExistsStreamBlock(ip);
        if (streamBlockData == null) {
            streamBlockData = new StreamBlockData(ip,
                    System.currentTimeMillis(),
                    stream.getContact().getId() <= 0 ?
                            App.context.getString(R.string.unknown) : stream.getContact().getName());
            blockList.add(streamBlockData);
        } else {
            streamBlockData.setAccess(System.currentTimeMillis());
        }


        stream.cleanResStream(true);
    }

    private StreamBlockData isExistsStreamBlock(String ip) {

        for (StreamBlockData blockData : blockList) {
            if (blockData.getIP().equals(ip)) {
                return blockData;
            }
        }
        return null;
    }

    public void dropConnectionStream(DataContact contact) {
        System.out.println("disconnectStream");
        for (ConnectStream stream : streamList) {
            if (stream.getContact().getId() == contact.getId()) {

                stream.cleanResStream(true);
                return;
            }
        }
    }


    public interface OnListeners {

        void onDropSocketListen();

        void onRunServerSocket();

        void onModifyConnection(ConnectStream stream);

        void onDisconnectionStream(ConnectStream stream, boolean clearChat);

        void onNewMessage(ConnectStream connectStream);

        void onGetIP(String sIP);

        void onDialBlockedContact(String IP);

        void onSendPacket(ConnectStream connectStream);

        void onMessageAcceptConnStream(ConnectStream connectStream, int i);

        void onChangeStateVoice(ConnectStream connectStream, int state);//++++

        void onRemoveMessageConnStream(ConnectStream connectStream, int index);

        void onConnection(ConnectStream connectStream);

        void onChangeModeSpeakerphone(ConnectStream finalStream, boolean speakerphoneOn);
    }

    public void setOnListener(OnListeners listener) {
        this.onListener = listener;
    }

    //-----------------------------------
    public Server(int port) {

        this.port = port;
        this.service = Executors.newFixedThreadPool(10);

    }


    public void startLoadServer() {
        service.submit(() -> {
            try {
                loadingServer();
            } catch (IOException e) {
                System.out.println("Server  ERROR startLoadServer() - " + e.getMessage());
            }
        });
    }

    private void loadingServer() throws IOException {
        if (server == null || server.isClosed()) {
//            InetSocketAddress insa = new InetSocketAddress("10.66.54.30", Const.PORT_CONNECT);
//            ServerSocket ss = new ServerSocket();
//            ss.bind(insa);
//            String host=ss.getInetAddress().getHostAddress();
//            System.out.println("!!!!!!!!!!!!!!  "+host);
            server = new ServerSocket(port);

            onListener.onRunServerSocket();

            this.bWhile = true;
            while (bWhile) {

                Socket socket = server.accept();// Блокируется до возникновения нового соединения
                //есть ли в списке блокированных
                StreamBlockData data = isExistsStreamBlock(socket.getInetAddress().getHostAddress());
                if (data == null) {

                        createConnectStream(socket);

                } else {
                    service.submit(() -> {
                        blocked(socket, data);
                    });
                }
            }

            onListener.onDropSocketListen();
        }
    }

    private void blocked(Socket socket, StreamBlockData data) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());//---------для отправки...
            out.writeObject(new Packet(Const.ID_BLOCKED_IP)); // отправляем  клиенту
            out.flush(); // чистим
            //фиксируем время
            data.setAccess(System.currentTimeMillis());
            socket.close();

            onListener.onDialBlockedContact(socket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            System.out.println("Server ERROR(IOException) blocked() - " + e.getMessage());
        }

    }

    private synchronized void createConnectStream(Socket socket) {

        ConnectStream connectStream = null;
        try {
            System.out.println("createConnectStream");
            //проверяем есть ли объект-stream по такому же IP
            deleteIfExistsConnection(socket.getInetAddress().getHostAddress());

            connectStream = new ConnectStream(socket);//создается класс подключение c клиентом

            ConnectStream finalStream = connectStream;
            connectStream.setOnListener(new ConnectStream.OnListeners() {
                @Override
                public void onNewMessage() {
                    //сообщение от клиента
                    onListener.onNewMessage(finalStream);
                }

                @Override
                public synchronized void onDisconnectConnectStream( boolean clearChat) {

                    onListener.onDisconnectionStream(finalStream,clearChat);

                    int i=streamList.indexOf(finalStream);
                    streamList.set(i,null);
                    streamList.remove(i);

                }

                @Override
                public void onAuthenticationStream() {

                    // 2 сигнализируем об оновленых данных аутентификации в подключении
                    onListener.onModifyConnection(finalStream);
                }


                @Override
                public void onNotifySendMsgPacket() {
                    onListener.onSendPacket(finalStream);
                }

                @Override
                public void onMessageAccept( int i) {
                    onListener.onMessageAcceptConnStream(finalStream, i);
                }

                @Override
                public void onChangeStateVoiceCommReq( int state) {//+++++
                    onListener.onChangeStateVoice(finalStream, state);
                }

                @Override
                public void onRemoveMessage( int index) {
                    onListener.onRemoveMessageConnStream(finalStream, index);
                }

                @Override
                public void onChangeStateSpeakerphone(boolean speakerphoneOn) {
                    onListener.onChangeModeSpeakerphone(finalStream,speakerphoneOn);
                }


            });

            streamList.add(connectStream);

            onListener.onConnection(connectStream);


        } catch (Exception e) {
            System.out.println("Server ERROR createConnectStream - " + e.getMessage());
            if (connectStream != null)
                connectStream.cleanResStream(false);
        }
    }


    private synchronized void deleteIfExistsConnection(String addressHost) {
        System.out.println("deleteIfExistsConnection");

        for (ConnectStream s : streamList) {
            //если stream по такому же IP
            if (s.addressHost.equals(addressHost)) {

                s.cleanResStream(false);
                return;
            }
        }
    }


}
