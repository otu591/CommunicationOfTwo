package com.alexei.communicationoftwo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.Util;
import com.alexei.communicationoftwo.adapter.AdapterBlockConnectStream;
import com.alexei.communicationoftwo.adapter.AdapterChatList;
import com.alexei.communicationoftwo.adapter.AdapterConnections;
import com.alexei.communicationoftwo.adapter.AdapterConnectionsByCriteria;
import com.alexei.communicationoftwo.adapter.AdapterContacts;
import com.alexei.communicationoftwo.adapter.AdapterHistoryMessageList;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;

import com.alexei.communicationoftwo.databinding.ActivityMainBinding;
import com.alexei.communicationoftwo.exClass.SoundClass;
import com.alexei.communicationoftwo.exClass.WaitConnectionsService;
import com.alexei.communicationoftwo.model.DataAccount;
import com.alexei.communicationoftwo.model.DataLocation;
import com.alexei.communicationoftwo.model.UserChat;
import com.alexei.communicationoftwo.socket.ConnectionObject;
import com.alexei.communicationoftwo.socket.HostConnections;
import com.alexei.communicationoftwo.socket.client.CommunicationNode;
import com.alexei.communicationoftwo.socket.packet.AttachedFilePacket;
import com.alexei.communicationoftwo.socket.packet.MessagePacket;
import com.alexei.communicationoftwo.socket.packet.Packet;
import com.alexei.communicationoftwo.socket.server.ConnectStream;
import com.alexei.communicationoftwo.socket.server.Server;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "Уведомление";
    private static final int NOTIFY_ID = 101;

    private static MainActivity instance;
    private ExecutorService service;
    private HostConnections hostSockets = WaitConnectionsService.hostSockets;
    private Menu optionsMenu;
    private SoundClass sound;
    public static DataAccount account;


    private AdapterContacts adapterContacts;
    private AdapterHistoryMessageList adapterHistory;
    private AdapterConnections adapterConnections;

    private DataLocation locationDotInMap;

    private ActivityMainBinding binding;


    private ImageView ivPhotoAccount;

    private ImageView ivPhotoContact;

    private int pageIndex;

    private int screenOrientation;
    @Nullable
    private UserChat userChatHistory;

    private boolean showNotification = true;


    public static MainActivity getInstance() {
        return instance;
    }


    public void initHosts() {
        System.out.println("MainActivity initHosts");
//        this.hostSockets = HostConnections.getInstance();
        handlerHostSocketsListeners();
        defSelectedCommunicationObject();

    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showNotification = false;

    }


    @Override
    public void onPause() {
        super.onPause();
        System.out.println("onPause()");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showNotification = true;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("orientation", screenOrientation);
        outState.putInt("page", pageIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        if (savedInstanceState != null) {
            screenOrientation = savedInstanceState.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_USER);

            initIndexPage(savedInstanceState.getInt("page", Const.PAGE_CHAT));

        } else {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
            initIndexPage(Const.PAGE_CHAT);
        }
// устанавливаем режим
        setRequestedOrientation(screenOrientation);

        instance = this;

        setContentView(binding.getRoot());

        service = Executors.newFixedThreadPool(10);

        account = new DataAccount("", "", "");

        sound = SoundClass.getInstance();

//        initHosts();

        buildRecyclerViewConnections();

        initRecyclerViewHistoryMessages(new ArrayList<>());

        initViewListener();

        updatePage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            App.context.startForegroundService(new Intent(MainActivity.this, WaitConnectionsService.class));
        } else {
            App.context.startService(new Intent(MainActivity.this, WaitConnectionsService.class));
        }
    }


    private void updatePage() {
        System.out.println("updatePage()");
        runOnUiThread(() -> {
            showBlock(true);
            try {
                binding.llTitlePage.setVisibility(View.INVISIBLE);
                binding.llBlockLayoutsRotateOrientation.setVisibility(View.INVISIBLE);

                if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {

                    binding.llBlockLayoutsRotateOrientation.setOrientation(LinearLayout.VERTICAL);
                    if (pageIndex == Const.PAGE_CHAT) {

                        drawTitlePageChat();
                        binding.rlTop.setVisibility(View.VISIBLE);
                        binding.rlBottom.setVisibility(View.GONE);

                    } else {

                        redrawNotifyViews();
                        drawTitlePageConnections();
                        binding.rlBottom.setVisibility(View.VISIBLE);
                        binding.rlTop.setVisibility(View.GONE);
                    }

                    binding.llTitlePage.setVisibility(View.VISIBLE);
                    binding.llBlockLayoutsRotateOrientation.setVisibility(View.VISIBLE);

                } else if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {

                    drawTitlePageConnections();

                    binding.rlTop.setVisibility(View.VISIBLE);
                    binding.rlBottom.setVisibility(View.VISIBLE);
                    binding.llBlockLayoutsRotateOrientation.setOrientation(LinearLayout.HORIZONTAL);

                    binding.llTitlePage.setVisibility(View.GONE);


                } else if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_USER) {

                    if (pageIndex == Const.PAGE_CHAT) {
                        drawTitlePageChat();

                        binding.rlTop.setVisibility(View.VISIBLE);
                        binding.rlBottom.setVisibility(View.GONE);
                    } else {
                        redrawNotifyViews();

                        drawTitlePageConnections();
                        binding.rlTop.setVisibility(View.GONE);
                        binding.rlBottom.setVisibility(View.VISIBLE);
                    }

                    binding.llTitlePage.setVisibility(View.VISIBLE);
                }


                binding.llBlockLayoutsRotateOrientation.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                System.out.println("MainActivity ERROR updatePage() - " + e.getMessage());

            }

            showBlock(false);
        });

    }

    private void redrawNotifyViews() {
        try {
            sound.stopSound(R.raw.dial);
            optionsMenu.findItem(R.id.optVoicePhone).setVisible(false);
            optionsMenu.findItem(R.id.optMail).setVisible(false);
            binding.tvEventNewConnection.setVisibility(View.GONE);
        } catch (Exception e) {
            System.out.println("MainActivity ERROR redrawNotifyViews() - " + e.getMessage());
        }

    }

    private void drawTitlePageChat() {
        binding.tvTitlePageChat.setTextColor(ContextCompat.getColor(this, R.color.colorDarkerAccent));
        binding.tvTitlePageChat.setTypeface(null, Typeface.BOLD);

        binding.tvTitlePageConnections.setTextColor(ContextCompat.getColor(this, R.color.grey2));
        binding.tvTitlePageConnections.setTypeface(null, Typeface.NORMAL);

        binding.rlChat.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDarkerAccent));
        binding.rlConnections.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

    }

    private void drawTitlePageConnections() {

        binding.tvTitlePageChat.setTextColor(ContextCompat.getColor(this, R.color.grey2));
        binding.tvTitlePageChat.setTypeface(null, Typeface.NORMAL);

        binding.tvTitlePageConnections.setTextColor(ContextCompat.getColor(this, R.color.colorDarkerAccent));
        binding.tvTitlePageConnections.setTypeface(null, Typeface.BOLD);

        binding.rlChat.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        binding.rlConnections.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDarkerAccent));
    }


    private void handlerHostSocketsListeners() {

        hostSockets.setOnListeners(new HostConnections.OnListeners() {

            @Override
            public void onConnectStream(ConnectStream stream) {
                System.out.println("подключение клиента(onConnect)");
                sound.startSound(R.raw.connect2, false);

                drawCountPageConnections("+");

            }

            @Override
            public void onDisconnectStream() {
                System.out.println("onDisconnectStream");

                drawCountPageConnections("-");

                checkIncomingCallForHideMenuPhone();

            }

            @Override
            public void onNewMessage(ConnectStream stream) {
                System.out.println("onNewMessage stream");
                if (showNotification)
                    showTopNotification();

                if (pageIndex == Const.PAGE_CHAT) {
                    runOnUiThread(() -> {
                        if (hostSockets.getSelectedConnectionObject() != null) {

                            if (hostSockets.getSelectedConnectionObject().getCommunication() != stream) {
                                // уведомляем-показываем значек
                                optionsMenu.findItem(R.id.optMail).setVisible(true);

                            } else {
                                // отображаем в чате
                                updateAdapterHMPOnNewMessage();
                            }

                        } else {
                            // уведомляем-показываем значек
                            optionsMenu.findItem(R.id.optMail).setVisible(true);
                        }
                    });
                }

                notifyAdapterConnections();
                sound.startSound(R.raw.sms, false);
            }


            @Override
            public void getIP(String sIP) {
                System.out.println("7");
                runOnUiThread(() -> {
                    String s = "  " + getString(R.string.ip_) + " " + sIP;
                    showSnackBar(s, getString(R.string.ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });
                });
            }

            @Override
            public synchronized void onDisconnectCommNode() {

//                notifyAdapterConnections();

                checkIncomingCallForHideMenuPhone();

                drawCountPageConnections("-");

            }

            @Override
            public void onConnectCommNode(CommunicationNode commNode) {

                notifyAdapterContact(commNode.contact);

                if (commNode.contact.isNotificationSoundOfConnection()) {
                    sound.startSound(R.raw.connect, true);
                }

                drawCountPageConnections(Const.CONNECTION_ADD);
            }

            @Override
            public void onNewMessage(CommunicationNode commNode) {
                System.out.println("onNewMessage commNode");
                if (showNotification)
                    showTopNotification();

                if (pageIndex == Const.PAGE_CHAT) {
                    runOnUiThread(() -> {
                        if (hostSockets.getSelectedConnectionObject() != null) {

                            if (hostSockets.getSelectedConnectionObject().getCommunication() != commNode) {
                                // уведомляем-показываем значек
                                optionsMenu.findItem(R.id.optMail).setVisible(true);

                            } else {
                                // отображаем в чате
                                updateAdapterHMPOnNewMessage();
                            }

                        } else {
                            // уведомляем-показываем значек

                            optionsMenu.findItem(R.id.optMail).setVisible(true);
                        }
                    });
                }

                notifyAdapterConnections();
                sound.startSound(R.raw.sms, false);
            }


            @Override
            public void onAddOrChangeContact(DataContact contact) {
                System.out.println("onAddOrChangeContact");

                notifyAdapterContact(contact);

                notifyAdapterConnections();

                if (hostSockets.getSelectedConnectionObject() != null &&
                        hostSockets.getSelectedConnectionObject().getContact().getId() == contact.getId()) {

                    drawRecipientFromSelectedConnectionObject(hostSockets.getSelectedConnectionObject().getContactName());
                }
            }

            @Override
            public void onModifyStatusContact(DataContact contact) {

                notifyAdapterContact(contact);
            }

            @Override
            public void onBlockedContact(String ip) {

                runOnUiThread(() -> {
                    optionsMenu.findItem(R.id.optShowBlocked).setVisible(true);
                });
            }

            @Override
            public void onSendPacket(ConnectStream stream) {

                updateAdapterHistoryEventSendMessage();
            }

            @Override
            public void onCommNodeSendPacket(CommunicationNode commNode) {

                updateAdapterHistoryEventSendMessage();
            }

            @Override
            public void onMessageAcceptCommNode(CommunicationNode commNode, int i) {

                runOnUiThread(() -> {
                    if (adapterHistory != null)
                        adapterHistory.notifyItemChanged(i);

                });
            }

            @Override
            public void onMessageAcceptConnectStream(ConnectStream connectStream, int i) {

                runOnUiThread(() -> {

                    if (adapterHistory != null)
                        adapterHistory.notifyItemChanged(i);

                });
            }

            @Override
            public void onChangeStateVoiceCommNode(CommunicationNode commNode, int state) {////

                onChangeStateVoice(commNode, state);
                notifyAdapterConnections();

            }

            @Override
            public void onChangeStateVoiceConnStream(ConnectStream stream, int state) {//+++++
                System.out.println("onChangeStateVoiceConnStream");
                onChangeStateVoice(stream, state);

                notifyAdapterConnections();
            }


            @Override
            public void onRemoveMessageCommNode(CommunicationNode commNode, int index) {
                runOnUiThread(() -> {
                    if (adapterHistory != null)
                        adapterHistory.notifyDataSetChanged();
//                    adapterHistory.notifyItemRemoved(index);
                });
            }

            @Override
            public void onRemoveMessageConnStream(ConnectStream connectStream, int index) {
                runOnUiThread(() -> {
                    if (adapterHistory != null)
                        adapterHistory.notifyDataSetChanged();
//                    adapterHistory.notifyItemRemoved(index);
                });
            }

            @Override
            public void onShowHistoryChat(List<HistoryMessagePacket> mps, UserChat userChat) {

                runOnUiThread(() -> {
                    userChatHistory = userChat;

                    offSelectedConnection();

                    replaceMessageListInHistoryAdapter(mps);

                    String s = userChat.getNameContact().equals("") ?
                            getString(R.string.unknown) : userChat.getNameContact().concat(getString(R.string.view_history));

                    drawRecipientEventNotConnection(s);

                });

            }

            @Override
            public void onRemoveMessageHistory(int index) {
                runOnUiThread(() -> {
                    if (adapterHistory != null)
                        adapterHistory.notifyDataSetChanged();
                });
            }

            @Override
            public void onDeletedContact(int i) {
                runOnUiThread(() -> {
                    if (adapterContacts != null)
                        adapterContacts.notifyDataSetChanged();

                    Toast.makeText(MainActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRestartConnectionByContact(long contactId) {
                System.out.println("20_2 - " + Thread.currentThread().getName());
                //если у текущего чата потеряно соединение
                runOnUiThread(() -> {
                    if (hostSockets.getSelectedConnectionObject() != null &&
                            hostSockets.getSelectedConnectionObject().getContact().getId() == contactId) {

                        Toast.makeText(MainActivity.this, getString(R.string.recovery_connection), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAuthenticationConnectionStream(ConnectStream stream) {
                notifyAdapterConnections();

            }

            @Override
            public void onSelectionConnectionObject(ConnectionObject object) {
                service.execute(() -> {
                    initSelectedConnectionObject(object);
                    notifyAdapterConnections();
                });
            }

            @Override
            public void onDisconnectSelectedConnectionObject(ConnectionObject selectedConnectionObject, String name, boolean clearChat) {

                if (clearChat) {

                    hostSockets.offSelectConnectionObject(selectedConnectionObject);

                    runOnUiThread(() -> {
                        updateAdapterHMPOnNewMessage();
                    });

                } else {

                    drawRecipientEventNotConnection(name + getString(R.string.not_connect));
                }
            }

            @Override
            public void onModifyConnections() {
                notifyAdapterConnections();
            }

            @Override
            public void onOffSelectConnection() {
                drawRecipientFromDisconnectObject();
            }

            @Override
            public void onChangeStateSpeakerphone(ConnectionObject object, boolean speakerphoneOn) {
                System.out.println("START onChangeStateSpeakerphone - " + System.currentTimeMillis());
//                int i = hostSockets.connectionObjectList.indexOf(object);
                runOnUiThread(() -> {
//                    System.out.println("onChangeStateSpeakerphone - " + object.getStateSpeaker());
//                    adapterConnections.notifyItemChanged(i);
                    adapterConnections.notifyDataSetChanged();

                });
                System.out.println("FINISCH onChangeStateSpeakerphone - " + System.currentTimeMillis());
            }


        });
    }

    private synchronized void drawRecipientEventNotConnection(String s) {
        runOnUiThread(() -> {

            binding.tvRecipientMessage.setText(s);
            binding.tvTitleRecipient.setVisibility(View.GONE);
//            binding.fabPhone.setVisibility(View.INVISIBLE);
            binding.llBlockPhone.setVisibility(View.GONE);

        });
    }


    private void onChangeStateVoice(Object obj, int state) {
        if (hostSockets.isSelectConnectionFromAttachObj(obj)) {
            drawImagePhone(state);
        }

        if (state == Const.INCOMING_PHONE_CALL) {

            if (showNotification)
                showTopNotification();

            runOnUiThread(() -> {
                if (pageIndex == Const.PAGE_CONNECTIONS) {
                    sound.startSound(R.raw.dial, false);
                } else {
                    drawMenuItemPhone();
                }
            });

        } else {

            checkIncomingCallForHideMenuPhone();
        }
    }

    private void showTopNotification() {
        // Удаляем все свои уведомления
//        notificationManager.cancelAll();
        System.out.println("showTopNotification()1");
        try {
            Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


//            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, notificationIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            PendingIntent pi = PendingIntent.getActivity(MainActivity.this,
                    0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                System.out.println("showTopNotification()2");
                NotificationChannel notificationChannel = new NotificationChannel
                        (CHANNEL_ID,"Service", NotificationManager.IMPORTANCE_HIGH);

                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.enableVibration(true);
                notificationChannel.setDescription("Description");

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_baseline_send_24)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                        R.drawable.chat64)) // большая картинка
                                .setContentTitle(getString(R.string.notification))
                                .setContentText(getString(R.string.incoming))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pi)

                                .setAutoCancel(true); // автоматически закрыть уведомление после нажатия;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                notificationManager.createNotificationChannel(notificationChannel);

                notificationManager.notify(NOTIFY_ID, builder.build());
            } else {
                System.out.println("showTopNotification()3");
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_baseline_send_24)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                        R.drawable.chat64)) // большая картинка
                                .setContentTitle(getString(R.string.notification))
                                .setContentText(getString(R.string.incoming))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pi)

                                .setAutoCancel(true); // автоматически закрыть уведомление после нажатия;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                notificationManager.notify(NOTIFY_ID, builder.build());

            }
        } catch (Exception e) {
            System.out.println("showTopNotification() ERROR " + e.getMessage());
        }

    }

    private void showBlockChat(boolean b) {
        runOnUiThread(() -> {

            binding.prgBarChat.setIndeterminate(b);
            if (b) {
                binding.flMapBlockChat.setVisibility(View.VISIBLE);
            } else {
                binding.prgBarChat.setMax(1);
                binding.prgBarChat.setProgress(1);
                binding.flMapBlockChat.setVisibility(View.INVISIBLE);
            }

        });
    }


    private void drawMenuItemPhone() {
        sound.startSound(R.raw.dial, true);
        optionsMenu.findItem(R.id.optVoicePhone).setVisible(true);

    }

    private void clearChat() {
        runOnUiThread(() -> {
            drawRecipientFromDisconnectObject();

            replaceMessageListInHistoryAdapter(new ArrayList<>());
        });
    }

    private void updateAdapterHistoryEventSendMessage() {
        runOnUiThread(() -> {

            if (pageIndex == Const.PAGE_CHAT) {

                if (adapterHistory != null) {
                    if (adapterHistory.getItemCount() > 0)
                        binding.rvHistoryMessages.scrollToPosition(adapterHistory.getItemCount() - 1);
                    adapterHistory.notifyDataSetChanged();
                }

                Toast.makeText(getApplicationContext(), R.string.send_message, Toast.LENGTH_SHORT).show();
                showBlock(false);
            }

        });
    }

    private synchronized void offSelectedConnection() {
        System.out.println("offSelectedConnection()");
        hostSockets.offSelectConnectionObject(hostSockets.getSelectedConnectionObject());
    }

    private synchronized void updateAdapterHMPOnNewMessage() {

        if (adapterHistory != null) {
            if (adapterHistory.getItemCount() > 0)
                binding.rvHistoryMessages.scrollToPosition(adapterHistory.getItemCount() - 1);

            adapterHistory.notifyDataSetChanged();

        }
    }


    private void checkIncomingCallForHideMenuPhone() {
        System.out.println("checkIncomingCallForHideMenuPhone()");
        if (!hostSockets.isVoiceStateIncomingToConnObjList()) {
            sound.stopSound(R.raw.dial);
            runOnUiThread(() -> {
                optionsMenu.findItem(R.id.optVoicePhone).setVisible(false);
            });
        }
    }


    @Override
    protected void onStart() {
        System.out.println("onStart()");
        super.onStart();
        showNotification = false;
        //при поворете Экрана
        startService(new Intent(MainActivity.this, WaitConnectionsService.class));
//        defSelectedCommunicationObject();
        loadDataAccount();
//        setTitleCountConnections();

    }

    private void defSelectedCommunicationObject() {
        service.submit(() -> {

            ConnectionObject object = hostSockets.getSelectedConnectionObject();
            if (object != null) {

                userChatHistory = null;

                drawRecipientFromSelectedConnectionObject(object.getContactName());

                getMessagesInSelectedConnections(object);

            }
        });

    }

    private void initMenu() {

        if (optionsMenu != null) {
            char c = Character.toUpperCase(account.getName().trim().charAt(0));
            optionsMenu.findItem(R.id.optAccount).setTitle("" + c);
        }
    }

    private void loadDataAccount() {
        service.submit(() -> {
            String s = getStringPreferences(Const.PREF_NAME_ACCOUNT);
            if (s.isEmpty()) {
                s = getString(R.string.unknown);
            }
            account.setName(s);
            s = getStringPreferences(Const.PREF_PATH_IMG_ACCOUNT);

            if (s.isEmpty()) {
                s = "android.resource://" + this.getPackageName() + "/drawable/ic_baseline_account_circle_24";
            }
            account.setPathImg(s);
            runOnUiThread(this::initMenu);
        });

    }

    @Override
    protected void onDestroy() {
        System.out.println("MainActivity onDestroy()");
        try {
            sound.recoveryRes();
        } catch (Exception e) {
            System.out.println("MainActivity ERROR onDestroy() - " + e.getMessage());
        } finally {
            super.onDestroy();
        }
    }


    private void notifyAdapterContact(DataContact contact) {
        System.out.println("notifyAdapterContact");
        runOnUiThread(() -> {
            if (adapterContacts != null) {
                adapterContacts.notifyDataSetChanged();
            }
        });

    }

    private synchronized void notifyAdapterConnections() {
        System.out.println("notifyAdapterConnections()");
        runOnUiThread(() -> {
            if (adapterConnections != null) {
                adapterConnections.notifyDataSetChanged();
            }
        });
    }

    private void drawCountPageConnections(String s) {
        System.out.println("setTitleCountConnections");
        runOnUiThread(() -> {
            if (pageIndex == Const.PAGE_CHAT) {
                if (adapterConnections.getItemCount() > 0) {
                    binding.tvEventNewConnection.setVisibility(View.VISIBLE);
                    binding.tvEventNewConnection.setText("" + adapterConnections.getItemCount());
                }
                if (s.equals(Const.CONNECTION_ADD)) {
                    binding.tvEventNewConnection.setTextColor(ContextCompat.getColor(this, R.color.green));
                } else {
                    binding.tvEventNewConnection.setTextColor(ContextCompat.getColor(this, R.color.red));
                }
            }

        });
    }

    private void drawImagePhone(int statusPhone) {

        runOnUiThread(() -> {

            AnimationDrawable rocketAnimation;
            binding.fabAcceptCallPhone.clearAnimation();
            binding.llBlockPhone.clearAnimation();

            switch (statusPhone) {

                case Const.OUTGOING_PHONE_CALL://исходяший
                    binding.llBlockPhone.setVisibility(View.VISIBLE);
                    binding.fabCancelCallPhone.setVisibility(View.GONE);

                    binding.fabAcceptCallPhone.setImageResource(R.drawable.list_phone_forwarded);

                    binding.fabAcceptCallPhone.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));

                    rocketAnimation = (AnimationDrawable) binding.fabAcceptCallPhone.getDrawable();//etImageDrawable()[3];
                    rocketAnimation.start();

                    break;
                case Const.INCOMING_PHONE_CALL://входящий
                    binding.llBlockPhone.setVisibility(View.VISIBLE);

                    binding.fabCancelCallPhone.setVisibility(View.VISIBLE);

                    binding.fabAcceptCallPhone.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green)));

                    binding.fabAcceptCallPhone.setImageResource(R.drawable.ic_baseline_phone_24);

                    Animation anim = new AlphaAnimation(0.5F, 1);
                    anim.setInterpolator(new LinearInterpolator());

                    anim.setRepeatCount(Animation.INFINITE);
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setDuration(1000);

                    binding.fabAcceptCallPhone.setAnimation(anim);

                    break;
                case Const.VOICE_PHONE://разговор
                    binding.llBlockPhone.setVisibility(View.VISIBLE);
                    binding.fabCancelCallPhone.setVisibility(View.GONE);

                    binding.fabAcceptCallPhone.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green)));
                    binding.fabAcceptCallPhone.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
                    binding.fabAcceptCallPhone.setImageResource(R.drawable.list_phone_voice);

                    rocketAnimation = (AnimationDrawable) binding.fabAcceptCallPhone.getDrawable();
                    rocketAnimation.start();
                    break;
                case Const.MISSING_PHONE://missing

                    binding.fabAcceptCallPhone.clearAnimation();
                    binding.fabAcceptCallPhone.setImageResource(R.drawable.ic_phone_missed);

                    Animation anim2 = new TranslateAnimation(0.0f, 0.0f, 0.0f, 200.0f);
                    anim2.setDuration(1000);
                    binding.llBlockPhone.setAnimation(anim2);

                    binding.llBlockPhone.setVisibility(View.GONE);

            }
        });

    }


    //*****************************************************************  initViewListener  ////////
    private void initViewListener() {

        binding.fabCancelCallPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.submit(() -> {
                    if (hostSockets.getSelectedConnectionObject() != null)
                        hostSockets.getSelectedConnectionObject().sendPacket(new Packet(Const.ID_DROP_VOICE_COMMUNICATION_REQUEST));//сброс всего
                });
            }
        });

        binding.fabAcceptCallPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                service.submit(() -> {
                    chooseOtherActionPhone(hostSockets.getSelectedConnectionObject());
                });

            }
        });

        binding.tvTitleRecipient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hostSockets.getSelectedConnectionObject() != null) {

                    dialogModifyContact(getString(R.string.recipient),
                            hostSockets.getSelectedConnectionObject().getContact(),
                            Const.EDIT_CONTACT,
                            hostSockets.getSelectedConnectionObject());
                } else {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.not_selected_connection), Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.ivMenuTools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenuTools();
            }
        });

        binding.tvEventNewConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("34");
                service.submit(() -> {
                    sortedByNewConnections();

                    initIndexPage(Const.PAGE_CONNECTIONS);
                    updatePage();
                });

            }
        });

        binding.ibClearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("35");
                binding.etText.setText("");

            }
        });

        binding.tvTitlePageChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.submit(() -> {
                    System.out.println("36");

                    initIndexPage(Const.PAGE_CHAT);
                    updatePage();
                });
            }
        });

        binding.tvTitlePageConnections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("37");
                service.submit(() -> {
                    sortedBySelectedConnections();
                    initIndexPage(Const.PAGE_CONNECTIONS);

                    updatePage();
                });

            }
        });


        binding.ibRecordMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startRecordVoiceActivity();
            }
        });

        binding.etText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.length() > 0) {

                    binding.ibRecordMessage.setVisibility(View.INVISIBLE);
                    binding.ibClearText.setVisibility(View.VISIBLE);
                } else {

                    binding.ibRecordMessage.setVisibility(View.VISIBLE);
                    binding.ibClearText.setVisibility(View.INVISIBLE);
                }
            }
        });

        binding.ibSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("SendMessage 1");
                service.execute(() -> {
                    System.out.println("SendMessage 2");
                    //запускаем подключение из существующих
                    if (hostSockets.getSelectedConnectionObject() != null) {
                        System.out.println("SendMessage 3");
                        if (binding.etText.getText().toString().length() > 0) {
                            System.out.println("SendMessage 4");
                            sendToSelectedConnection(hostSockets.getSelectedConnectionObject(), binding.etText.getText().toString(), null);
                        }
                    } else {

                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), R.string.not_selected_connection, Toast.LENGTH_LONG).show();
                        });

                    }
                });
            }
        });
    }

    private void initIndexPage(int page) {
//        if (page == Const.PAGE_CHAT) {
//            hostSockets.closeAllVoice();
//        }
        pageIndex = page;
    }

    private void showMenuTools() {
        final PopupMenu popupMenu = new PopupMenu(App.context, binding.ivMenuTools);
        popupMenu.inflate(R.menu.menu_tools);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.optAttachFile) {
                    selectAttachFile();
                } else if (menuItem.getItemId() == R.id.optLocation) {
                    locationDotInMap = null;
                    getDotLocation(null);
                } else if (menuItem.getItemId() == R.id.optCamera) {
                    writeVideoFile();
                } else if (menuItem.getItemId() == R.id.optCallPhone) {
                    handlerCallPhone();
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void handlerCallPhone() {
        if (hostSockets.getSelectedConnectionObject() != null) {

            service.submit(() -> {
                chooseOtherActionPhone(hostSockets.getSelectedConnectionObject());
            });
        }
    }

    private void writeVideoFile() {
        if (checkCameraPermissionGranted()) {
            startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), Const.ACTION_VIDEO_REQUEST);
        }
    }

    private void sortedBySelectedConnections() {
        if (hostSockets.getConnectionObjectsList().size() > 0) {
            for (ConnectionObject object : hostSockets.getConnectionObjectsList()) {
                if (object.isSelected()) {
                    Collections.swap(hostSockets.getConnectionObjectsList(), hostSockets.getConnectionObjectsList().indexOf(object), 0);
                    break;
                }
            }
        }
    }


    private void sortedByNewConnections() {
        System.out.println("44");

        Collections.sort(hostSockets.getConnectionObjectsList(), new Comparator<ConnectionObject>() {
            @Override
            public int compare(final ConnectionObject p1, final ConnectionObject p2) {
                return Long.compare(p1.getCreate(), p2.getCreate());
            }
        });
    }

    private void selectAttachFile() {
        System.out.println("45");

        Intent i = new Intent(Intent.ACTION_PICK);
        i.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "*/*");
        startActivityForResult(i, Const.ATTACH_FILES_REQUEST);
    }

    private void getDotLocation(DataLocation l) {
        System.out.println("46");
        Intent intent = new Intent(MainActivity.this, SelectPointInMapActivity.class);
        intent.putExtra("location", l);
        if (l == null) {
            startActivityForResult(intent, Const.SELECT_DOT_REQUEST_CODE);
        } else {
            startActivity(intent);
        }
    }


    private void defineIpDevice() {
        hostSockets.getMyIP();
    }

    private void setDotPosition(DataLocation l) {
        String s = " { " +
                l.getLatitude() +
                "," +
                l.getLongitude() +
                " }\n";
        binding.etText.append(s);
    }


    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        //Snackbar содержит действие, которое устанавливается через- setAction(action,listener)

        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }


    private void sendToSelectedConnection(ConnectionObject selObj, String text, AttachedFilePacket filePacket) {
        System.out.println("SendMessage 5");

        long t = System.currentTimeMillis();
        String key = Util.generateKey();

        selObj.sendPacket(new Packet(filePacket == null ? Const.ID_MESSAGE : Const.ID_MESSAGE_AND_ATTACHED_FILE,
                new MessagePacket(account.getName(),
                        text,
                        t,
                        filePacket,
                        Const.NO_ACCEPTED_MESSAGE_STATUS,
                        key,
                        selObj.getAddressHost()),
                t, key, selObj.getAddressHost()));

        System.out.println("SendMessage 7");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        System.out.println("49");
        getMenuInflater().inflate(R.menu.menu, menu);
        optionsMenu = menu;
        char c = Character.toUpperCase(account.getName().trim().charAt(0));
        optionsMenu.findItem(R.id.optAccount).setTitle("" + c);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.optRunToConnection) {

            service.submit(() -> {
                sortContacts();
                dialogContacts(null);
            });
        } else if (id == R.id.optMyIP) {
            defineIpDevice();
        } else if (id == R.id.optShowBlocked) {
            displayBlockConnectStream();
        } else if (id == R.id.optMode) {
            defineSelectModeScreen();
        } else if (id == R.id.optAccount) {
            dialogAccount();
        } else if (id == R.id.modePortrait) {
            if (!item.isChecked()) {
                changeOrientationScreen(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                changeOrientationScreen(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        } else if (id == R.id.modeLandscape) {
            if (!item.isChecked()) {

                changeOrientationScreen(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                changeOrientationScreen(ActivityInfo.SCREEN_ORIENTATION_USER);
            }

        } else if (id == R.id.optVoicePhone) {
            selOptPhone();
        } else if (id == R.id.optMail) {
            service.submit(() -> {
                List<ConnectionObject> list = getObjectsByMailArrived();
                dlgSelectConnectionForChat(list);
            });
        } else if (id == R.id.optAbout) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.optHistoryChat) {

            optionsMenu.findItem(R.id.optDelCurrentChat).setEnabled(userChatHistory != null || hostSockets.getSelectedConnectionObject() != null);
            optionsMenu.findItem(R.id.optCloseCurrChat).setEnabled(hostSockets.getSelectedConnectionObject() != null);


        } else if (id == R.id.optChooseChat) {
            getAllChats();

        } else if (id == R.id.optDelCurrentChat) {
            delCurrentChat();
        } else if (id == R.id.optCloseCurrChat) {
            offSelectedConnection();

        } else if (id == R.id.optExit) {
            exitApp();

        }

        return super.onOptionsItemSelected(item);
    }

    private void exitApp() {
        WaitConnectionsService.serviceInstance.onDestroy();
        System.exit(0);
    }

    private void selOptPhone() {
        service.submit(() -> {
            sound.stopSound(R.raw.dial);
            sortedConnectionsByPhone();

            initIndexPage(Const.PAGE_CONNECTIONS);
            updatePage();
        });
    }

    private void delCurrentChat() {
        if (userChatHistory != null) {
            long idContact = userChatHistory.getIdContact();
            String name = userChatHistory.getNameContact();

            showDlgConfirmation(name, idContact);

        } else if (hostSockets.getSelectedConnectionObject() != null) {
            long idContact = hostSockets.getSelectedConnectionObject().getContact().getId();
            String name = hostSockets.getSelectedConnectionObject().getContact().getName();

            showDlgConfirmation(name, idContact);
        }
    }

    private void showDlgConfirmation(String name, long idContact) {
        dlgConfirmation("(" + name + ")\n" + getString(R.string.del_chat), getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                service.submit(() -> {
                    int count = hostSockets.delChat(idContact);
                    if (count > 0) {

                        userChatHistory = null;

                        if (hostSockets.getSelectedConnectionObject() != null
                                && hostSockets.getSelectedConnectionObject().getContact().getId() == idContact) {
                            // если контакт текущего выбранного объекта равен контакту очистки - перевыбор для отображения чата заново
                            hostSockets.setActionSelectedConnectionObject(hostSockets.getSelectedConnectionObject());//перевыбор

                        } else {
                            clearChat();
                        }
                    }
                });
            }
        });
    }

    private void dlgConfirmation(String message, String caption, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message);
        builder.setPositiveButton(caption, listener);

        builder.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        runOnUiThread(() -> {
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }

    private void dlgSelectConnectionForChat(List<ConnectionObject> list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setIcon(R.drawable.ic_baseline_contact_mail_24);
        builder.setTitle(R.string.sel_sender);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list, null);
        builder.setView(dialogView);

        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvContactsForConnectedList);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        AdapterConnectionsByCriteria adapter = new AdapterConnectionsByCriteria(list);
        rv.setAdapter(adapter);


        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                list.clear();
            }
        });

        runOnUiThread(() -> {
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            listenerAdapterConnectionsByCriteria(adapter, alertDialog, list);
        });
    }

    private void listenerAdapterConnectionsByCriteria(AdapterConnectionsByCriteria adapter, AlertDialog dialog, List<ConnectionObject> list) {
        adapter.setSelectListener(new AdapterConnectionsByCriteria.OnSelectListener() {
            @Override
            public void onSelItem(ConnectionObject object) {

                service.execute(() -> {

                    hostSockets.setActionSelectedConnectionObject(object);

                    object.setArrivedMail(false);//убераем маил из списка
                    getObjectsByMailArrived();   //определяем заново есть ли еще у кого сообщения
                    list.clear();
                    dialog.dismiss();
                });
            }
        });
    }


    private List<ConnectionObject> getObjectsByMailArrived() {
        List<ConnectionObject> l = hostSockets.getArrivedList();
        runOnUiThread(() -> {
            optionsMenu.findItem(R.id.optMail).setVisible(l.size() != 0);
        });

        return l;
    }


    private void defineSelectModeScreen() {
        switch (screenOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                optionsMenu.findItem(R.id.modeLandscape).setChecked(false);
                optionsMenu.findItem(R.id.modePortrait).setChecked(true);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                optionsMenu.findItem(R.id.modePortrait).setChecked(false);
                optionsMenu.findItem(R.id.modeLandscape).setChecked(true);
                break;

        }

    }

    private void getAllChats() {
        service.execute(() -> {
            List<UserChat> l = hostSockets.getAllChats();
            dialogListChat(l);
        });


    }

    private void dialogListChat(List<UserChat> userChats) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list, null);
        builder.setView(dialogView);

        AdapterChatList adapterChatList = new AdapterChatList(userChats);


        RecyclerView rvChats = (RecyclerView) dialogView.findViewById(R.id.rvContactsForConnectedList);
        rvChats.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvChats.setLayoutManager(layoutManager);

        rvChats.setAdapter(adapterChatList);
        rvChats.addItemDecoration(new DividerItemDecoration(rvChats.getContext(), DividerItemDecoration.VERTICAL));

        runOnUiThread(() -> {
            AlertDialog dialog = builder.create();
            dialog.show();
            handlerAdapterChatListeners(adapterChatList, dialog);
        });

    }

    private void handlerAdapterChatListeners(AdapterChatList adapterChatList, AlertDialog dialog) {
        adapterChatList.setSelectListener(new AdapterChatList.OnSelectListener() {
            @Override
            public void onSelItemView(UserChat chat) {
                service.submit(() -> {
                    hostSockets.getHistoryChat(chat);
                    initIndexPage(Const.PAGE_CHAT);
                    updatePage();

                    dialog.dismiss();
                });
            }

            @Override
            public void onSelItemDel(DataContact contact) {
                service.submit(() -> {
                    if (contact != null)
                        showDlgConfirmation(contact.getName(), contact.getId());

                    dialog.dismiss();
                });
            }

        });
    }


    private void sortContacts() {
        System.out.println("50");
        if (hostSockets.getContactList().size() > 0) {

            Collections.sort(hostSockets.getContactList(), new Comparator<DataContact>() {
                @Override
                public int compare(final DataContact p1, final DataContact p2) {
                    return p1.getName().compareTo(p2.getName());
                }
            });
        }
    }

    private void sortedConnectionsByPhone() {
        System.out.println("51");
        if (hostSockets.getConnectionObjectsList().size() > 0) {
            for (ConnectionObject object : hostSockets.getConnectionObjectsList()) {
                if (object.getType() == Const.TYPE_CONNECT_STREAM) {

                    ConnectStream stream = (ConnectStream) object.getCommunication();
                    if (stream.getVoiceCommReqState() == Const.INCOMING_PHONE_CALL) {
                        Collections.swap(hostSockets.getConnectionObjectsList(), hostSockets.getConnectionObjectsList().indexOf(object), 0);
                    }

                } else if (object.getType() == Const.TYPE_COMMUNICATION_NODE) {

                    CommunicationNode commNode = (CommunicationNode) object.getCommunication();
                    if (commNode.getVoiceCommReqState() == Const.INCOMING_PHONE_CALL) {
                        Collections.swap(hostSockets.getConnectionObjectsList(), hostSockets.getConnectionObjectsList().indexOf(object), 0);
                    }
                }

                notifyAdapterConnections();
            }
        }
    }

    private void chooseOtherActionPhone(ConnectionObject object) {
        System.out.println("chooseOtherActionPhone");

        if (object != null) {
            switch (object.getVoiceCommReqState()) {
                case Const.MISSING_PHONE://тек. покой
                    if (isRecordAudioPermissionGranted())
                        object.prepareBeforeSendPacket(new Packet(Const.ID_OUTGOING_VOICE_COMMUNICATION_REQUEST), true);//запросить разговор

                    break;
                case Const.INCOMING_PHONE_CALL://тек. Входящий
                    if (isRecordAudioPermissionGranted())
                        object.prepareBeforeSendPacket(new Packet(Const.ID_VOICE_COMMUNICATION_REQUEST), true);//я взял трубку

                    break;
                case Const.OUTGOING_PHONE_CALL://тек. исходящий
                    object.prepareBeforeSendPacket(new Packet(Const.ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST), false);//отменить вызов
                case Const.VOICE_PHONE://тек. разговор
                    object.prepareBeforeSendPacket(new Packet(Const.ID_MISSING_VOICE_COMMUNICATION_REQUEST), false);//завершить разговор
                    break;
            }
        }
    }

    private boolean checkCameraPermissionGranted() {
        System.out.println("checkCameraPermissionGranted()");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
//            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, Const.AUDIO_RECORD_REQUEST_CODE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, Const.CAMERA_CODE_PERMISSION);
            return false;
        }
    }

    private boolean isRecordAudioPermissionGranted() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
