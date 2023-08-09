package com.alexei.communicationoftwo.exClass;

import android.media.MediaPlayer;

import com.alexei.communicationoftwo.App;

import java.util.HashMap;
import java.util.Map;

public class SoundClass {
    private static SoundClass instance;

    private Map<Integer, MediaPlayer> mapSounds = new HashMap<>();

    public static synchronized SoundClass getInstance() {
        if (instance == null) {
            instance = new SoundClass();
        }
        return instance;
    }

    public SoundClass() {

    }

    public void startSound(int res, boolean loop) {

        if (mapSounds.get(res) == null || (!loop)) {
            MediaPlayer mp = MediaPlayer.create(App.context, res);
            mp.setLooping(loop);
            mp.start();

            mapSounds.put(res, mp);
        }
    }

    public void stopSound(Integer res) {
        try {
            if (mapSounds.get(res) != null) {
                mapSounds.get(res).release();
                mapSounds.put(res, null);
            }
        } catch (Exception e) {
            System.out.println("SoundClass ERROR stopSound(Integer res) - " + e.getMessage());
        }

    }

    public void recoveryRes() {
        for (Map.Entry<Integer, MediaPlayer> entry : mapSounds.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().release();
            }
        }
        mapSounds.clear();
    }

}
