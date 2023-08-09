package com.alexei.communicationoftwo;

import android.media.ExifInterface;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Util {
    public static String saveFileToStore(String type, byte[] bytes, String path) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                boolean b = dir.mkdirs();
            }
            File file = getGenerateFile(type, path);
            OutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            return file.getPath();
        } catch (Exception e) {
            System.out.println("CommunicationNode->saveFileToStore ERROR - " + e.getMessage());
            return "";
        }
    }

    public static String saveFileAvatar(byte[] bytes, String path, String name) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                boolean b = dir.mkdirs();
            }
            File file = new File(dir + "/" + name + ".jpg");
            OutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            return file.getPath();
        } catch (Exception e) {
            System.out.println("Util->saveFileAvatar ERROR - " + e.getMessage());
            return "android.resource://" + App.context.getPackageName() + "/drawable/ic_baseline_account_circle_24";
        }
    }

    @NonNull
    public static synchronized File getGenerateFile(String type, String path) {
        File file = new File(path, System.currentTimeMillis() + "." + type);
        int count = 0;
        while (file.exists()) {
            file = new File(path, System.currentTimeMillis() + "_" + (count++) + "." + type);
        }
        return file;
    }

    public static synchronized long getFolderSize(File dir) {
        long size = 0;
        try {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    System.out.println(file.getName() + " " + file.length());
                    size += file.length();
                } else
                    size += getFolderSize(file);
            }
        } catch (Exception e) {
            System.out.println("Util->getFolderSize(File dir) ERROR  - " + e.getMessage());
        }

        return size;
    }

    public static String generateKey() {
        UUID idOne = UUID.randomUUID();
        return "" + idOne;
    }

    public static synchronized byte[] getByteOfFile(Uri uri, long size) throws Exception, OutOfMemoryError {

        InputStream inputStream = App.context.getContentResolver().openInputStream(uri);

        byte[] buffer = new byte[(int) size];
        inputStream.read(buffer);
        inputStream.close();

        return buffer;
    }


    public synchronized static void defineOrientationBitmap(String filePath, ImageView ivBitmapFile) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                ivBitmapFile.setRotation(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                ivBitmapFile.setRotation(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                ivBitmapFile.setRotation(270);
            }

        } catch (Exception e) {
            System.out.println("AdapterHistoryMessageList -> getOrientationBitmap ERROR : - " + e.getMessage());
        }
    }
}