//            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, Const.AUDIO_RECORD_REQUEST_CODE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, Const.AUDIO_RECORD_REQUEST_CODE);
            return false;
        }
    }


    private void changeOrientationScreen(int mode) {
        System.out.println("55");
        screenOrientation = mode;
        setRequestedOrientation(mode);
        updatePage();

    }

    private void dialogAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.user);
        builder.setIcon(R.drawable.ic_baseline_manage_accounts_24);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_account, null);
        builder.setView(dialogView);

        ivPhotoAccount = (ImageView) dialogView.findViewById(R.id.ivImageAccount);
        EditText etName = (EditText) dialogView.findViewById(R.id.etNameAccount);
        etName.setText(account.getName());


        loadPhotoContact(account.getPathImg(), ivPhotoAccount);

        ivPhotoAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(photoPickerIntent, Const.GET_AVATAR_TO_ACCOUNT_REQUEST);
            }
        });
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveAccount(etName);
                loadDataAccount();
                ivPhotoAccount = null;
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void saveAccount(EditText et) {
        System.out.println("56");
        saveStringPreferences(Const.PREF_NAME_ACCOUNT, et.getText().toString());

    }

    private String saveStringPreferences(String key, String value) {
        System.out.println("57");
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();

        return value;
    }


    private String getStringPreferences(String key) {
        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    private void displayBlockConnectStream() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setIcon(R.drawable.ic_baseline_block_24);
        builder.setTitle(R.string.block_connections);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_block_stream_list, null);
        builder.setView(dialogView);

        TextView tv = (TextView) dialogView.findViewById(R.id.tvNoElementsBlock);

        RecyclerView rvBlock = (RecyclerView) dialogView.findViewById(R.id.rvBlockConnectStreamList);

        rvBlock.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvBlock.setLayoutManager(layoutManager);

        AdapterBlockConnectStream adapter = new AdapterBlockConnectStream(Server.blockList);
        rvBlock.setAdapter(adapter);

        itemTouchAdapterBlockContactStream(rvBlock, tv, adapter);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void itemTouchAdapterBlockContactStream(RecyclerView rvBlockList, TextView
            tv, AdapterBlockConnectStream adapter) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Server.blockList.remove(position);
                adapter.notifyItemRemoved(position);
                if (Server.blockList.size() == 0) {
                    optionsMenu.findItem(R.id.optShowBlocked).setVisible(false);
                }

            }
        }).attachToRecyclerView(rvBlockList);//---------------прикрепить это действие к recyclerView
    }

    private void replaceMessageListInHistoryAdapter(List<HistoryMessagePacket> messages) {


        if (messages != null && adapterHistory != null) {
            System.out.println("changeMessageListInHistoryAdapter2");

            adapterHistory.setMsgList(messages);

            if (messages.size() > 0)
                binding.rvHistoryMessages.scrollToPosition(messages.size() - 1);

            adapterHistory.notifyDataSetChanged();


        }

    }

    private boolean isNumeric(String s) {
        return s.trim().matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    private void initRecyclerViewHistoryMessages(List<HistoryMessagePacket> messages) {
        binding.rvHistoryMessages.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvHistoryMessages.setLayoutManager(layoutManager);

        adapterHistory = new AdapterHistoryMessageList(messages);
        binding.rvHistoryMessages.setAdapter(adapterHistory);

        setHistoryMessageAdapterListener();
        handlerTouchAdapterHistoryMessages();

    }

    private void handlerTouchAdapterHistoryMessages() {

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int position = viewHolder.getAdapterPosition();

                HistoryMessagePacket hmp = adapterHistory.getMsgList().get(position);

                dlgDeleteMessage(hmp);
            }
        }).attachToRecyclerView(binding.rvHistoryMessages);//---------------прикрепить это действие к recyclerView
    }

    private void dlgDeleteMessage(HistoryMessagePacket hmp) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.ask_delete_msg);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (hostSockets.getSelectedConnectionObject() != null) {
                    service.submit(() -> {
                        hostSockets.getSelectedConnectionObject().deleteMessage(hmp);
                    });

                } else {
                    service.submit(() -> {
                        if (adapterHistory != null)
                            hostSockets.deleteMessage(hmp, adapterHistory.getMsgList());
                    });
                }
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                adapterHistory.notifyDataSetChanged();
            }
        });
        builder.create().show();
    }

    private void setHistoryMessageAdapterListener() {
        adapterHistory.setListeners(new AdapterHistoryMessageList.OnAdapterListener() {
            @Override
            public void onClickByLocationText(String[] sp) {
                if (sp.length == 2 &&
                        isNumeric(sp[0]) &&
                        isNumeric(sp[1])) {

                    locationDotInMap = new DataLocation(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
                    getDotLocation(locationDotInMap);

                }
            }

            @Override
            public void onOpenVideoAudioFile(MessagePacket msgPacket) {
                try {
                    if (checkStoragePermission()) {
                        openVideoAudioFile(msgPacket);
                    }
                } catch (IOException e) {
                    System.out.println("MainActivity ERROR openVideoAudioFile(msgPacket) - " + e.getMessage());
                }
            }

            @Override
            public void onOpenFile(MessagePacket msgPacket) {
                try {
                    if (checkStoragePermission()) {
                        openFile(msgPacket);
                    }
                } catch (IOException e) {
                    System.out.println("MainActivity ERROR openFile(msgPacket) - " + e.getMessage());
                }
            }

            @Override
            public void onChooseApp(MessagePacket msgPacket) {
                if (msgPacket.getFilePacket() != null) {
                    if (checkStoragePermission()) {
                        chooseAPP(msgPacket);
                    }
                }
            }
        });
    }

    private void chooseAPP(MessagePacket msgPacket) {
        System.out.println("chooseAPP");
        try {
            if (msgPacket.getFilePacket() != null)
                showActivityIntent(msgPacket, msgPacket.getFilePacket().getPathFile());
        } catch (ActivityNotFoundException activityNotFoundException) {
            if (msgPacket.getFilePacket() != null)
                Toast.makeText(this, getString(R.string.not_found_activity) + msgPacket.getFilePacket().getTypeFile(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            System.out.println("MainActivity ERROR chooseAPP(MessagePacket msgPacket) - " + e.getMessage());
        }
    }

    private void buildRecyclerViewConnections() {

        binding.rvConnectionList.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvConnectionList.setLayoutManager(layoutManager);

        adapterConnections = new AdapterConnections(hostSockets.getConnectionObjectsList());
        binding.rvConnectionList.setAdapter(adapterConnections);

        binding.rvConnectionList.addItemDecoration(new DividerItemDecoration(binding.rvConnectionList.getContext(),
                DividerItemDecoration.VERTICAL));

        handlerItemTouchHelperAdapterConnections();

        adapterConnections.setListeners(new AdapterConnections.OnListeners() {
            @Override
            public void onSelectItem(ConnectionObject object) {

                hostSockets.setActionSelectedConnectionObject(object);

            }


            @Override
            public void onCreateContactFromConnection(ConnectionObject object) {

                ConnectStream stream = (ConnectStream) object.getCommunication();

                service.submit(() -> {
                    //модифицируем ВРЕМЕННЫЙ контакт для этого обекта

                    dialogModifyContact(getString(R.string.create_contact),
                            stream.getContact(),
                            Const.ADD_CONTACT,
                            object);
                });

            }

            @Override
            public void onNewActionPhone(ConnectionObject object) {

                service.submit(() -> {
                    chooseOtherActionPhone(object);
                });
            }

            @Override
            public void onClickInfoMissedCalls(List<Long> list, int position) {

                StringBuilder msg = new StringBuilder("\n");
                for (Long data : list) {
                    msg.append(Const.formatTime.format(data)).append("\n\n");
                }
                list.clear();
                adapterConnections.notifyItemChanged(position);

                showMissedCalls(msg.toString());
            }

            @Override
            public void onSelectAvatar(ConnectionObject object) {
                service.submit(() -> {
                    if (object.getContact().getId() > 0) {

                        dialogModifyContact(getString(R.string.edit_contact),
                                object.getContact(),
                                Const.EDIT_CONTACT,
                                object);


                    } else {
                        dialogModifyContact(getString(R.string.create_contact),
                                object.getContact(),
                                Const.ADD_CONTACT,
                                object);
                    }
                });
            }
        });
    }


    private void initSelectedConnectionObject(ConnectionObject object) {

        userChatHistory = null;

        //инициализируем массив
        getMessagesInSelectedConnections(object);

        drawRecipientFromSelectedConnectionObject(object.getContactName());

        initIndexPage(Const.PAGE_CHAT);

        updatePage();
    }


    private void showMissedCalls(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setIcon(android.R.drawable.sym_call_missed);
        builder.setTitle(R.string.missed_call);
        builder.setMessage(msg);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void getMessagesInSelectedConnections(ConnectionObject selConnObj) {
        System.out.println("getMessagesInSelectedConnections()1");

        List<HistoryMessagePacket> l = selConnObj.initHistoryMessagePackets();
        runOnUiThread(() -> {
            replaceMessageListInHistoryAdapter(l);
        });

    }

    private void handlerItemTouchHelperAdapterConnections() {

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                System.out.println("67");
                ConnectionObject co = hostSockets.getConnectionObjectsList().get(position);
                if (co.getType() == Const.TYPE_CONNECT_STREAM) {
                    dlgBlockConnectStream((ConnectStream) co.getCommunication());
                } else if (co.getType() == Const.TYPE_COMMUNICATION_NODE) {
                    dlgDropCommNode((CommunicationNode) co.getCommunication());
                }
            }
        }).attachToRecyclerView(binding.rvConnectionList);//---------------прикрепить это действие к recyclerView
    }

    private void dlgDropCommNode(CommunicationNode cn) {
        System.out.println("68");
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.ask_unconnect);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.unconnect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cn.contact.setRestart(false);
                cn.closeCommNode(true);
            }
        });
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                adapterConnections.notifyDataSetChanged();
            }
        });
        builder.create().show();
    }

    private void dlgBlockConnectStream(ConnectStream cs) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.ask_block_contact);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.block, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                hostSockets.blockConnectStream(cs);
            }
        });

        builder.setNeutralButton(R.string.unconnect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                service.submit(cs::dropStream);
            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                adapterConnections.notifyDataSetChanged();
            }
        });
        builder.create().show();
    }

    private void drawRecipientFromSelectedConnectionObject(String nameRecipient) {
        runOnUiThread(() -> {

            binding.tvRecipientMessage.setText(nameRecipient);

            binding.tvTitleRecipient.setVisibility(View.VISIBLE);

        });
    }

    private void drawRecipientFromDisconnectObject() {
        runOnUiThread(() -> {

            binding.tvRecipientMessage.setText("");

            binding.tvTitleRecipient.setVisibility(View.VISIBLE);

            binding.llBlockPhone.setVisibility(View.GONE);

        });
    }

    private void dialogContacts(ConnectionObject obj) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setIcon(R.drawable.ic_baseline_connect_without_contact_24);
        builder.setTitle(R.string.outgoing_connection);
        builder.setCancelable(false);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list, null);
        builder.setView(dialogView);

        RecyclerView rvContacts = (RecyclerView) dialogView.findViewById(R.id.rvContactsForConnectedList);
        rvContacts.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvContacts.setLayoutManager(layoutManager);


        if (obj != null)
            sortedByContact(obj.getCurrentIP());

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (adapterContacts != null && pageIndex == Const.PAGE_CONNECTIONS)
                    adapterConnections.notifyDataSetChanged();

