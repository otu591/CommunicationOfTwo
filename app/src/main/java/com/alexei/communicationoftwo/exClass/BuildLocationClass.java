package com.alexei.communicationoftwo.exClass;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alexei.communicationoftwo.App;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


public class BuildLocationClass{

    private static BuildLocationClass instance;

    private final FusedLocationProviderClient fusedLocationClient;//   класс для определения местоположения
    private SettingsClient settingsClient;//настройки клиента
    private LocationRequest locationRequest;//сохраненние данных запроса -FusedLocationProviderClient Api
    private LocationSettingsRequest locationSettingsRequest; //определение настроек пользователя на данный момент
    private LocationCallback locationCallback;//определение событий в определении местоположения

    public Location currentLocation = null;

    private boolean isLocationUpdatesActive;
    private int inUse = 0;

    private long interval;
    private long fastInterval;
    private long intervalMeter;


    public OnLocationListeners onLocationListener;


    public interface OnLocationListeners {

        void onUpdateLocation(Location location, int satellites);

        void onRunning();

        void onStop();

        void onGetPermission(Exception e);

        void onChangeUnavailable();
    }

    public void setOnListeners(OnLocationListeners listener) {
        this.onLocationListener = listener;
    }



    public static synchronized BuildLocationClass getInstance() {
        if (instance == null) {
            instance = new BuildLocationClass();
        }
        return instance;
    }

    public BuildLocationClass() {
        this.interval = 5000;
        this.fastInterval = 5000;
        this.intervalMeter = 5;

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(App.context);
        this.settingsClient = LocationServices.getSettingsClient(App.context);

        this.buildLocationRequest();
        this.buildLocationSettingsRequest();
        this.buildLocationCallBack();
    }



    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastInterval);
        locationRequest.setSmallestDisplacement(intervalMeter);//5meter
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//высокая точность
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();//---Возвращает самое новые значения последнее доступное местоположение в этом результате или null, если местоположения недоступны.

                onLocationListener.onUpdateLocation(currentLocation, inUse);

            }
        };
    }


    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);//-----------------Добавляет коллекцию, LocationRequests которая созданна в методе buildLocationRequest().
        locationSettingsRequest = builder.build();//-------------------Создает LocationSettingsRequest, который можно использовать с SettingsApi.

    }

    public void stopLocationUpdate() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    isLocationUpdatesActive = false;
                    currentLocation = null;
                    recoveryResources();
                    onLocationListener.onStop();
                }
            });
        }
    }

    public void startLocationUpdate() {
        //проверка установлены ли соответствующие настройки
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {

            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                //если отсутствуют какие-то разрешения
                if (ContextCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                } else {
                    return;
                }
                //запрос местоположения в отдельном потоке ,результат  методе onLocationResult в созданном объекте callback: locationCallback = new LocationCallback() {
                //лупер текущего потока работающее непрерывно(c настройками locationRequest) (задача в отдельном потоке)

                isLocationUpdatesActive = true;
                onLocationListener.onRunning();
            }
        }).addOnFailureListener(new OnFailureListener() {// ----------произошла неудача настройки
            @Override
            public void onFailure(@NonNull Exception e) {
                //   определение причины неудачи
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED://-------------------требуется разрешение от пользователя
                        stopLocationUpdate();
                        onLocationListener.onGetPermission(e);
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE://-----------невозможно изменить из приложения эти настройки(нужно установить в ручную)
                        onLocationListener.onChangeUnavailable();
                        isLocationUpdatesActive = false;
                        break;
                }
            }
        });
    }


    @Override
    protected void finalize() throws Throwable {

        try {
            stopLocationUpdate();
        } finally {
            super.finalize();
        }
    }

    public void recoveryResources() {
        stopLocationUpdate();
    }


}

