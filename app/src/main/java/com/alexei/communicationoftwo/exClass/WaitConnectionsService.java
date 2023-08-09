package com.alexei.communicationoftwo.exClass;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;

import androidx.annotation.Nullable;

import com.alexei.communicationoftwo.activity.MainActivity;
import com.alexei.communicationoftwo.socket.HostConnections;

public class WaitConnectionsService extends Service {
    public static WaitConnectionsService serviceInstance;
    public static HostConnections hostSockets=HostConnections.getInstance();


    public WaitConnectionsService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceInstance = this;
        hostSockets = HostConnections.getInstance();
        System.out.println("WaitConnectionsService onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("WaitConnectionsService onStartCommand");
        MainActivity.getInstance().initHosts();
        if (Build.VERSION.SDK_INT >= 9) {
            StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
                super.onDestroy();
        System.out.println("WaitConnectionsService onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}