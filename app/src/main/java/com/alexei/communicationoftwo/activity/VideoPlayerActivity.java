package com.alexei.communicationoftwo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.alexei.communicationoftwo.R;

import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private VideoView vw;
    private String path;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Intent intent= getIntent();
        if (intent!=null){
            path=intent.getStringExtra("path");
            vw = (VideoView)findViewById(R.id.vidvw);
            vw.setMediaController(new MediaController(this));
            vw.setOnCompletionListener(this);

            setVideo(path);
//            setVideo(videolist.get(0));
        }

    }

    public void setVideo(String path)
    {
        Uri uri = Uri.parse(path);
        vw.setVideoURI(uri);
        vw.start();
    }

    public void onCompletion(MediaPlayer mp)
    {
        AlertDialog.Builder obj = new AlertDialog.Builder(this);
        obj.setTitle(R.string.finish_play);
        obj.setIcon(R.mipmap.ic_launcher);
        MyListener m = new MyListener();
        obj.setPositiveButton(R.string.replay, m);
        obj.show();
    }

    class MyListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which)
        {
            if (which == -1) {
                vw.seekTo(0);
                vw.start();
            }
            else {

                setVideo(path);
            }
        }
    }
}