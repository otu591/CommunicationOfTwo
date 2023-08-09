package com.alexei.communicationoftwo.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.alexei.communicationoftwo.App;
import com.alexei.communicationoftwo.BuildConfig;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.database.AppDB;
import com.alexei.communicationoftwo.databinding.ActivityAboutBinding;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class AboutActivity extends AppCompatActivity {
    private ActivityAboutBinding binding;
    private ExecutorService service;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        service = Executors.newFixedThreadPool(3);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getCurrentVersion();
        service.execute(this::getSizeChat);
    }

    private void getSizeChat() {

        String path = getFilesDir() + "/" + Const.PATH_STORAGE;
        double size = getSizeFile(path);
        double n = getNumberDouble(size / (1024 * 1024));//mb
        runOnUiThread(() -> {
            binding.tvTitleSizeChat.setText(String.format(getString(R.string.size_files_chat), "" + n));
        });
    }

    private double getNumberDouble(double v) {
        return (double) Math.round(v * 1000d) / 1000d;
    }

    private double getSizeFile(String path) {
        double size = 0;
        File[] files = getFiles(path);
        for (File f : files) {
            size += f.length();
        }
        return size;
    }

    private void getCurrentVersion() {
        binding.tvVersion.setText(getString(R.string.curr_ver));
        binding.tvVersion.append(BuildConfig.VERSION_NAME);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }


    private void getVersionRequest() {

    }




    public void getAvailableVersion(View view) {
        getVersionRequest();
    }

    public void displayInputMessage(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);

        builder.setIcon(R.drawable.ic_baseline_message_24);
        builder.setTitle(R.string.send_msg);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_input_message, null);
        EditText et = dialogView.findViewById(R.id.etTextSupport);
        builder.setView(dialogView);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String msg = et.getText().toString().trim();
                if (msg.length() > 4) {

//                    sendEmailVerificationCode(msg);

                }
            }
        });


        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    public void clearFileChat(View view) {
        service.execute(() -> {
            String path = getFilesDir() + "/" + Const.PATH_STORAGE;
            deleteFiles(path);
            getSizeChat();
        });
    }

    private void deleteFiles(String path) {
        File[] files = getFiles(path);
        for (File f : files) {
            f.delete();
        }
    }

    private synchronized File[] getFiles(String path) {
        File dir = new File(path);
        ///data/user/0/com.alexei.communicationoftwo/files/storage
        if (dir.exists()) {
            return dir.listFiles();
        }
        return new File[0];
    }
}