//                adapterContacts = null;
            }
        });

        builder.setNeutralButton(getString(R.string.add_contact), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        runOnUiThread(() -> {
            adapterContacts = new AdapterContacts(hostSockets.getContactList());
            handlerAdapterContactsListeners(obj);

            rvContacts.setAdapter(adapterContacts);
            rvContacts.addItemDecoration(new DividerItemDecoration(rvContacts.getContext(), DividerItemDecoration.VERTICAL));

            itemTouchHelperAdapterContacts(rvContacts);

            AlertDialog dialog = builder.create();
            dialog.show();


            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    service.submit(() -> {
                        dialogModifyContact(getString(R.string.create_contact), new DataContact(0, "",
                                        "",
                                        Const.NO_CONNECT_CONTACT_STATUS,
                                        "android.resource://" + App.context.getPackageName() + "/drawable/ic_baseline_account_circle_24",
                                        "",
                                        null,
                                        ""),
                                Const.ADD_CONTACT, null);
                    });
                }
            });

        });

    }

    private void sortedByContact(String ip) {
        if (hostSockets.getContactList().size() > 0) {
            for (DataContact object : hostSockets.getContactList()) {
                if (object.getIp().equals(ip)) {
                    Collections.swap(hostSockets.getContactList(), hostSockets.getContactList().indexOf(object), 0);
                }
            }
        }
    }


    private void handlerAdapterContactsListeners(ConnectionObject obj) {
        adapterContacts.setSelectListener(new AdapterContacts.OnSelectListener() {

            @Override
            public synchronized void onCmdRunConnection(DataContact contact) {
                service.submit(() -> {
                    System.out.println("1 - " + Thread.currentThread().getName() + "/ " + contact.getName());
                    hostSockets.startConnectionByContact(contact);
                });

            }

            @Override
            public void onEditItem(DataContact contact) {
                service.submit(() -> {
                    dialogModifyContact(getString(R.string.edition), contact, Const.EDIT_CONTACT, obj);
                });
            }

            @Override
            public void onNotifyConnection(DataContact contact) {
                notifySoundConnection(contact);
            }

            @Override
            public void onCloseConnection(DataContact contact) {

                hostSockets.closeConnectByContact(contact, true);

            }
        });
    }


    private void notifySoundConnection(DataContact contact) {
        System.out.println("71");
        if (contact.isNotificationSoundOfConnection()) {
            Toast.makeText(getApplicationContext(), R.string.notifycation_on, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.notifycation_off, Toast.LENGTH_SHORT).show();
            sound.stopSound(R.raw.connect);
        }
    }


    private void itemTouchHelperAdapterContacts(RecyclerView rvList) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                service.submit(() -> {
                    if (!hostSockets.deleteContact(viewHolder.getAdapterPosition())) {
                        runOnUiThread(() -> {
                            if (adapterContacts != null)
                                adapterContacts.notifyDataSetChanged();
                        });
                    }
                });
            }
        }).attachToRecyclerView(rvList);//---------------прикрепить это действие к recyclerView
    }


    private void dialogModifyContact(String title, DataContact contact,
                                     int flg, ConnectionObject object) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        if (flg == Const.ADD_CONTACT) {
            builder.setIcon(R.drawable.ic_baseline_person_add_24);
        } else if (flg == Const.EDIT_CONTACT) {
            builder.setIcon(R.drawable.ic_baseline_contact_page_24);
        }

        builder.setTitle(title);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_modify_contact, null);
        builder.setView(dialogView);

        ivPhotoContact = dialogView.findViewById(R.id.ivPhotoContact);
        if (contact.getBytesPhoto() != null && contact.getBytesPhoto().length > 0) {//поумолчанию из байт
            Bitmap bitmap = BitmapFactory.decodeByteArray(contact.getBytesPhoto(),
                    0,
                    contact.getBytesPhoto().length);
            ivPhotoContact.setImageBitmap(bitmap);
        } else {//из файла
            loadPhotoContact(contact.getPathAvatar(), ivPhotoContact);
        }

        EditText etName = (EditText) dialogView.findViewById(R.id.etName);
        EditText etIP = (EditText) dialogView.findViewById(R.id.etIP);

        etName.setText(contact.getName());
        etIP.setText(contact.getIp());

        ivPhotoContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(photoPickerIntent, Const.GET_AVATAR_TO_CONTACT_REQUEST);
            }
        });

        builder.setPositiveButton(R.string.perform, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                service.submit(() -> {

                    contact.setIp(etIP.getText().toString());
                    contact.setName(etName.getText().toString());
                    contact.setStatus(contact.getStatus());
                    contact.setPathAvatar(ivPhotoContact.getTag() == null ? "" : (String) ivPhotoContact.getTag());
                    //если фото выбранно байты убераем
                    if (ivPhotoContact.getTag() != null)
                        contact.setBytesPhoto(null);
                    //contact.путь файла( сохраняется hostSockets.addOrChangeContact)

                    //сохранение/изменение в базе данных
                    if (hostSockets.addOrChangeContactToDB(contact)) {
                        //объект переданн из ObjectConnections
                        if (object != null)
                            object.initAttachContact();//получаем прикрепленный контакт

                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), R.string.successfully, Toast.LENGTH_SHORT).show();
                        });
                    } else {

                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        runOnUiThread(() -> {
            builder.create().show();
        });
    }

    private void loadPhotoContact(String pathF, ImageView view) {
        System.out.println("72");
        try {
            File f = new File(pathF);
            if (f.exists()) {
                if (checkStoragePermission()) {
                    view.setImageURI(Uri.fromFile(f));
                    view.setTag(pathF);
                }
            } else {
                view.setImageResource(R.drawable.ic_baseline_account_circle_24);
            }
        } catch (Exception e) {
            System.out.println("MainActivity -> loadPhotoContact(DataContact contact, ImageView ivPhotoContact) ERROR : - " + e.getMessage());
        }
    }

    private boolean checkStoragePermission() {
        System.out.println("73");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, Const.STORAGE_REQUEST_PERMISSION);
            return false;
        }
    }


    private void openFile(MessagePacket msgPacket) throws IOException {

        if (msgPacket.getFilePacket() != null) {
            if (msgPacket.getFilePacket().getTypeFile().equals("pdf")) {

                openPDF(msgPacket.getFilePacket().getPathFile());
            } else {

                chooseAPP(msgPacket);
            }
        }
    }

    private void showActivityIntent(MessagePacket msgPacket, String pathFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", new File(pathFile));
            if (msgPacket.getFilePacket() != null)
                intent.setDataAndType(uri, msgPacket.getFilePacket().getMimeType());
        } else {
            if (msgPacket.getFilePacket() != null)
                intent.setDataAndType(Uri.fromFile(new File(pathFile)), msgPacket.getFilePacket().getMimeType());
        }

        startActivity(intent);
    }

    private void openVideoAudioFile(MessagePacket msgPacket) throws IOException {

        if (msgPacket.getFilePacket() != null)
            playFile(msgPacket.getFilePacket().getPathFile());
    }


    private void playFile(final String path) {
        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);

    }

    private void openPDF(final String path) {
        Intent intent = new Intent(MainActivity.this, PdfActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Const.SELECT_DOT_REQUEST_CODE:
                if (resultCode == Const.RESULT_OK) {
                    DataLocation dot = data.getParcelableExtra("location");
                    setDotPosition(dot);
                }
                break;
            case Const.CAMERA_CODE_PERMISSION:
                if (resultCode == Const.RESULT_OK) {
                    writeVideoFile();
                }
                break;

            case Const.ACTION_VIDEO_REQUEST:
            case Const.ATTACH_FILES_REQUEST:
            case Const.INTENT_RECORDING_VOICE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    File f = new File(uri.getEncodedPath());
                    long s = f.length();
                    startThreadDefineAttachedFile(uri);
                }
                break;
            case Const.GET_AVATAR_TO_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    saveStringPreferences(Const.PREF_PATH_IMG_ACCOUNT, getRealPathFromURI(uri));
                    loadPhotoContact(getRealPathFromURI(uri), ivPhotoAccount);
                }
                break;
            case Const.GET_AVATAR_TO_CONTACT_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    loadPhotoContact(getRealPathFromURI(uri), ivPhotoContact);

                }
                break;
        }
    }


    private synchronized String getRealPathFromURI(Uri uri) {
        System.out.println("getRealPathFromURI");
        String path = "";
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Files.FileColumns.DATA};
            cursor = getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            cursor.moveToFirst();

            path = cursor.getString(column_index);
            return path;
        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private synchronized long getSizeFileURI(Uri uri) {
        System.out.println("getSizeFileURI");

        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Files.FileColumns.SIZE};
            cursor = getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            cursor.moveToFirst();

            return cursor.getLong(column_index);

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

    }

    private void startThreadDefineAttachedFile(Uri uri) {
        System.out.println("startThreadDefineAttachedFile");
        showBlock(true);
        service.submit(() -> {
            try {

                defineAttachedFile(uri);
            } catch (IOException e) {
                System.out.println("MainActivity ERROR startThread(Uri uri) - " + e.getMessage());
                showBlock(false);
            }
        });

    }

    private void showBlock(boolean b) {

        runOnUiThread(() -> {
            if (b) {

                binding.progressBar.setIndeterminate(true);
                binding.flMapBlock.setVisibility(View.VISIBLE);
                binding.tvProgressInfo.setText(R.string.load_file);


            } else {

                binding.progressBar.setIndeterminate(false);
                binding.progressBar.setMax(1);
                binding.progressBar.setProgress(1);

                binding.tvProgressInfo.setText(R.string.completed);
                binding.flMapBlock.setVisibility(View.INVISIBLE);
            }
        });
    }


    private void defineAttachedFile(Uri uri) throws IOException {
        System.out.println("defineAttachedFile");

        String type = getFileExtension(uri);
        String mimeType = getMimeType(uri);
        String nameFile = getFileNameFromCursor(uri);
        String fPath = getRealPathFromURI(uri);

        byte[] buffer = getBytesFile(uri);

        showBlock(false);
        if (buffer.length != 0) {
            dialogSendAttachFile(new AttachedFilePacket(mimeType, buffer, type, fPath, nameFile));
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.file_no_accept, Toast.LENGTH_LONG).show();
            });
        }

    }


    private byte[] getBytesFile(Uri uri) throws IOException {

        System.out.println("getBytesFile");

        InputStream inputStream = getContentResolver().openInputStream(uri);

        long size = getSizeFileURI(uri);
        int bufferSize = (int) size;
        System.out.println("getBytesFile 1");
        try {
            byte[] buffer = new byte[bufferSize];
            inputStream.read(buffer);
            inputStream.close();

            return buffer;
        } catch (OutOfMemoryError e) {
            System.out.println("MainActivity->getBytesFile ERROR OutOfMemoryError " + e.getMessage());
            showBlock(false);

//            double normalD = (double) Math.round((maxSize / 1000000) * 1000d) / 1000d;
            runOnUiThread(() -> {

                showSnackBar(e.getMessage(),
                        getString(R.string.ok), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        });
            });
            return new byte[0];
        }
    }

    private void dialogSendAttachFile(AttachedFilePacket filePacket) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setIcon(R.drawable.ic_baseline_attach_file_24);
        builder.setTitle(R.string.send_file);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_send_file, null);

        TextView tvAttachFile = dialogView.findViewById(R.id.tvAttachFile);
        EditText text = dialogView.findViewById(R.id.etTitleForAttachFile);

        tvAttachFile.setText(filePacket.getNameFile());
        builder.setView(dialogView);

        builder.setPositiveButton(R.string.perform, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                service.execute(() -> {

                    if (hostSockets.getSelectedConnectionObject() != null) {

                        sendToSelectedConnection(hostSockets.getSelectedConnectionObject(), text.getText().toString(),
                                new AttachedFilePacket(filePacket.getMimeType(),
                                        filePacket.getBytesAttachedFile(),
                                        filePacket.getTypeFile(),
                                        filePacket.getPathFile(),
                                        filePacket.getNameFile()));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), R.string.not_selected_connection, Toast.LENGTH_LONG).show();
                            showBlock(false);
                        });
                    }
                });
            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showBlock(false);
            }
        });

        runOnUiThread(() -> {
            builder.create().show();
        });
    }


    public String getFileExtension(Uri uri) {
        System.out.println("getFileExtension");
        String mimeType = getMimeType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    public String getMimeType(Uri uri) {
        return getContentResolver().getType(uri);
    }

    public String getFileNameFromCursor(Uri uri) {
        System.out.println("getFileNameFromCursor 1");
        Cursor fileCursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        String fileName = null;
        if (fileCursor != null && fileCursor.moveToFirst()) {

            int cIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (cIndex != -1) {
                fileName = fileCursor.getString(cIndex);
            }
            fileCursor.close();
        }
        System.out.println("getFileNameFromCursor 3");
        return fileName;
    }


    @Override
    public void onBackPressed() {
//        showBlock(false);
    }

    private void startRecordVoiceActivity() {
        System.out.println("startRecordVoiceActivity");
        Intent intent = new Intent();
        intent.setAction(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        try {
            startActivityForResult(intent, Const.INTENT_RECORDING_VOICE);
        } catch (Exception e) {

            System.out.println("MainActivity ERROR startRecordVoiceActivity() - " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}