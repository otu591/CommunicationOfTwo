package com.alexei.communicationoftwo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.Const;

import java.io.File;
import java.io.IOException;

public class PdfActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_PERMISSION = 1;

    private String path;
    private String fName;
    private ImageView imgView;
    private Button btnPrevious, btnNext;
    private int currentPage = 0;
    private ImageButton btn_zoomin, btn_zoomout;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page curPage;
    private ParcelFileDescriptor descriptor;
    private float currentZoomLevel = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        if (intent != null) {
            path = intent.getStringExtra("path");
//            setTitle(intent.getStringExtra("title"));
        }

        imgView = findViewById(R.id.imgView);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btn_zoomin = findViewById(R.id.zoomin);
        btn_zoomout = findViewById(R.id.zoomout);
        // устанавливаем слушатели на кнопки
        btnPrevious.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btn_zoomin.setOnClickListener(this);
        btn_zoomout.setOnClickListener(this);

        // если в банлде есть номер страницы - забираем его
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(Const.CURRENT_PAGE, 0);
        }


    }


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            readStart();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readStart();
                } else {
                    // в разрешении отказано (в первый раз, когда чекбокс "Больше не спрашивать" ещё не показывается)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        finish();
                    }
                    // в разрешении отказано (выбрано "Больше не спрашивать")
                    else {
                        // показываем диалог, сообщающий о важности разрешения
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.check_permission)
                                // при согласии откроется окно настроек, в котором пользователю нужно будет вручную предоставить разрешения
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                })
                                // закрываем приложение
                                .setNegativeButton(R.string.canceled, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });
                        builder.setCancelable(false);
                        builder.create().show();
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (curPage != null) {
            outState.putInt(Const.CURRENT_PAGE, curPage.getIndex());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            readStart();
        }
    }

    private void readStart() {
        try {
            openPdfRenderer();
            displayPage(currentPage);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfRenderer() {
        File file = new File(path);
        fName = file.getName();

        descriptor = null;
        pdfRenderer = null;
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(descriptor);
            setTitle(fName + " (" + (currentPage + 1) + "/" + pdfRenderer.getPageCount() + ")");
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayPage(int index) {
        if (pdfRenderer.getPageCount() <= index) return;
        Bitmap bitmap;
        // закрываем текущую страницу
        if (curPage != null) curPage.close();
        // открываем нужную страницу
        curPage = pdfRenderer.openPage(index);


        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // определяем размеры Bitmap
            int newWidth = (int) (getResources().getDisplayMetrics().widthPixels * curPage.getWidth() / 72
                    * currentZoomLevel / 45);//45

            int newHeight = (int) (getResources().getDisplayMetrics().heightPixels * curPage.getHeight() / 72
                    * currentZoomLevel / 70);//90
            bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        } else {
            // определяем размеры Bitmap
            int newWidth = (int) (getResources().getDisplayMetrics().widthPixels * curPage.getWidth() / 72
                    * currentZoomLevel / 60);//45

            int newHeight = (int) (getResources().getDisplayMetrics().heightPixels * curPage.getHeight() / 72
                    * currentZoomLevel / 45);//90
            bitmap = Bitmap.createBitmap(newWidth,newHeight , Bitmap.Config.ARGB_8888);
        }

//        Bitmap bitmap = Bitmap.createBitmap(
//                getResources().getDisplayMetrics().densityDpi * curPage.getWidth() / 72,
//                getResources().getDisplayMetrics().densityDpi * curPage.getHeight() / 72,
//                Bitmap.Config.ARGB_8888);

//        Matrix matrix = new Matrix();
//        float dpiAdjustedZoomLevel = currentZoomLevel * DisplayMetrics.DENSITY_LOW
//                / getResources().getDisplayMetrics().densityDpi;
//        matrix.setScale(dpiAdjustedZoomLevel, dpiAdjustedZoomLevel);

        curPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // отображаем результат рендера
        imgView.setImageBitmap(bitmap);
        // проверяем, нужно ли делать кнопки недоступными
        int pageCount = pdfRenderer.getPageCount();
        btnPrevious.setEnabled(0 != index);
        btnNext.setEnabled(index + 1 < pageCount);
        btn_zoomout.setEnabled(currentZoomLevel != 2);
        btn_zoomin.setEnabled(currentZoomLevel != 50);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPrevious: {
                // получаем индекс предыдущей страницы
                int index = curPage.getIndex() - 1;
                displayPage(index);
                setTitle(fName + " (" + (index + 1) + "/" + pdfRenderer.getPageCount() + ")");
                break;
            }
            case R.id.btnNext: {
                // получаем индекс следующей страницы
                int index = curPage.getIndex() + 1;
                displayPage(index);
                setTitle(fName + " (" + (index + 1) + "/" + pdfRenderer.getPageCount() + ")");
                break;
            }
            case R.id.zoomout: {
                // уменьшаем зум
                --currentZoomLevel;
                displayPage(curPage.getIndex());
                break;
            }
            case R.id.zoomin: {
                // увеличиваем зум
                ++currentZoomLevel;
                displayPage(curPage.getIndex());
                break;
            }
        }
    }

    @Override
    public void onStop() {
        try {
            closePdfRenderer();
        } catch (IOException e) {
            System.out.println("PdfActivity ERROR onStop() - " + e.getMessage());
        }
        super.onStop();
    }

    private void closePdfRenderer() throws IOException {
        if (curPage != null) curPage.close();
        if (pdfRenderer != null) pdfRenderer.close();
        if (descriptor != null) descriptor.close();
    }

}