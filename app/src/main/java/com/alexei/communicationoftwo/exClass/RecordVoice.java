package com.alexei.communicationoftwo.exClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.alexei.communicationoftwo.App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordVoice {

    //    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private final ExecutorService service;

    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private AudioTrack track;
    private DatagramSocket socketSpeak;
    private DatagramSocket socketMic;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_IN_MONO = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUF_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN_MONO, AUDIO_FORMAT);
    private boolean bMic;
    private boolean speakers;
    private final String toIP;
    private final int port = 51737;


    //--------------------------
    private OnListeners onListeners;


    public interface OnListeners {
        void onStartRec();

        void onBreakVoice();

        void onChangeStateSpeakerphone(boolean speakerphoneOn);
    }

    public void setOnListener(OnListeners listeners) {
        this.onListeners = listeners;
    }

    //------------------------------------------
    public RecordVoice(String toIP) {
        System.out.println("RecordVoice 1");
        this.service = Executors.newFixedThreadPool(2);
        this.toIP = toIP;

        service.submit(this::startMic);

    }

    public void mute() {
        System.out.println("RecordVoice 2");
        bMic = false;
        speakers = false;
    }

    @SuppressLint("MissingPermission")
    private void startMic() {
        System.out.println("RecordVoice 3");
        try {

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN_MONO, AUDIO_FORMAT, BUF_SIZE);

            InetAddress IPAddress = InetAddress.getByName(toIP);
            byte[] buf = new byte[BUF_SIZE];
            int bytes_read;

            socketMic = new DatagramSocket();

            audioRecord.startRecording();

            service.submit(this::startSpeakers);

            bMic = true;
            while (bMic) {
                bytes_read = audioRecord.read(buf, 0, BUF_SIZE);
                DatagramPacket sendPacket = new DatagramPacket(buf, bytes_read, IPAddress, port);
                socketMic.send(sendPacket);
            }

        } catch (IOException e) {
            System.out.println("RecordVoice ERROR startMic() - " + e.getMessage());

        } finally {

            recoverResources();

        }
    }



    private void startSpeakers() {
        System.out.println("RecordVoice 6");


//STREAM_MUSIC
//MODE_NORMAL

        track = new AudioTrack(AudioManager.MODE_NORMAL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUF_SIZE,
                AudioTrack.MODE_STREAM);

        try {
            track.play();

            socketSpeak = new DatagramSocket(null);
            socketSpeak.setReuseAddress(true);
            socketSpeak.setBroadcast(true);
            socketSpeak.bind(new InetSocketAddress(port));

            byte[] buf = new byte[BUF_SIZE];

            onListeners.onStartRec();

            speakers = true;
            while (speakers) {

                DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                socketSpeak.receive(packet);
                track.write(packet.getData(), 0, BUF_SIZE);
            }

        } catch (IOException e) {
            System.out.println("RecordVoice ERROR startSpeakers() - " + e.getMessage());

        } finally {

            recoverResources();

        }
    }

    private synchronized void recoverResources() {

        System.out.println("recoverResources() - " + Thread.currentThread().getName());

        bMic = false;
        speakers = false;
        try {
            if (socketSpeak != null) {
                socketSpeak.close();
                socketSpeak.disconnect();
            }

            if (socketMic != null) {
                socketMic.disconnect();
                socketMic.close();
            }

            if (track != null) {
                track.stop();
                track.flush();
                track.release();
            }

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
            }

        } catch (Exception e) {
            System.out.println("recoverResources() ERROR - " + e.getMessage());
        } finally {
            if (onListeners != null) {
                onListeners.onBreakVoice();
            }

            if (service != null) {
                service.shutdown();
            }
        }
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("RecordVoice finalize()");
    }
}
